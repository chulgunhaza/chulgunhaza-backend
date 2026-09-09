package com.example.chulgunhazabackend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// #101: chatting-server를 여러 인스턴스로 띄웠을 때, "이 사람이 이 방에 지금
// 어딘가에는(어느 인스턴스인지는 몰라도) 붙어있다"는 사실만 Redis에 기록해두는
// 가벼운 registry. WebSocketMessageHandler의 로컬 세션 맵은 그대로 "이 인스턴스
// 안에서" 세션을 찾는 용도로 남기고, 이 registry는 ChatDeliveryService가
// "로컬에 없으면 다른 인스턴스에 팬아웃할지, 진짜 오프라인이라 SSE로 보낼지"를
// 판단하는 데만 쓴다.
@Component
@RequiredArgsConstructor
public class ChatPresenceRegistry {

    private static final String KEY_PREFIX = "chat:presence:";

    private final StringRedisTemplate redisTemplate;

    public void markPresent(Long employeeId, Long roomId) {
        redisTemplate.opsForValue().set(key(employeeId, roomId), "1");
    }

    public void markAbsent(Long employeeId, Long roomId) {
        redisTemplate.delete(key(employeeId, roomId));
    }

    public boolean isPresentSomewhere(Long employeeId, Long roomId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(employeeId, roomId)));
    }

    private String key(Long employeeId, Long roomId) {
        return KEY_PREFIX + employeeId + ":" + roomId;
    }
}
