package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.service.AttendanceRabbitMQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceRabbitMQServiceImpl implements AttendanceRabbitMQService {

    private final RabbitTemplate rabbitTemplate;

    public void sendAttendanceDto(AttendanceCreateRequestDto attendanceCreateRequestDto){
        rabbitTemplate.convertAndSend(RabbitMQConfig.MAIN_EXCHANGE_NAME,
                RabbitMQConfig.ATTENDANCE_ROUTING_KEY, attendanceCreateRequestDto);
        log.info("메세지 큐 등록:  ${}", attendanceCreateRequestDto);
    }

    // #100: user-server의 MainNotificationListener가 소비해서
    // AttendanceAlarmService.sendSseEvent(...)로 이어준다 — 예전에 같은 프로세스
    // 안에서 Events.raise(new AttendanceCreateEvent(...))로 하던 걸 대신한다.
    @Override
    public void notifyRegistered(AttendanceCreateRequestDto attendanceCreateRequestDto) {
        MainNotificationDto notification = new MainNotificationDto(
                attendanceCreateRequestDto.getEmployeeNo(),
                "출근이 등록되었습니다.",
                LocalDateTime.now()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.MAIN_EXCHANGE_NAME,
                RabbitMQConfig.MAIN_NOTIFICATION_ROUTING_KEY, notification);
    }

}
