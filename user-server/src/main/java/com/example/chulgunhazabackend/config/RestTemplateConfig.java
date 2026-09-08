package com.example.chulgunhazabackend.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// #100: attendance-server 같은 다른 서비스에 동기 호출(내부 API)을 하기 위한
// 공용 RestTemplate. 상대 서비스가 응답을 안 주는 상황에서 요청 스레드가
// 무한정 붙잡히지 않도록 타임아웃을 짧게 잡는다 — 대시보드 통계처럼 "느리게
// 답하느니 빨리 실패하고 기본값으로 대체"가 맞는 호출에 쓴다.
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
