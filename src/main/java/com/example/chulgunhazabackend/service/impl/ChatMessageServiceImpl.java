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

import java.util.List;



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

        // INFO: 채팅방 인원이 아닌데 전송하는 경우 예외 처리
        // (수신자는 단체 채팅에서 여러 명일 수 있어 더 이상 여기서 검증하지 않는다 — 실시간 전달
        // 대상은 ChatRabbitMQMessageServiceImpl에서 방 참여자 전원을 조회해서 결정한다.)
        EmployeeChatRoom employeeChatRoom = employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(sendEmployee.getId(), chatRoom.getId()).orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER));

        return chatMessageRepository.save(chatMessageCreateRMQDto.toEntity(chatRoom, sendEmployee)).getId();
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
        // HACK : 추후 배치 처리 필요.
        Long maxMessageId = chatMessageRepository.findMaxMessageId(roomId);
        if (maxMessageId != null) {
            employeeChatRoomRepository.markReadUpTo(employeeId, roomId, maxMessageId);
        }

        // INFO : 메시지별 "안읽은 사람 수"를 계산하려고 방 참여자 전원(과 각자의 lastReadMessageId)을
        // 한 번만 불러온다 — 메시지마다 쿼리 날리면 페이지당 N+1이 나서 여기서 한 번에 처리.
        List<EmployeeChatRoom> members = employeeChatRoomRepository.findByChatRoomId(roomId);

        // INFO : PAGING
        Page<ChatMessageListResponseDto> pageDto = contents.map(chatMessage -> {
            long unReadCount = members.stream()
                    .filter(member -> !member.getEmployee().getId().equals(chatMessage.getEmployee().getId())) // 발신자 본인 제외
                    .filter(member -> member.getLastReadMessageId() == null || member.getLastReadMessageId() < chatMessage.getId())
                    .count();
            return ChatMessageListResponseDto.fromEntity(chatMessage, unReadCount);
        });

        return new PageDto<>(pageDto) ;
    }
}
