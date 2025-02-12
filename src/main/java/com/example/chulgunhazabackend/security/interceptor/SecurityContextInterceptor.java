package com.example.chulgunhazabackend.security.interceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class SecurityContextInterceptor implements HandshakeInterceptor {

    // INFO: WebSocket Session 에 HttpSession 의 SecurityContext 를 추가하기 위한 로직
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

        SecurityContext securityContext = SecurityContextHolder.getContext();

        if(securityContext != null && securityContext.getAuthentication() != null) {
            attributes.put("SPRING_SECURITY_CONTEXT", securityContext);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // INFO: 필요 시 추가 처리 가능
    }
}
