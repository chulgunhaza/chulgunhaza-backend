package com.example.chulgunhazabackend.websocket;

import com.example.chulgunhazabackend.websocket.handler.WebSocketMessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

// #101: WebSocket 실시간 전달을 "로컬 세션 → (없으면) 다른 인스턴스로 팬아웃 →
// (그것도 없으면) 호출부가 SSE 폴백" 순서로 통일한 진입점. 예전엔
// ChatRabbitMQMessageServiceImpl.deliverToOneReceiver와
// ChatMessageServiceImpl.broadcastReadReceipt가 각자
// webSocketMessageHandler.getSession(...)을 직접 불러서, chatting-server가
// 여러 인스턴스로 늘어나면 둘 다 "로컬에 없으면 무조건 오프라인"으로 잘못
// 판단하는 문제가 있었다 — 이 서비스로 통일해서 그 판단 로직을 한 곳에 모았다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatDeliveryService {

    private final WebSocketMessageHandler webSocketMessageHandler;
    private final ChatPresenceRegistry chatPresenceRegistry;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @return true면 로컬 전달 또는 다른 인스턴스로의 팬아웃을 시도한 것 —
     *         호출부는 "실시간으로 전달 시도했다"고 보고 SSE 폴백을 하지 않는다.
     *         false면 이 사람이 이 방에 어디에도(어느 인스턴스에도) 붙어있지
     *         않다는 뜻 — 호출부가 SSE 폴백을 해야 한다.
     */
    public boolean deliverOrFanout(Long receiverId, Long roomId, Object payload) {
        WebSocketSession localSession = webSocketMessageHandler.getSession(receiverId, roomId);
        if (localSession != null) {
            if (deliverToSession(localSession, receiverId, roomId, payload)) {
                return true;
            }
            // 세션이 죽어있었다 — 로컬 맵/presence 둘 다 정리하고 다른 인스턴스에
            // 혹시 붙어있는지 계속 확인한다(같은 사람이 다른 탭/기기로 다른
            // 인스턴스에 붙어있을 수 있음).
            webSocketMessageHandler.removeSession(receiverId, roomId);
            chatPresenceRegistry.markAbsent(receiverId, roomId);
        }

        if (chatPresenceRegistry.isPresentSomewhere(receiverId, roomId)) {
            return fanout(receiverId, roomId, payload);
        }

        return false;
    }

    private boolean deliverToSession(WebSocketSession session, Long receiverId, Long roomId, Object payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            return true;
        } catch (IOException | IllegalStateException e) {
            // INFO : IllegalStateException = 세션이 이미 닫힌 상태 — 클라이언트가 정상
            // 종료 핸드셰이크 없이 끊긴 경우 등(user-server 시절부터 있던 방어 로직 이전).
            log.info("failed to deliver via local WebSocket session, removing stale entry: {}", e.getMessage());
            return false;
        }
    }

    private boolean fanout(Long receiverId, Long roomId, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            ChatFanoutMessage fanoutMessage = new ChatFanoutMessage(receiverId, roomId, payloadJson);
            stringRedisTemplate.convertAndSend(ChatFanoutSubscriber.FANOUT_CHANNEL, objectMapper.writeValueAsString(fanoutMessage));
            return true;
        } catch (JsonProcessingException e) {
            log.warn("팬아웃 메시지 직렬화 실패, SSE 폴백으로 대체: {}", e.getMessage());
            return false;
        }
    }
}
