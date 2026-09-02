package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageListResponseDto;
import com.example.chulgunhazabackend.dto.chat.ChatReadEventDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.example.chulgunhazabackend.websocket.handler.WebSocketMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;



@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;
    private final WebSocketMessageHandler webSocketMessageHandler;
    private final ObjectMapper objectMapper;

    @Override
    public Long saveChatMessage(ChatMessageCreateRMQDto chatMessageCreateRMQDto) {

        // INFO: 채팅방 존재 유무 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageCreateRMQDto.getRoomId()).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM));

        // INFO: 전송하는 사원이 실제로 존재하는지 확인 (Employee 애그리게이트는 id로만 참조 — #74 Phase 0)
        Long senderId = chatMessageCreateRMQDto.getSenderId();
        if (!employeeRepository.existsById(senderId)) {
            throw new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER);
        }

        // INFO: 채팅방 인원이 아닌데 전송하는 경우 예외 처리
        // (수신자는 단체 채팅에서 여러 명일 수 있어 더 이상 여기서 검증하지 않는다 — 실시간 전달
        // 대상은 ChatRabbitMQMessageServiceImpl에서 방 참여자 전원을 조회해서 결정한다.)
        employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(senderId, chatRoom.getId()).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER));

        return chatMessageRepository.save(chatMessageCreateRMQDto.toEntity(chatRoom, senderId)).getId();
    }

    @Override
    public PageDto<ChatMessageListResponseDto> getChatMessagesByRoomId(Long roomId, Long employeeId, Pageable pageable) {

        // INFO : 채팅방 존재 유무 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM));

        // INFO : 채팅방 별 메시지 가져오기
        Page<ChatMessage> contents = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable);

        // INFO : 채팅방 읽음 처리 — "나"(employeeId)가 이 방의 최신 메시지까지 봤다고 표시.
        // 사람별 포인터(EmployeeChatRoom.lastReadMessageId)라서 그룹 채팅에서 다른 사람의
        // 읽음 여부에는 영향을 주지 않는다. 아래 안읽은 사람 수 계산보다 먼저 해야
        // 방금 읽은 메시지들에 "나"가 안읽은 사람으로 잡히지 않는다.
        // 실시간 알림용으로 "읽기 전" 포인터를 먼저 기억해둔다 — 이번에 새로 읽힌 구간
        // (previousLastReadMessageId, maxMessageId]을 계산하는 데 필요하다.
        // HACK : 추후 배치 처리 필요.
        Long previousLastReadMessageId = employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(employeeId, roomId)
                .map(EmployeeChatRoom::getLastReadMessageId)
                .orElse(null);
        Long maxMessageId = chatMessageRepository.findMaxMessageId(roomId);
        boolean advanced = maxMessageId != null
                && (previousLastReadMessageId == null || previousLastReadMessageId < maxMessageId);
        if (advanced) {
            employeeChatRoomRepository.markReadUpTo(employeeId, roomId, maxMessageId);
        }

        // INFO : 메시지별 "안읽은 사람 수"를 계산하려고 방 참여자 전원(과 각자의 lastReadMessageId)을
        // 한 번만 불러온다 — 메시지마다 쿼리 날리면 페이지당 N+1이 나서 여기서 한 번에 처리.
        List<EmployeeChatRoom> members = employeeChatRoomRepository.findByChatRoomId(roomId);

        // INFO : PAGING
        Page<ChatMessageListResponseDto> pageDto = contents.map(chatMessage ->
                ChatMessageListResponseDto.fromEntity(chatMessage, countUnread(members, chatMessage)));

        // INFO : 방금 내가 방을 읽어서 안읽은 인원 수가 바뀐 메시지들을, 그 방에 실시간으로
        // 붙어있는 다른 참여자들에게 즉시 알려준다 — "읽으면 실시간으로 사라져야지"라는
        // 피드백으로 추가. REST로 다시 불러오기 전까진 안 바뀌던 걸 WebSocket으로 push한다.
        if (advanced) {
            broadcastReadReceipt(roomId, employeeId, previousLastReadMessageId, maxMessageId, members);
        }

        return new PageDto<>(pageDto) ;
    }

    private long countUnread(List<EmployeeChatRoom> members, ChatMessage chatMessage) {
        return members.stream()
                .filter(member -> !member.getEmployeeId().equals(chatMessage.getEmployeeId())) // 발신자 본인 제외
                .filter(member -> member.getLastReadMessageId() == null || member.getLastReadMessageId() < chatMessage.getId())
                .count();
    }

    private void broadcastReadReceipt(Long roomId, Long readerId, Long fromMessageId, Long toMessageId, List<EmployeeChatRoom> members) {
        List<ChatMessage> newlyReadMessages = chatMessageRepository.findMessagesInRange(roomId, fromMessageId, toMessageId);
        if (newlyReadMessages.isEmpty()) {
            return;
        }

        List<ChatReadEventDto.MessageUnreadUpdate> updates = newlyReadMessages.stream()
                .map(message -> new ChatReadEventDto.MessageUnreadUpdate(message.getId(), countUnread(members, message)))
                .toList();
        ChatReadEventDto event = new ChatReadEventDto(roomId, readerId, updates);

        // INFO : 방 참여자 중 "읽은 나"를 뺀 나머지에게 — 실시간으로 접속 중인(WebSocket 세션이
        // 열려 있는) 사람에게만 보낸다. 접속 안 해 있으면 다음에 REST로 이 방을 다시 불러올 때
        // 어차피 정확한 값을 받는다.
        for (EmployeeChatRoom member : members) {
            Long memberId = member.getEmployeeId();
            if (memberId.equals(readerId)) {
                continue;
            }
            WebSocketSession session = webSocketMessageHandler.getSession(memberId, roomId);
            if (session != null) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                } catch (IOException | IllegalStateException e) {
                    // INFO : IllegalStateException = 세션이 이미 닫힌 상태(클라이언트가 정상
                    // 종료 핸드셰이크 없이 끊긴 경우 등) — 실측으로 처음 발견한 케이스라
                    // ChatRabbitMQMessageServiceImpl.deliverToOneReceiver와 동일하게 방어.
                    log.info("failed to send read receipt, removing stale session: {}", e.getMessage());
                    webSocketMessageHandler.removeSession(memberId, roomId);
                }
            }
        }
    }
}
