package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
