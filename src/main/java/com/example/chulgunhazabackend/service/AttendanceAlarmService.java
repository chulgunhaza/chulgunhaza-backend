package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface AttendanceAlarmService {

    SseEmitter subscribe(Long employeeNo) throws IOException;

    void sendSseEvent(MainNotificationDto mainNotificationDto);
}
