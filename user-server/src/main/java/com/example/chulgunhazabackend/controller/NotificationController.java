package com.example.chulgunhazabackend.controller;


import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.service.AttendanceAlarmService;
import com.example.chulgunhazabackend.service.ChatAlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController{

    private final ChatAlarmService chatAlarmService;

    private final AttendanceAlarmService attendanceAlarmService;

    @GetMapping(value ="/subscribe/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeChat(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto ) throws IOException {
        return chatAlarmService.subscribe(employeeCredentialDto.getId());
    }

    // #46: 근태(출근 등록, 연차 사용 등) MAIN SSE 알림 구독.
    // CHAT과 달리 이 채널은 employeeNo로 키잉된다 (#100: attendance-server가
    // MainNotificationDto를 RabbitMQ로 보내오는 것도, AnnualUseEventHandler가
    // 인프로세스로 보내는 것도 전부 이 규약을 따른다).
    @GetMapping(value = "/subscribe/main", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeMain(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto) throws IOException {
        return attendanceAlarmService.subscribe(employeeCredentialDto.getEmployeeNo());
    }

}
