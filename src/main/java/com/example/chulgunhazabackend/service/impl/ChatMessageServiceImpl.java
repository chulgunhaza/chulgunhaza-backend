package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageListResponseDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;

    @Override
    public Long saveChatMessage(ChatMessageCreateRMQDto chatMessageCreateRMQDto) {

        // INFO: 채팅방 존재 유무 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageCreateRMQDto.getRoomId()).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM));

        // INFO: 전송하는 사원의 정보
        Employee sendEmployee = employeeRepository.findEmployeeById(chatMessageCreateRMQDto.getSenderId()).orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        // INFO: 전송받는 사원의 유무 확인
        Employee receiveEmployee = employeeRepository.findEmployeeById(chatMessageCreateRMQDto.getReceiverId()).orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        // INFO: 채팅방 인원이 아닌데 전송하는 경우 예외 처리
        EmployeeChatRoom employeeChatRoom = employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(sendEmployee.getId(), chatRoom.getId()).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER));

        return chatMessageRepository.save(chatMessageCreateRMQDto.toEntity(chatRoom, sendEmployee)).getId();
    }

    @Override
    public PageDto<ChatMessageListResponseDto> getChatMessagesByRoomId(Long roomId, Long employeeId, Pageable pageable) {

        // INFO : 채팅방 존재 유무 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM));

        // INFO : 채팅방 별 메시지 가져오기
        Page<ChatMessage> contents = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable);

        // INFO : 채팅방 읽음 처리
        // HACK : 추후 배치 처리 필요.
        chatMessageRepository.updateByChatRoomAndNotSendUserMessage(roomId, employeeId);

        // INFO : PAGING
        Page<ChatMessageListResponseDto> pageDto = contents.map(chatMessage -> new ChatMessageListResponseDto().fromEntity(chatMessage));


        return new PageDto<>(pageDto) ;
    }
}
