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
                sessions.remove(credentialDto.getEmployeeNo()); // 세션 종료 시 제거
                log.info("WebSocket connection closed for user: {}", credentialDto.getEmployeeNo());
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
}
