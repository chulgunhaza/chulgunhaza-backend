package com.example.chulgunhazabackend.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// #51: README에 "AOP로 로그 추적"이 있었지만 @Aspect 클래스가 하나도 없었다.
// 컨트롤러 메서드를 가로채 요청/응답/소요시간/예외를 공통으로 남긴다 — 각
// 컨트롤러/서비스에 로그 코드를 흩어놓지 않아도 된다. 인자 값은 일부러 안 찍는다
// (EmployeeCredentialDto처럼 password 필드를 들고 있는 DTO가 컨트롤러 메서드
// 인자로 들어오는 경우가 있어서, 잘못 로그로 새는 걸 원천 차단하는 쪽을 택했다).
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.chulgunhazabackend.controller..*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        String requestLine = currentRequestLine();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("[{}] {} -> {}ms", requestLine, signature, elapsedMs);
            return result;
        } catch (Throwable ex) {
            long elapsedMs = System.currentTimeMillis() - start;
            log.warn("[{}] {} -> {}ms, 예외: {}: {}", requestLine, signature, elapsedMs,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // AOP는 웹 요청 스레드가 아닌 컨텍스트(스케줄러, 리스너 등)에서도 호출될 수 있어서
    // RequestContextHolder가 비어있을 수 있다 — 그런 경우엔 "N/A"로 대체한다.
    private String currentRequestLine() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return "N/A";
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }
}
