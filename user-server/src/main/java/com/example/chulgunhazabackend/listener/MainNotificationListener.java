package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.service.AttendanceAlarmService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

// #100: attendance-server가 물리 분리되면서 같은 프로세스 안에서 못 하게 된
// "출근 등록 완료" SSE 알림을 대신 받아준다. attendance-server가
// mainExchange/MAIN_NOTIFICATION_ROUTING_KEY로 발행한 MainNotificationDto를
// 소비해서 AttendanceAlarmService.sendSseEvent(...)로 그대로 이어준다 — 예전엔
// 같은 프로세스 안에서 Events.raise(new AttendanceCreateEvent(...))로 하던 걸
// 대신한다. SSE 발송은 "지금 그 사람이 접속해 있으면 보내고, 아니면 그냥 넘어가는"
// best-effort라 실패해도 재시도/DLQ 없이 그대로 ack한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class MainNotificationListener {

    private final AttendanceAlarmService attendanceAlarmService;

    @RabbitListener(queues = RabbitMQConfig.MAIN_NOTIFICATION_QUEUE_NAME, containerFactory = "simpleRabbitListenerContainerFactory")
    public void consume(@Payload MainNotificationDto mainNotificationDto,
                         Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        attendanceAlarmService.sendSseEvent(mainNotificationDto);
        channel.basicAck(tag, false);
        log.info("교차 서비스 알림 처리: {}", mainNotificationDto);
    }
}
