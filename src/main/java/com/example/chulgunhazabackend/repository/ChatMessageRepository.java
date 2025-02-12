package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);

    @Query(value = "SELECT * FROM chat_message WHERE room_id = :roomId ORDER BY create_time DESC LIMIT 1", nativeQuery = true)
    ChatMessage findByChatRoomLastMessage(Long roomId);


    @Query(value = "SELECT COUNT(*) FROM chat_message WHERE room_id = :roomId AND is_read = false  AND employee_id <> :employeeId", nativeQuery = true)
    Long findByIsReadCount(Long roomId, Long employeeId);

    @Modifying
    @Query(value = "UPDATE chat_message c SET c.is_read = true WHERE room_id = :roomId AND employee_id <> :employeeId AND is_read = false", nativeQuery = true)
    void updateByChatRoomAndNotSendUserMessage(Long roomId, Long employeeId);

}
