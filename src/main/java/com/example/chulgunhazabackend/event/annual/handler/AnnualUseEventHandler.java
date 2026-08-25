package com.example.chulgunhazabackend.event.annual.handler;

import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.event.annual.event.AnnualUseEvent;
import com.example.chulgunhazabackend.service.AttendanceAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnualUseEventHandler {

    private final AttendanceAlarmService attendanceAlarmService;

    @Async
    @EventListener
    public void handleAnnualUseEvent(AnnualUseEvent event) {
        String message = String.format("연차 사용이 처리됐습니다. (잔여 %.1f일)", event.getRemainingAnnualCount());
        attendanceAlarmService.sendSseEvent(new MainNotificationDto(
                event.getEmployeeNumber(), message, LocalDateTime.now()
        ));
    }
}
