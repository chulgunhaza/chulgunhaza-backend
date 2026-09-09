package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

// #101: attendance-server의 SecurityConfig와 동일한 패턴 — 로그인 주체가 아니라
// JWT(access_token 쿠키) 검증만 한다. WebSocket 핸드셰이크(/websocket)도 결국
// 일반 HTTP 요청이라 이 필터 체인을 그대로 통과한다 — 쿠키가 없으면 업그레이드
// 전에 401로 막힌다(#98에서 user-server 기준으로 이미 검증된 동작과 동일).
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        httpSecurity.csrf(csrf -> csrf.disable());
        httpSecurity.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER));

        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
        );

        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    // AppCorsConfigurationSource는 common 모듈의 같은 패키지(com.example.chulgunhazabackend.config)에
    // 있어서 import 없이 바로 쓸 수 있다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new AppCorsConfigurationSource(allowedOrigins);
    }
}
