package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.ChatRoomService;
import com.example.chulgunhazabackend.service.EmployeeChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeChatRoomService employeeChatRoomService;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public Long saveChatRoom(ChatRoomCreateRequestDto chatRoomCreateRequestDto, Long employeeId) {

        // INFO : 생성된 채팅방인지 검증
        Optional<Long> id = employeeChatRoomRepository.findRoomIdByEmployeeIds(employeeId, chatRoomCreateRequestDto.getReceiverId());

        // INFO : 존재 시 예외 발생
        if(id.isPresent()){
            throw new ChatException(ChatExceptionType.ALREADY_CHAT_ROOM);
        }

        // INFO : 존재 하지 않을 시 새로운 채팅 방 생성
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder().build());

        // INFO : 전송자, 수신자 아이디를 새로운 채팅방에 추가
        Employee senderEmployee = employeeRepository.findEmployeeById(chatRoomCreateRequestDto.getSenderId()).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        Employee receiverEmployee = employeeRepository.findEmployeeById(chatRoomCreateRequestDto.getReceiverId()).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        // INFO : 관련 채팅방에 유저 저장 로직
        employeeChatRoomService.save(chatRoom, senderEmployee, receiverEmployee);

        return chatRoom.getId();
        
    }

    // INFO : 전송 사원의 아이디로 해당 사원의 채팅방 목록 가져오기
    @Override
    public PageDto<ChatRoomListResponseDto> getAllChatRoomsByEmployeeId(Long employeeId, Pageable pageable) {

        // INFO : 사원이 속한 채팅 방을 가져오는 로직
        Page<EmployeeChatRoom> contents = employeeChatRoomService.findByEmployeeId(employeeId, pageable);



        // INFO : 가져온 데이터를 PageDto 에 넣기 위한 타입으로 변환
        Page<ChatRoomListResponseDto> list =  contents.map(employeeChatRoom -> new ChatRoomListResponseDto()
                .fromEntity( employeeChatRoom
                            , chatMessageRepository.findByChatRoomLastMessage(employeeChatRoom.getChatRoom().getId()).getMessage()
                            , chatMessageRepository.findByIsReadCount(employeeChatRoom.getChatRoom().getId(), employeeId)
                            , chatMessageRepository.findByChatRoomLastMessage(employeeChatRoom.getChatRoom().getId()).getCreateTime()));

        return new PageDto<ChatRoomListResponseDto>(list);

    }
}
