package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.security.interceptor.SecurityContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    // INFO : WebSocket 연결을 위한 설정입니다. 또한 웹 소켓 세션을 받을 때 시큐리티 컨텍스트의 정보 함께 받는 Interceptor 를 사용하고 있습니다.
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(webSocketHandler, "/websocket")
                .addInterceptors(new SecurityContextInterceptor())
                .setAllowedOrigins("*");
    }
}
