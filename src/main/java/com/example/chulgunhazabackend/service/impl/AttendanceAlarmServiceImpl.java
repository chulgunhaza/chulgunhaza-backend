package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.service.AttendanceAlarmService;
import com.example.chulgunhazabackend.service.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

// #46: 근태(MAIN) SSE 알림. 골격(SseEmitterManager.mainRegisterEmitter 등)은 이미 있었고
// 실제 구독 엔드포인트와 발송 로직이 빠져 있던 부분을 채웠다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceAlarmServiceImpl implements AttendanceAlarmService {

    private final SseEmitterManager sseEmitterManager;

    @Override
    public SseEmitter subscribe(Long employeeNo) throws IOException {
        return sseEmitterManager.mainRegisterEmitter(employeeNo);
    }

    @Async
    @Override
    public void sendSseEvent(MainNotificationDto mainNotificationDto) {
        SseEmitter emitter = sseEmitterManager.getMainEmitter(mainNotificationDto.getReceiverEmployeeNo());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(mainNotificationDto));
                log.info("send from AttendanceAlarmService sendSseEvent Method");
            } catch (IOException e) {
                log.info("Occur error from AttendanceAlarmService sendSseEvent Method");
                sseEmitterManager.removeMainEmitter(mainNotificationDto.getReceiverEmployeeNo());
            }
        } else {
            log.info("main emitter is null");
        }
    }
}
