package com.example.chulgunhazabackend.event.attendance.handler;

import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.event.attendance.event.AttendanceCreateEvent;
import com.example.chulgunhazabackend.service.AttendanceAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

// #46: 이전엔 println으로만 로그를 남기고 끝이라 실제 알림이 나가지 않았다.
// AttendanceAlarmService(MAIN SSE)로 실제 발송하도록 연결했다.
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceCreateEventHandler {

    private final AttendanceAlarmService attendanceAlarmService;

    @Async
    @TransactionalEventListener(
            classes = AttendanceCreateEvent.class,
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleAttendanceCreateEventAfterCommit(AttendanceCreateEvent attendanceCreateEvent){
        log.info("이벤트 등록 : {}", attendanceCreateEvent);

        attendanceAlarmService.sendSseEvent(new MainNotificationDto(
                attendanceCreateEvent.getEmployeeNumber(),
                attendanceCreateEvent.getMessage(),
                LocalDateTime.now()
        ));
    }

}
