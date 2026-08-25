package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
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

import java.util.List;
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

        List<Long> memberIds = chatRoomCreateRequestDto.getMemberIds();

        // INFO : 1:1 채팅은 기존 방이 있으면 재사용(중복 생성 방지). 단체 채팅(2명 이상)은
        // "같은 멤버 조합"을 매칭하는 로직이 없으므로 매번 새 방을 만든다 — 사용자가 같은
        // 사람들과 여러 개의 대화방을 만들 수도 있으니 자연스러운 동작이다.
        if (memberIds.size() == 1) {
            Optional<Long> id = employeeChatRoomRepository.findRoomIdByEmployeeIds(employeeId, memberIds.get(0));
            if (id.isPresent()) {
                throw new ChatException(ChatExceptionType.ALREADY_CHAT_ROOM);
            }
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder().build());

        Employee senderEmployee = employeeRepository.findEmployeeById(employeeId).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        List<Employee> members = memberIds.stream()
                .map(id -> employeeRepository.findEmployeeById(id).orElseThrow(()
                        -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER)))
                .toList();

        employeeChatRoomService.save(chatRoom, senderEmployee, members);

        return chatRoom.getId();

    }

    // INFO : 전송 사원의 아이디로 해당 사원의 채팅방 목록 가져오기
    @Override
    public PageDto<ChatRoomListResponseDto> getAllChatRoomsByEmployeeId(Long employeeId, Pageable pageable) {

        // INFO : 사원이 속한 채팅 방을 가져오는 로직 (방 하나당 내 참여 기록 1건 — 그룹 채팅에서도 중복 없음)
        Page<EmployeeChatRoom> myRooms = employeeChatRoomService.findByEmployeeId(employeeId, pageable);

        // INFO : 가져온 데이터를 PageDto 에 넣기 위한 타입으로 변환
        // 아직 메시지가 하나도 없는 새 채팅방은 findByChatRoomLastMessage가 null을 반환한다.
        // 예전엔 여기서 바로 .getMessage()를 호출해서 NPE(500)가 났다 — 방을 막 만들고
        // 목록을 조회하면 100% 재현됨. null-safe하게 고치면서 중복 조회도 한 번으로 줄였다.
        Page<ChatRoomListResponseDto> list = myRooms.map(myRoom -> {
            Long roomId = myRoom.getChatRoom().getId();
            List<EmployeeChatRoom> otherMembers = employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, employeeId);
            ChatMessage lastMessage = chatMessageRepository.findByChatRoomLastMessage(roomId);
            return ChatRoomListResponseDto.fromEntity(
                    roomId,
                    otherMembers,
                    lastMessage != null ? lastMessage.getMessage() : null,
                    chatMessageRepository.countUnread(roomId, employeeId, myRoom.getLastReadMessageId()),
                    lastMessage != null ? lastMessage.getCreateTime() : null
            );
        });

        return new PageDto<ChatRoomListResponseDto>(list);

    }

    // INFO : 채팅방 나가기 — 내 참여 기록만 삭제한다. 방/메시지는 그대로 남지만 나머지
    // 참여자에게는 계속 보이고, 모든 참여자가 나가면 그 방은 자연히 아무 목록에도 나오지 않는다.
    @Override
    public void leaveChatRoom(Long roomId, Long employeeId) {
        EmployeeChatRoom membership = employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(employeeId, roomId)
                .orElseThrow(() -> new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER));
        employeeChatRoomRepository.delete(membership);
    }
}
