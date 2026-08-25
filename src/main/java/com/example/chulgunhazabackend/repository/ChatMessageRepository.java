package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);

    @Query(value = "SELECT * FROM chat_message WHERE room_id = :roomId ORDER BY create_time DESC LIMIT 1", nativeQuery = true)
    ChatMessage findByChatRoomLastMessage(Long roomId);

    // INFO : 이 방에서 가장 최근 메시지의 id. "읽음 처리"를 이 값까지로 표시하는 데 쓴다.
    @Query("SELECT MAX(m.id) FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    Long findMaxMessageId(@Param("roomId") Long roomId);

    // INFO : employeeId 기준 안읽은 메시지 수 — 그 사람이 마지막으로 읽은 지점
    // (lastReadMessageId, EmployeeChatRoom에서 조회해서 넘겨받음)보다 뒤에 온, 본인이
    // 보내지 않은 메시지 개수. 사람마다 lastReadMessageId가 다르므로 그룹 채팅에서도
    // 참여자별로 정확하다.
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.employee.id <> :employeeId " +
            "AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)")
    long countUnread(@Param("roomId") Long roomId, @Param("employeeId") Long employeeId, @Param("lastReadMessageId") Long lastReadMessageId);

}
