package com.example.chulgunhazabackend.websocket;

import com.example.chulgunhazabackend.websocket.handler.WebSocketMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

// #101: 모든 chatting-server 인스턴스가 이 채널(chat:fanout)을 구독한다.
// ChatDeliveryService가 "이 사람이 로컬엔 없지만 어딘가엔 붙어있다"고 판단했을
// 때만 여기로 발행하므로, 실제로 그 세션을 들고 있는 인스턴스만 로컬 조회에서
// 걸려서 전달하고 나머지 인스턴스는 조용히 무시한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFanoutSubscriber implements MessageListener {

    public static final String FANOUT_CHANNEL = "chat:fanout";

    private final WebSocketMessageHandler webSocketMessageHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatFanoutMessage fanoutMessage = objectMapper.readValue(message.getBody(), ChatFanoutMessage.class);
            WebSocketSession session = webSocketMessageHandler.getSession(fanoutMessage.getReceiverId(), fanoutMessage.getRoomId());
            if (session != null) {
                session.sendMessage(new TextMessage(fanoutMessage.getPayloadJson()));
                log.info("팬아웃으로 로컬 세션에 전달: receiverId={}, roomId={}", fanoutMessage.getReceiverId(), fanoutMessage.getRoomId());
            }
            // session == null이면 이 인스턴스가 그 세션을 안 들고 있다는 뜻 — 정상 무시.
        } catch (IOException | IllegalStateException e) {
            log.warn("팬아웃 메시지 처리 실패(무시): {}", e.getMessage());
        }
    }
}
