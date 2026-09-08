package com.example.chulgunhazabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

// #100: 스켈레톤에서 실제 근태 도메인 서비스로 — AttendanceRecord/Controller/
// Service/Listener가 user-server에서 여기로 이전됐다. @EnableRetry는
// AttendanceDeadLetterListener의 @Retryable/@Recover에 필요.
@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
public class AttendanceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceServerApplication.class, args);
    }
}
