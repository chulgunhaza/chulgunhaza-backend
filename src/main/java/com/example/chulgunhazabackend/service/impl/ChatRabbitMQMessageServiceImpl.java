package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.event.chat.event.ChatCreateEvent;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.ChatRabbitMQMessageService;
import com.example.chulgunhazabackend.service.sse.SseEmitterManager;
import com.example.chulgunhazabackend.websocket.handler.WebSocketMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRabbitMQMessageServiceImpl implements ChatRabbitMQMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final WebSocketMessageHandler webSocketMessageHandler;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChatMessageRepository chatMessageRepository;
    private final SseEmitterManager sseEmitterManager;
    private final EmployeeRepository employeeRepository;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;

    //INFO : 채팅 알람 발생 시 RMQ 로 메시지 전송
    public void sendNotification(ChatNotificationDto chatNotificationDto){
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME, RabbitMQConfig.CHAT_NOTIFICATION_ROUTING_KEY, chatNotificationDto);
        log.info("Sending notification to RabbitMQ");
    }

    // INFO: 채팅 저장 RMQ 전송 + 방 참여자 전원(그룹 포함)에게 실시간 전달
    public String sendChatMessage(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId){

        // INFO : RMQ 로 전송을 위한 데이터 바인딩 (저장은 receiverId 없이도 가능 — roomId로 충분)
        ChatMessageCreateRMQDto dto = new ChatMessageCreateRMQDto(
                senderId
                , chatMessageCreateRequestDto.getMessage()
                , chatMessageCreateRequestDto.getRoomId()
                , chatMessageCreateRequestDto.getCreateTime());

        // INFO : RMQ 전송
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME
                , RabbitMQConfig.CHAT_ROUTING_KEY
                , dto);

        // INFO : 이 방의 나(sender)를 제외한 참여자 전원에게 실시간 전달 — 1:1이면 1명,
        // 단체 채팅이면 여러 명에게 순서대로 WS(접속 중) 또는 SSE(미접속) 알림을 보낸다.
        List<EmployeeChatRoom> receivers = employeeChatRoomRepository.findOtherMembersByChatRoomId(chatMessageCreateRequestDto.getRoomId(), senderId);

        for (EmployeeChatRoom receiver : receivers) {
            deliverToOneReceiver(chatMessageCreateRequestDto, senderId, receiver);
        }

        log.info("Sending chat message to RabbitMQ");

        return "전송 완료";
    }

    private void deliverToOneReceiver(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId, EmployeeChatRoom receiver) {

        Long receiverId = receiver.getEmployeeId();
        Long roomId = chatMessageCreateRequestDto.getRoomId();

        // INFO : ChatRoom 관련 session 이 존재하면 전송
        // HACK : 추후 Listener 내부로 로직 리펙토링 가능.
        WebSocketSession session = webSocketMessageHandler.getSession(receiverId, roomId);
        log.info(String.valueOf(session));
        boolean delivered = false;
        if (session != null) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessageCreateRequestDto)));
                delivered = true;
            } catch (IOException | IllegalStateException e) {
                // INFO : IllegalStateException은 "세션이 이미 닫혔다"는 뜻 — 클라이언트가 정상
                // 종료 핸드셰이크 없이 뚝 끊긴 경우 등으로 실측 확인됨. 이걸 안 잡으면 이 요청
                // 전체가 500으로 죽는다. 맵에서도 정리해서 다음번엔 바로 SSE로 폴백하게 한다.
                log.info("failed to deliver via WebSocket, removing stale session: {}", e.getMessage());
                webSocketMessageHandler.removeSession(receiverId, roomId);
            }
        }

        if (!delivered) {
            // INFO : 세션이 없거나(session == null) 방금 죽은 걸 확인했으면 알람 전송
            log.info("no live session -> fall back to sse");

            if(sseEmitterManager.getChatEmitter(receiverId) != null){
                Employee senderEmployee = employeeRepository.findEmployeeById(senderId).orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
                // INFO : 안읽은 메시지 수는 "받는 사람" 기준이어야 한다 — 기존엔 senderId로 잘못
                // 조회하고 있었다(단체 채팅 브로드캐스트 리팩토링하며 같이 바로잡음). 지금 막
                // 도착한 이 메시지까지 포함해서 세야 하니 receiver의 lastReadMessageId(아직
                // 이 메시지를 반영 안 한 값) 기준으로 그대로 카운트하면 된다.
                long unReadMessageCount = chatMessageRepository.countUnread(
                        chatMessageCreateRequestDto.getRoomId(), receiverId, receiver.getLastReadMessageId());

                // INFO event 발행
                applicationEventPublisher.publishEvent(new ChatCreateEvent(
                        chatMessageCreateRequestDto.getRoomId()
                        , senderEmployee.getEmployeeNo()
                        , senderEmployee.getName()
                        , receiverId
                        , chatMessageCreateRequestDto.getMessage()
                        , chatMessageCreateRequestDto.getCreateTime()
                        , unReadMessageCount
                ));
            }

        }
    }

}
