package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.dto.chat.ChatNotificationDto;
import com.example.chulgunhazabackend.service.ChatAlarmService;
import com.example.chulgunhazabackend.service.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAlarmServiceImpl implements ChatAlarmService {

    private final SseEmitterManager sseEmitterManager;

    //INFO: Chat Sse 를 구독하는 로직입니다. 세부 로직 -> SseEmitterManager
    @Override
    public SseEmitter subscribe(Long employeeId) throws IOException {
        return sseEmitterManager.chatRegisterEmitter(employeeId);
    }

    //INFO : 구독 중이라면 전송하는 로직입니다.
    @Async
    @Override
    public void sendSseEvent(ChatNotificationDto chatNotificationDto) {
        SseEmitter emitter = sseEmitterManager.getChatEmitter(chatNotificationDto.getReceiverEmployeeNo());
        if (emitter != null){
            try{
                emitter.send(SseEmitter.event().data(chatNotificationDto));
                log.info("send from ChatAlarmService Send sendSseEvent Method");
            } catch (IOException e) {
                log.info("Occur error from ChatAlarmService Send sendSseEvent Method");
                sseEmitterManager.removeChatEmitter(chatNotificationDto.getReceiverEmployeeNo());
            }
        }else {
            log.info("emitter is null");
        }
    }
}
