package com.example.chulgunhazabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// #98: 예전엔 이 RedisTemplate이 세션 스토어(Spring Session Data Redis)와 같이
// 쓰였는데, 세션 인증을 JWT로 바꾸면서 세션 저장 자체를 안 쓰게 됐다. 지금은
// RefreshTokenStore가 refresh 토큰의 jti만 저장하는 용도로 이 빈을 쓴다.
// (예전에 여기 있던 "세션 인덱스 저장소 경합으로 죽는 문제" 관련 내용은
// docs/troubleshooting-concurrent-session-race.md 참고 — JWT 전환으로 해당
// 클래스의 세션 관련 문제 자체가 더 이상 발생하지 않는다.)
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

}
