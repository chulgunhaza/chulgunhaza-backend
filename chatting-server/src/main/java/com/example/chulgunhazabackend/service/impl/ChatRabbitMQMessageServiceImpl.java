package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.service.ChatRabbitMQMessageService;
import com.example.chulgunhazabackend.websocket.ChatDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRabbitMQMessageServiceImpl implements ChatRabbitMQMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final ChatDeliveryService chatDeliveryService;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;

    // #101: chatting-server는 SseEmitterManager(user-server 전용)를 직접 호출할 수
    // 없어서, 여기서 발행한 걸 user-server의 ChatNotificationListener가 소비해서
    // ChatAlarmService.sendSseEvent(...)로 이어준다. "그 사람이 실제로 SSE를 구독
    // 중인지"는 이제 user-server 쪽(ChatAlarmServiceImpl)만 알 수 있으므로 여기선
    // 무조건 발행하고 판단은 그쪽에 맡긴다(구독 중이 아니면 그쪽에서 조용히 무시).
    @Override
    public void sendNotification(ChatNotificationDto chatNotificationDto){
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME, RabbitMQConfig.CHAT_NOTIFICATION_ROUTING_KEY, chatNotificationDto);
        log.info("Sending notification to RabbitMQ");
    }

    // INFO: 채팅 저장 RMQ 전송 — 큐에 발행만 한다. 실시간 전달은 더 이상 여기서 안 함(#57):
    // ChatMessageListener가 DB 저장에 실제로 성공한 뒤 deliverToReceivers를 따로 호출한다.
    // (예전엔 이 메서드가 발행과 동시에 실시간 전달까지 했는데, 저장은 비동기(큐 소비 후)라
    // 저장이 나중에 실패해도 상대는 이미 메시지를 받아본 상태가 될 수 있는 구조였다.)
    @Override
    public void sendChatMessage(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId, String senderName, Long senderEmployeeNo){

        // INFO : RMQ 로 전송을 위한 데이터 바인딩 (저장은 receiverId 없이도 가능 — roomId로 충분)
        ChatMessageCreateRMQDto dto = new ChatMessageCreateRMQDto(
                senderId
                , senderName
                , senderEmployeeNo
                , chatMessageCreateRequestDto.getMessage()
                , chatMessageCreateRequestDto.getRoomId()
                , chatMessageCreateRequestDto.getCreateTime());

        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME
                , RabbitMQConfig.CHAT_ROUTING_KEY
                , dto);

        log.info("Sending chat message to RabbitMQ");
    }

    // INFO : 이 방의 나(sender)를 제외한 참여자 전원에게 실시간 전달 — 1:1이면 1명,
    // 단체 채팅이면 여러 명에게 순서대로 WS(접속 중) 또는 SSE(미접속) 알림을 보낸다.
    // ChatMessageListener가 DB 저장 성공을 확인한 뒤에만 호출한다.
    @Override
    public void deliverToReceivers(ChatMessageCreateRMQDto chatMessageCreateRMQDto) {
        Long senderId = chatMessageCreateRMQDto.getSenderId();
        List<EmployeeChatRoom> receivers = employeeChatRoomRepository.findOtherMembersByChatRoomId(chatMessageCreateRMQDto.getRoomId(), senderId);

        for (EmployeeChatRoom receiver : receivers) {
            deliverToOneReceiver(chatMessageCreateRMQDto, receiver);
        }
    }

    private void deliverToOneReceiver(ChatMessageCreateRMQDto chatMessageCreateRMQDto, EmployeeChatRoom receiver) {

        Long receiverId = receiver.getEmployeeId();
        Long roomId = chatMessageCreateRMQDto.getRoomId();

        // #101: 로컬 세션 → (없으면) 다른 chatting-server 인스턴스로 팬아웃까지
        // ChatDeliveryService가 판단한다. 예전엔 여기서 직접
        // webSocketMessageHandler.getSession(...)만 보고 "없으면 오프라인"이라고
        // 판단했는데, 인스턴스가 여러 개면 그게 틀린 판단이 될 수 있다.
        boolean delivered = chatDeliveryService.deliverOrFanout(receiverId, roomId, chatMessageCreateRMQDto);

        if (!delivered) {
            // INFO : 안읽은 메시지 수는 "받는 사람" 기준이어야 한다 — 기존엔 senderId로 잘못
            // 조회하고 있었다(단체 채팅 브로드캐스트 리팩토링하며 같이 바로잡음). 지금 막
            // 도착한 이 메시지까지 포함해서 세야 하니 receiver의 lastReadMessageId(아직
            // 이 메시지를 반영 안 한 값) 기준으로 그대로 카운트하면 된다.
            long unReadMessageCount = chatMessageRepository.countUnread(
                    roomId, receiverId, receiver.getLastReadMessageId());

            // #101: 인프로세스 이벤트(ChatCreateEvent) 대신 RabbitMQ로 user-server에
            // 알림을 요청한다 — chatting-server가 물리 분리되면서 SseEmitterManager를
            // 더 이상 같은 프로세스에서 호출할 수 없다.
            sendNotification(new ChatNotificationDto(
                    roomId
                    , chatMessageCreateRMQDto.getSenderEmployeeNo()
                    , chatMessageCreateRMQDto.getSenderName()
                    , receiverId
                    , chatMessageCreateRMQDto.getMessage()
                    , unReadMessageCount
            ));
        }
    }

}
