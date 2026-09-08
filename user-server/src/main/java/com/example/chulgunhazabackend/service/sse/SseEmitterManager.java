package com.example.chulgunhazabackend.service.sse;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterManager {

    private final Map<EmitterType, Map<Long, SseEmitter>> emitters = new ConcurrentHashMap<>();

    // INFO: Emitter type 은 Main 과 chat 2가지 입니다.
    public enum EmitterType{
        CHAT, MAIN;
    }

    @PostConstruct
    public void init() {

        emitters.put(EmitterType.CHAT, new ConcurrentHashMap<>());
        emitters.put(EmitterType.MAIN, new ConcurrentHashMap<>());

    }

    // INFO: chatEmitter 를 등록하고 반환하는 메서드 입니다.
    // HACK: 추후 Emitter 시간 조정 필요.
    public SseEmitter chatRegisterEmitter(Long employeeId) {

        SseEmitter emitter = new SseEmitter(10000000L);
        emitters.get(EmitterType.CHAT).put(employeeId, emitter);
        emitter.onCompletion(() -> emitters.get(EmitterType.CHAT).remove(employeeId));
        emitter.onTimeout(() -> emitters.get(EmitterType.CHAT).remove(employeeId));

        sendEmitterData(EmitterType.CHAT, emitter, employeeId, "ChatSseConnect", "Success Chat SSE");
        return emitter;

    }

    // INFO: mainEmitter 를 등록하고 반환하는 메서드 입니다.
   public SseEmitter mainRegisterEmitter(Long employeeNo){
       SseEmitter emitter = new SseEmitter(10000000L);
       emitters.get(EmitterType.MAIN).put(employeeNo, emitter);
       emitter.onCompletion(() -> emitters.get(EmitterType.MAIN).remove(employeeNo));
       emitter.onTimeout(() -> emitters.get(EmitterType.MAIN).remove(employeeNo));
       sendEmitterData(EmitterType.MAIN, emitter, employeeNo, "MainSseConnect", "Success Main SSE");
       return emitter;
   }

    // INFO: chatEmitter 를 가져오는 로직
    public SseEmitter getChatEmitter(Long employeeNo) {
        return emitters.get(EmitterType.CHAT).get(employeeNo);
    }

    // INFO chatEmitter 를 제거하는 로직
    public void removeChatEmitter(Long employeeNo) {
        emitters.get(EmitterType.CHAT).remove(employeeNo);
    }

    // INFO: mainEmitter 를 가져오는 로직
    public SseEmitter getMainEmitter(Long employeeNo){
        return emitters.get(EmitterType.MAIN).get(employeeNo);
    }

    public void removeMainEmitter(Long employeeNo) {
        emitters.get(EmitterType.MAIN).remove(employeeNo);
    }

    // INFO: 모든 Emitter 를 제거하는 로직 -> logout 시에 사용
    public void clearEmitters() {
        emitters.forEach((type, emitterMap) -> emitterMap.clear());
    }

    // INFO: client 기본 유지를 데이터를 보내는 메서드
    public void sendEmitterData(EmitterType type, SseEmitter emitter, Long employeeNo, String name, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(Long.toString(employeeNo))
                    .name(name)
                    .data(data));

        } catch (IOException e) {
            emitters.get(type).remove(employeeNo);
            emitter.complete();
        }
    }

}
