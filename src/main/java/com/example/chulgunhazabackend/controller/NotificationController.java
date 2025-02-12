package com.example.chulgunhazabackend.controller;


import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.service.ChatAlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController{

    private final ChatAlarmService chatAlarmService;

    @GetMapping(value ="/subscribe/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeChat(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto ) throws IOException {
        return chatAlarmService.subscribe(employeeCredentialDto.getId());
    }

    //TODO: MAIN Controller 생성

}
