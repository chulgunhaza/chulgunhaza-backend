package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.service.ChatAlarmService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

// #101: chatting-server가 물리 분리되면서 같은 프로세스 안에서 못 하게 된 "채팅
// 오프라인 알림" SSE 푸시를 대신 받아준다. chatting-server가 chatExchange/
// CHAT_NOTIFICATION_ROUTING_KEY로 발행한 ChatNotificationDto를 소비해서
// ChatAlarmService.sendSseEvent(...)로 그대로 이어준다 — #100의
// MainNotificationListener와 동일한 패턴.
//
// 흥미로운 점: CHAT_NOTIFICATION_QUEUE_NAME 큐/바인딩은 RabbitMQConfig에 이미
// 있었지만, 발행 주체도 소비 주체도 없는 죽은 배관이었다(#100의
// MAIN_NOTIFICATION_QUEUE_NAME과 완전히 같은 상황) — 이번에 그 자리를 채운다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationListener {

    private final ChatAlarmService chatAlarmService;

    @RabbitListener(queues = RabbitMQConfig.CHAT_NOTIFICATION_QUEUE_NAME, containerFactory = "simpleRabbitListenerContainerFactory")
    public void consume(@Payload ChatNotificationDto chatNotificationDto,
                         Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        chatAlarmService.sendSseEvent(chatNotificationDto);
        channel.basicAck(tag, false);
        log.info("교차 서비스 채팅 알림 처리: {}", chatNotificationDto);
    }
}
