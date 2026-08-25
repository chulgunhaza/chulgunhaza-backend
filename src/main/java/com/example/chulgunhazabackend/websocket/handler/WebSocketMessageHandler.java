package com.example.chulgunhazabackend.websocket.handler;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketMessageHandler implements WebSocketHandler {

    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Long, WebSocketSession>> sessions = new ConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        // INFO : 인증된 유저의 정보를 가져와서 ConcurrentHashMap 에 저장을 합니다.
        EmployeeCredentialDto credentialDto = getAuthenticatedUser(session);
        if (credentialDto != null) {
            sessions.putIfAbsent(credentialDto.getId(), new ConcurrentHashMap<>());
            log.info("WebSocket Connected for user {}", credentialDto.getId());
        } else {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            log.warn("Failed to connect to websocket, no Authentication");
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        //INFO : Client 로 부터 접속 중인 채팅방 정보 받아서 처리
        String payload = message.getPayload().toString();
        log.info("Received message: {}", payload);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(payload);
        String type = jsonNode.get("type").asText();
        Long chatRoomId = jsonNode.get("chatRoomId").asLong();

        EmployeeCredentialDto credentialDto = getAuthenticatedUser(session);
        if (credentialDto != null) {
            Long userId = credentialDto.getId();

            if ("subscribe".equals(type)) {
                subscribeToChatRoom(userId, chatRoomId, session);
            } else if ("unsubscribe".equals(type)) {
                unsubscribeFromChatRoom(userId, chatRoomId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        //INFO : Client 로 부터 받은 메시지에 에러가 발생했을 때 처리할 수 있음.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        SecurityContext securityContext = (SecurityContext) session.getAttributes().get("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();
            if (authentication != null) {
                EmployeeCredentialDto credentialDto = (EmployeeCredentialDto) authentication.getPrincipal();
                // INFO : sessions 맵은 employee "id"(PK)로 저장한다(afterConnectionEstablished,
                // subscribeToChatRoom, getSession 전부 getId() 사용). 여기만 getEmployeeNo()로
                // 지우고 있어서 — id != employeeNo(항상 10000000 차이) — 연결이 끊겨도 세션이
                // 맵에서 절대 안 지워지는 버그였다. 그 결과 이미 끊긴(닫힌) 세션 참조가
                // 계속 남아있다가, 나중에 그 참조로 메시지를 보내려 하면
                // "세션이 이미 닫혔다"는 IllegalStateException으로 요청 전체가 500 났다
                // (읽음 실시간 알림 기능 붙이다가 실측으로 발견).
                sessions.remove(credentialDto.getId());
                log.info("WebSocket connection closed for user: {}", credentialDto.getId());
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private EmployeeCredentialDto getAuthenticatedUser(WebSocketSession session) {
        SecurityContext securityContext = (SecurityContext) session.getAttributes().get("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return (EmployeeCredentialDto) authentication.getPrincipal();
            }
        }
        return null;
    }

    // INFO : 특정 채팅방 session 저장
    private void subscribeToChatRoom(Long userId, Long chatRoomId, WebSocketSession session) {
        sessions.putIfAbsent(userId, new ConcurrentHashMap<>());
        sessions.get(userId).put(chatRoomId, session);
        log.info("User {} subscribed to chatRoom {}", userId, chatRoomId);
    }

    // INFO : 특정 채팅방 session 제거
    private void unsubscribeFromChatRoom(Long userId, Long chatRoomId) {
        if (sessions.containsKey(userId) && sessions.get(userId).containsKey(chatRoomId)) {
            sessions.get(userId).remove(chatRoomId);
            log.info("User {} unsubscribed from chatRoom {}", userId, chatRoomId);
        }
    }

    // INFO : 특정 채팅방 session 찾는 메서드
    public WebSocketSession getSession(long employeeNo, Long roomId) {
        ConcurrentHashMap<Long, WebSocketSession> session = sessions.get(employeeNo);
        if(session == null){
            log.info("User {} not found", employeeNo);
            return null;
        }
        WebSocketSession webSocketSession = Objects.requireNonNull(session).get(roomId);
        if(webSocketSession == null) {
            log.info("no session");
            return null;
        }
        return webSocketSession;
    }

    // INFO : 세션으로 메시지 전송을 시도했는데 이미 닫혀 있어서 실패했을 때, 그 stale
    // 참조를 맵에서 지우는 자기치유용 메서드. afterConnectionClosed가 정상 동작하면
    // 원래는 필요 없어야 하지만, 클라이언트가 정상 종료 핸드셰이크 없이 뚝 끊기는
    // 경우(네트워크 끊김 등)까지 대비해서 방어적으로 둔다.
    public void removeSession(Long userId, Long roomId) {
        ConcurrentHashMap<Long, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(roomId);
        }
    }
}
