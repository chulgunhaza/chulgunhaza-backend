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

// #100: user-server의 SecurityConfig와 달리 이 서비스는 로그인 주체가 아니므로
// formLogin/logout/DaoAuthenticationProvider가 전혀 없다 — JWT(access_token 쿠키)
// 검증만 하고, /internal(대시보드 통계 등 서비스 간 호출)과 /actuator(헬스체크)는
// 인증 없이 열어둔다.
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
                .requestMatchers("/internal/**", "/actuator/**").permitAll()
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
