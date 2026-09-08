package com.example.chulgunhazabackend.listener;

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


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final ChatAlarmService chatAlarmService;

    // INFO: Chat Notification Queue 의 Listener 입니다.
    @RabbitListener(queues = "chulgunhazabackend_notification_queue")
    public void receiveChatNotification(@Payload ChatNotificationDto chatNotificationDto,
                                            Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try{
            chatAlarmService.sendSseEvent(chatNotificationDto);
            log.info("send Chat Alarm to : " + chatNotificationDto.roomId);
        }catch (Exception e){
            channel.basicNack(tag, false, false);
        }

    }
}
