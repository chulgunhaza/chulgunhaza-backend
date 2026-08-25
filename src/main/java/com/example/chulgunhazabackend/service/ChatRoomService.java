package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import org.springframework.data.domain.Pageable;


public interface ChatRoomService {

    Long saveChatRoom(ChatRoomCreateRequestDto chatRoomCreateRequestDto, Long employeeId);

    PageDto<ChatRoomListResponseDto> getAllChatRoomsByEmployeeId(Long employeeId, Pageable pageable);

    // INFO : 채팅방 나가기 — 내 참여 기록만 삭제한다 (메시지/방 자체는 보존).
    void leaveChatRoom(Long roomId, Long employeeId);

}
