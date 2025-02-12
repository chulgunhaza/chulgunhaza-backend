package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import org.springframework.data.domain.Pageable;


public interface ChatRoomService {

    Long saveChatRoom(ChatRoomCreateRequestDto chatRoomCreateRequestDto, Long employeeId);

    PageDto<ChatRoomListResponseDto> getAllChatRoomsByEmployeeId(Long employeeId, Pageable pageable);

}
