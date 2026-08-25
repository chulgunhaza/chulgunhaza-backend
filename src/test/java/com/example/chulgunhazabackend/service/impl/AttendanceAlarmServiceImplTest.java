package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.dto.notification.MainNotificationDto;
import com.example.chulgunhazabackend.service.sse.SseEmitterManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * #46 근태(MAIN) SSE 알림 — 골격(SseEmitterManager)은 있었지만 실제 발송 로직이
 * 없던 부분을 채운 {@link AttendanceAlarmServiceImpl} 회귀 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceAlarmServiceImplTest {

    @Mock
    private SseEmitterManager sseEmitterManager;

    @InjectMocks
    private AttendanceAlarmServiceImpl attendanceAlarmService;

    @Test
    @DisplayName("구독 중인 employeeNo면 등록된 emitter로 알림을 전송한다")
    void sendSseEvent_구독중이면_전송한다() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        given(sseEmitterManager.getMainEmitter(10000001L)).willReturn(emitter);

        MainNotificationDto dto = new MainNotificationDto(10000001L, "출근등록이 완료되었습니다.", LocalDateTime.now());
        attendanceAlarmService.sendSseEvent(dto);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(sseEmitterManager, never()).removeMainEmitter(any());
    }

    @Test
    @DisplayName("구독 중이 아니면(emitter 없음) 예외 없이 아무 것도 하지 않는다")
    void sendSseEvent_구독중이_아니면_무시한다() {
        given(sseEmitterManager.getMainEmitter(999L)).willReturn(null);

        MainNotificationDto dto = new MainNotificationDto(999L, "메시지", LocalDateTime.now());
        attendanceAlarmService.sendSseEvent(dto);

        verify(sseEmitterManager, never()).removeMainEmitter(any());
    }

    @Test
    @DisplayName("전송 중 IOException이 나면 해당 emitter를 레지스트리에서 제거한다")
    void sendSseEvent_전송실패시_emitter를_제거한다() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        given(sseEmitterManager.getMainEmitter(10000001L)).willReturn(emitter);
        doThrow(new IOException("연결 끊김")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        MainNotificationDto dto = new MainNotificationDto(10000001L, "메시지", LocalDateTime.now());
        attendanceAlarmService.sendSseEvent(dto);

        verify(sseEmitterManager).removeMainEmitter(10000001L);
    }
}
