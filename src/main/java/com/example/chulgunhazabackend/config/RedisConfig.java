package com.example.chulgunhazabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// 세션 저장소는 이 클래스가 아니라 application.yml의 spring.session.store-type=redis로
// 스프링 부트가 자동 구성한다(비인덱스 RedisSessionRepository). 예전엔 여기서
// @EnableRedisHttpSession으로 인덱스형 저장소(RedisIndexedSessionRepository)를 강제로
// 켜고, 그 저장소가 쓰지도 않는 별도 RedisSessionRepository 빈까지 수동으로 또 만들고
// 있었다. 인덱스형 저장소는 만료 관리를 위해 정렬 셋(인덱스)을 따로 유지하는데, 같은
// 세션에 동시 요청이 여러 개 몰리면(로그인 직후 대시보드가 여러 컴포넌트에서 동시에
// /v1/chat/find/rooms 등을 부르는 상황) 그 인덱스 갱신끼리 경합하면서
// "Session was invalidated"로 죽는 문제가 실측으로 확인됐다. 이 프로젝트는 "특정
// 유저의 모든 세션 찾기" 같은 인덱스 조회 기능을 쓰지 않아서 인덱스형 저장소가 애초에
// 필요 없었다 — @EnableRedisHttpSession과 수동 세션 리포지토리 빈을 제거하고
// application.yml의 자동 구성(비인덱스)에 맡겨서 해결.
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
