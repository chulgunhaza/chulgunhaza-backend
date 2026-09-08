package com.example.chulgunhazabackend.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

// #98: refresh 토큰 자체가 아니라 jti(발급 시 부여한 UUID)만 Redis에 저장해서,
// 로그아웃/재발급 시 그 jti를 지우거나 새 값으로 바꾸는 것만으로 이전 refresh
// 토큰을 무효화할 수 있게 한다(직원 하나당 활성 refresh 토큰은 하나라는 가정).
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Long employeeId, String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(employeeId), jti, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> find(Long employeeId) {
        Object value = redisTemplate.opsForValue().get(key(employeeId));
        return Optional.ofNullable((String) value);
    }

    public void delete(Long employeeId) {
        redisTemplate.delete(key(employeeId));
    }

    private String key(Long employeeId) {
        return KEY_PREFIX + employeeId;
    }
}
