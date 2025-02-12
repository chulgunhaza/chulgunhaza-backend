package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.event.chat.event.ChatCreateEvent;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
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

    //INFO : 채팅 알람 발생 시 RMQ 로 메시지 전송
    public void sendNotification(ChatNotificationDto chatNotificationDto){
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME, RabbitMQConfig.CHAT_NOTIFICATION_ROUTING_KEY, chatNotificationDto);
        log.info("Sending notification to RabbitMQ");
    }

    // INFO: 채팅 저장 RMQ 전송
    public String sendChatMessage(ChatMessageCreateRequestDto chatMessageCreateRequestDto, Long senderId){

        // INFO : RMQ 로 전송을 위한 데이터 바인딩
        ChatMessageCreateRMQDto dto = new ChatMessageCreateRMQDto(
                senderId
                , chatMessageCreateRequestDto.getReceiverId()
                , chatMessageCreateRequestDto.getMessage()
                , chatMessageCreateRequestDto.getRoomId()
                , chatMessageCreateRequestDto.getCreateTime());

        // INFO : RMQ 전송
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE_NAME
                , RabbitMQConfig.CHAT_ROUTING_KEY
                , dto);

        // INFO : ChatRoom 관련 session 이 존재하면 전송
        // HACK : 추후 Listener 내부로 로직 리펙토링 가능.
        WebSocketSession session = webSocketMessageHandler.getSession(chatMessageCreateRequestDto.getReceiverId(), chatMessageCreateRequestDto.getRoomId());
        log.info(String.valueOf(session));
        if(session != null){
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessageCreateRequestDto)));
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }else{
            // INFO : 그렇지 않다면 알람 전송
            log.info("session is null -> change sse session");

            if(sseEmitterManager.getChatEmitter(chatMessageCreateRequestDto.getReceiverId()) != null){
                Employee senderEmployee = employeeRepository.findEmployeeById(senderId).orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
                long unReadMessageCount = chatMessageRepository.findByIsReadCount(chatMessageCreateRequestDto.getRoomId(), senderId);

                // INFO event 발행
                applicationEventPublisher.publishEvent(new ChatCreateEvent(
                        chatMessageCreateRequestDto.getRoomId()
                        , senderEmployee.getEmployeeNo()
                        , senderEmployee.getName()
                        , chatMessageCreateRequestDto.getReceiverId()
                        , chatMessageCreateRequestDto.getMessage()
                        , chatMessageCreateRequestDto.getCreateTime()
                        , unReadMessageCount
                ));
            }

        }

        log.info("Sending chat message to RabbitMQ");

        return "전송 완료";
    }

}
