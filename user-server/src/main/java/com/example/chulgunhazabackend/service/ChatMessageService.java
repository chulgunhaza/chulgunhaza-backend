package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageListResponseDto;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {

    Long saveChatMessage(ChatMessageCreateRMQDto chatMessageCreateRMQDto);

    PageDto<ChatMessageListResponseDto> getChatMessagesByRoomId(Long roomId, Long employeeId, Pageable pageable);

}
