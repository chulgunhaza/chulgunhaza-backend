package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public interface ChatAlarmService {

    SseEmitter subscribe(Long employeeNo) throws IOException;

    void sendSseEvent(ChatNotificationDto chatNotificationDto);
}
