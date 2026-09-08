package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.service.AttendanceRabbitMQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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

}
