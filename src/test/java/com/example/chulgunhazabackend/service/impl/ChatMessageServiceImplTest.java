package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageListResponseDto;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * "단톡방에서 2명이 안 읽었으면 2로 표시돼야 한다"는 피드백으로 다시 설계한 메시지별
 * 안읽은 인원 수(unReadCount) 계산 로직 검증. 카카오톡 단톡방처럼 각자 읽을 때마다 그
 * 메시지의 unReadCount가 하나씩 줄어들고, 발신자를 제외한 전원이 읽으면 0이 되어야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeChatRoomRepository employeeChatRoomRepository;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    @Test
    @DisplayName("4인방에서 발신자를 제외한 나머지 중 아직 2명이 안 읽었으면 unReadCount는 2다")
    void 단톡방_메시지의_안읽은_인원수를_계산한다() {
        Long roomId = 900L;
        Long viewerId = 1L;        // 지금 방을 조회하는 사람 — 이미 최신까지 읽은 상태라 계산엔 안 잡힘
        Long senderId = 2L;        // 메시지를 보낸 사람 — 안읽은 인원 수 계산에서 항상 제외
        Long unreadMemberA = 3L;   // 훨씬 전 메시지까지만 읽어서 이 메시지는 아직 안 읽음
        Long unreadMemberB = 4L;   // 한 번도 안 읽음(lastReadMessageId null)

        ChatRoom chatRoom = ChatRoom.builder().build();
        setField(chatRoom, "id", roomId);

        Employee sender = mock(Employee.class);
        given(sender.getId()).willReturn(senderId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .employee(sender)
                .message("안녕하세요")
                .createTime(LocalDateTime.now())
                .build();
        setField(message, "id", 50L);

        Pageable pageable = PageRequest.of(0, 30);
        Page<ChatMessage> messagePage = new PageImpl<>(List.of(message), pageable, 1);

        EmployeeChatRoom senderMembership = employeeChatRoomOf(senderId, 999L);       // 발신자는 제외되니 값은 무관
        EmployeeChatRoom viewerMembership = employeeChatRoomOf(viewerId, 60L);        // 이미 50보다 뒤까지 읽음
        EmployeeChatRoom unreadAMembership = employeeChatRoomOf(unreadMemberA, 10L);  // 50보다 한참 이전까지만
        EmployeeChatRoom unreadBMembership = employeeChatRoomOf(unreadMemberB, null); // 한 번도 안 읽음

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable)).willReturn(messagePage);
        given(chatMessageRepository.findMaxMessageId(roomId)).willReturn(50L);
        given(employeeChatRoomRepository.findByChatRoomId(roomId))
                .willReturn(List.of(senderMembership, viewerMembership, unreadAMembership, unreadBMembership));

        PageDto<ChatMessageListResponseDto> result = chatMessageService.getChatMessagesByRoomId(roomId, viewerId, pageable);

        // viewer가 방을 열어봤다는 읽음 처리 자체는 정상 호출됐는지 별도로 확인.
        verify(employeeChatRoomRepository).markReadUpTo(viewerId, roomId, 50L);

        ChatMessageListResponseDto dto = result.getContents().get(0);
        // 발신자(senderId)·이미 읽은 viewer는 빠지고, unreadMemberA/B 2명만 안읽은 인원으로 잡힌다.
        assertThat(dto.getUnReadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("발신자를 제외한 참여자 전원이 이미 읽었으면 unReadCount는 0이다")
    void 전원이_읽으면_안읽은_인원수는_0이다() {
        Long roomId = 901L;
        Long viewerId = 1L;
        Long senderId = 2L;

        ChatRoom chatRoom = ChatRoom.builder().build();
        setField(chatRoom, "id", roomId);

        Employee sender = mock(Employee.class);
        given(sender.getId()).willReturn(senderId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .employee(sender)
                .message("안녕하세요")
                .createTime(LocalDateTime.now())
                .build();
        setField(message, "id", 50L);

        Pageable pageable = PageRequest.of(0, 30);
        Page<ChatMessage> messagePage = new PageImpl<>(List.of(message), pageable, 1);

        EmployeeChatRoom senderMembership = employeeChatRoomOf(senderId, 999L);
        EmployeeChatRoom viewerMembership = employeeChatRoomOf(viewerId, 60L); // 이미 50보다 뒤까지 읽음

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable)).willReturn(messagePage);
        given(chatMessageRepository.findMaxMessageId(roomId)).willReturn(50L);
        given(employeeChatRoomRepository.findByChatRoomId(roomId))
                .willReturn(List.of(senderMembership, viewerMembership));

        PageDto<ChatMessageListResponseDto> result = chatMessageService.getChatMessagesByRoomId(roomId, viewerId, pageable);

        ChatMessageListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getUnReadCount()).isEqualTo(0L);
    }

    private EmployeeChatRoom employeeChatRoomOf(Long employeeId, Long lastReadMessageId) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(employeeId);

        EmployeeChatRoom employeeChatRoom = EmployeeChatRoom.builder()
                .employee(employee)
                .build();
        setField(employeeChatRoom, "lastReadMessageId", lastReadMessageId);
        return employeeChatRoom;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
