package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.EmployeeChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * React 프론트로 실제 채팅방을 만들어보다가 실측으로 발견한 버그 2건에 대한 회귀 테스트.
 * <ol>
 *   <li>채팅방을 새로 만들고 바로 목록을 조회하면(=메시지가 하나도 없는 상태),
 *       {@code chatMessageRepository.findByChatRoomLastMessage()} 가 null을 반환하는데
 *       예전 코드는 여기서 바로 {@code .getMessage()} 를 불러서 NPE(500)가 났다.</li>
 *   <li>{@code ChatRoomListResponseDto.roomId} 에 실제 채팅방 id(chat_room.room_id)가
 *       아니라 참여자-채팅방 연결 테이블(employee_chatroom)의 PK가 잘못 채워지고 있었다.
 *       그 값으로 메시지 조회/전송을 호출하면 전부 "존재하지 않는 채팅방입니다"(404).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeChatRoomService employeeChatRoomService;
    @Mock
    private EmployeeChatRoomRepository employeeChatRoomRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @Test
    @DisplayName("메시지가 하나도 없는 새 채팅방이 섞여 있어도 NPE 없이 목록을 반환한다")
    void 메시지가_없는_채팅방도_목록_조회가_된다() {
        EmployeeChatRoom room = employeeChatRoomOf(1L, 100L, "김태동");
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmployeeChatRoom> page = new PageImpl<>(List.of(room), pageable, 1);

        given(employeeChatRoomService.findByEmployeeId(1L, pageable)).willReturn(page);
        given(chatMessageRepository.findByChatRoomLastMessage(100L)).willReturn(null); // 메시지 없음
        given(chatMessageRepository.findByIsReadCount(100L, 1L)).willReturn(0L);

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(1L, pageable);

        assertThat(result.getContents()).hasSize(1);
        ChatRoomListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getLastMessage()).isNull();
        assertThat(dto.getLastMessageTime()).isNull();
        assertThat(dto.getUserName()).isEqualTo("김태동");
        // roomId는 employee_chatroom PK(1L)가 아니라 실제 chat_room.room_id(100L)여야 한다 —
        // 이게 뒤바뀌면 프론트가 이 값으로 메시지 조회/전송을 호출할 때 전부 404가 난다.
        assertThat(dto.getRoomId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("메시지가 있는 채팅방은 마지막 메시지/시각을 정상적으로 채운다")
    void 메시지가_있으면_마지막_메시지를_채운다() {
        EmployeeChatRoom room = employeeChatRoomOf(2L, 200L, "이서연");
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmployeeChatRoom> page = new PageImpl<>(List.of(room), pageable, 1);
        LocalDateTime now = LocalDateTime.now();
        ChatMessage lastMessage = ChatMessage.builder().message("안녕하세요, 반갑습니다!").createTime(now).build();

        given(employeeChatRoomService.findByEmployeeId(2L, pageable)).willReturn(page);
        given(chatMessageRepository.findByChatRoomLastMessage(200L)).willReturn(lastMessage);
        given(chatMessageRepository.findByIsReadCount(200L, 2L)).willReturn(3L);

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(2L, pageable);

        ChatRoomListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getLastMessage()).isEqualTo("안녕하세요, 반갑습니다!");
        assertThat(dto.getLastMessageTime()).isEqualTo(now);
        assertThat(dto.getUnReadMessageCount()).isEqualTo(3L);
    }

    private EmployeeChatRoom employeeChatRoomOf(Long employeeChatRoomId, Long roomId, String employeeName) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(employeeChatRoomId);
        given(employee.getName()).willReturn(employeeName);
        given(employee.getEmployeeNo()).willReturn(employeeChatRoomId + 10000000L);
        given(employee.getPosition()).willReturn(com.example.chulgunhazabackend.domain.member.Position.EMPLOYEE);
        given(employee.getDepartment()).willReturn("개발팀");

        ChatRoom chatRoom = ChatRoom.builder().build();
        setField(chatRoom, "id", roomId);

        EmployeeChatRoom employeeChatRoom = EmployeeChatRoom.builder()
                .chatRoom(chatRoom)
                .employee(employee)
                .build();
        setField(employeeChatRoom, "id", employeeChatRoomId);
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
