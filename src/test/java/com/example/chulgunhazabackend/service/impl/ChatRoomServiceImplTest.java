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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
 * React 프론트로 실제 채팅방을 만들어보다가 실측으로 발견한 버그 2건에 대한 회귀 테스트 +
 * 단체 채팅(그룹) 지원 이후의 방 목록 조회 테스트.
 * <ol>
 *   <li>채팅방을 새로 만들고 바로 목록을 조회하면(=메시지가 하나도 없는 상태),
 *       {@code chatMessageRepository.findByChatRoomLastMessage()} 가 null을 반환하는데
 *       예전 코드는 여기서 바로 {@code .getMessage()} 를 불러서 NPE(500)가 났다.</li>
 *   <li>{@code ChatRoomListResponseDto.roomId} 에 실제 채팅방 id(chat_room.room_id)가
 *       아니라 참여자-채팅방 연결 테이블(employee_chatroom)의 PK가 잘못 채워지고 있었다.
 *       그 값으로 메시지 조회/전송을 호출하면 전부 "존재하지 않는 채팅방입니다"(404).</li>
 * </ol>
 * 단체 채팅 지원 이후엔 "내가 속한 방"을 {@code EmployeeChatRoomService.findByEmployeeId}
 * (내 참여 기록 기준, 방마다 1행)로 가져오고, 다른 참여자는 별도 쿼리
 * ({@code findOtherMembersByChatRoomId})로 조회해서 조합한다.
 */
// INFO : 헬퍼(employeeChatRoomOf)가 매번 employee의 5개 프로퍼티를 다 스텁하는데,
// "나 자신"의 참여 기록처럼 employee 값을 실제로 들여다보지 않는 테스트도 있어서
// strict stubbing이면 UnnecessaryStubbingException이 난다 — lenient로 완화.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        Long myEmployeeId = 1L;
        Long roomId = 100L;
        EmployeeChatRoom myMembership = employeeChatRoomOf(1L, roomId, myEmployeeId, "김태동");
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmployeeChatRoom> myRoomsPage = new PageImpl<>(List.of(myMembership), pageable, 1);
        EmployeeChatRoom other = employeeChatRoomOf(2L, roomId, 2L, "이서연");

        given(employeeChatRoomService.findByEmployeeId(myEmployeeId, pageable)).willReturn(myRoomsPage);
        given(employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, myEmployeeId)).willReturn(List.of(other));
        given(chatMessageRepository.findByChatRoomLastMessage(roomId)).willReturn(null); // 메시지 없음
        given(chatMessageRepository.countUnread(roomId, myEmployeeId, myMembership.getLastReadMessageId())).willReturn(0L);

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(myEmployeeId, pageable);

        assertThat(result.getContents()).hasSize(1);
        ChatRoomListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getLastMessage()).isNull();
        assertThat(dto.getLastMessageTime()).isNull();
        assertThat(dto.getRoomName()).isEqualTo("이서연");
        assertThat(dto.isGroup()).isFalse();
        // roomId는 employee_chatroom PK(1L)가 아니라 실제 chat_room.room_id(100L)여야 한다 —
        // 이게 뒤바뀌면 프론트가 이 값으로 메시지 조회/전송을 호출할 때 전부 404가 난다.
        assertThat(dto.getRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("메시지가 있는 채팅방은 마지막 메시지/시각을 정상적으로 채운다")
    void 메시지가_있으면_마지막_메시지를_채운다() {
        Long myEmployeeId = 1L;
        Long roomId = 200L;
        EmployeeChatRoom myMembership = employeeChatRoomOf(1L, roomId, myEmployeeId, "김태동");
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmployeeChatRoom> myRoomsPage = new PageImpl<>(List.of(myMembership), pageable, 1);
        EmployeeChatRoom other = employeeChatRoomOf(3L, roomId, 2L, "이서연");
        LocalDateTime now = LocalDateTime.now();
        ChatMessage lastMessage = ChatMessage.builder().message("안녕하세요, 반갑습니다!").createTime(now).build();

        given(employeeChatRoomService.findByEmployeeId(myEmployeeId, pageable)).willReturn(myRoomsPage);
        given(employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, myEmployeeId)).willReturn(List.of(other));
        given(chatMessageRepository.findByChatRoomLastMessage(roomId)).willReturn(lastMessage);
        given(chatMessageRepository.countUnread(roomId, myEmployeeId, myMembership.getLastReadMessageId())).willReturn(3L);

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(myEmployeeId, pageable);

        ChatRoomListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getLastMessage()).isEqualTo("안녕하세요, 반갑습니다!");
        assertThat(dto.getLastMessageTime()).isEqualTo(now);
        assertThat(dto.getUnReadMessageCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("참여자가 3명 이상이면 단체 채팅방으로 표시되고, 방 목록엔 중복 없이 1건만 나온다")
    void 참여자가_여럿이면_단체_채팅방이다() {
        Long myEmployeeId = 1L;
        Long roomId = 300L;
        EmployeeChatRoom myMembership = employeeChatRoomOf(10L, roomId, myEmployeeId, "김태동");
        Pageable pageable = PageRequest.of(0, 20);
        // INFO : 내가 속한 방은 이 그룹방 1개뿐 — "내 참여 기록" 기준 조회이므로 다른 참여자
        // 수와 무관하게 방 목록엔 정확히 1건만 나와야 한다 (예전 방식은 참여자 수만큼 중복 노출됨).
        Page<EmployeeChatRoom> myRoomsPage = new PageImpl<>(List.of(myMembership), pageable, 1);
        EmployeeChatRoom member2 = employeeChatRoomOf(11L, roomId, 2L, "이서연");
        EmployeeChatRoom member3 = employeeChatRoomOf(12L, roomId, 3L, "박민준");

        given(employeeChatRoomService.findByEmployeeId(myEmployeeId, pageable)).willReturn(myRoomsPage);
        given(employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, myEmployeeId)).willReturn(List.of(member2, member3));
        given(chatMessageRepository.findByChatRoomLastMessage(roomId)).willReturn(null);
        given(chatMessageRepository.countUnread(roomId, myEmployeeId, myMembership.getLastReadMessageId())).willReturn(0L);

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(myEmployeeId, pageable);

        assertThat(result.getContents()).hasSize(1);
        ChatRoomListResponseDto dto = result.getContents().get(0);
        assertThat(dto.isGroup()).isTrue();
        assertThat(dto.getRoomName()).isEqualTo("이서연, 박민준");
        assertThat(dto.getMembers()).hasSize(2);
    }

    @Test
    @DisplayName("그룹 채팅방에서 안읽은 메시지 수는 각자 마지막으로 읽은 지점 기준으로 사람마다 다르게 계산된다")
    void 그룹_채팅방_안읽은_메시지_수는_사람마다_다르다() {
        Long roomId = 500L;
        Long meId = 1L;
        Long otherMemberId = 2L;

        // "나"는 15번 메시지까지 읽었고, 같은 방의 다른 참여자는 5번 메시지까지만 읽었다 —
        // 예전엔(메시지당 is_read boolean 하나) 한쪽이 방을 열어보면 둘 다 읽음 처리돼버렸지만,
        // 이제는 EmployeeChatRoom.lastReadMessageId가 사람별로 따로 있어서 서로 영향을 안 준다.
        EmployeeChatRoom myMembership = employeeChatRoomOf(1L, roomId, meId, "김태동");
        setField(myMembership, "lastReadMessageId", 15L);
        EmployeeChatRoom otherMembership = employeeChatRoomOf(2L, roomId, otherMemberId, "이서연");
        setField(otherMembership, "lastReadMessageId", 5L);

        Pageable pageable = PageRequest.of(0, 20);

        // 내 관점으로 방 목록을 조회하면: 다른 참여자는 "이서연" 1명, 내 lastReadMessageId(15) 기준.
        given(employeeChatRoomService.findByEmployeeId(meId, pageable))
                .willReturn(new PageImpl<>(List.of(myMembership), pageable, 1));
        given(employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, meId))
                .willReturn(List.of(otherMembership));
        given(chatMessageRepository.findByChatRoomLastMessage(roomId)).willReturn(null);
        given(chatMessageRepository.countUnread(roomId, meId, 15L)).willReturn(2L);

        // 같은 방을 다른 참여자 관점으로 조회하면: 상대는 "나", 그 사람의 lastReadMessageId(5) 기준.
        given(employeeChatRoomService.findByEmployeeId(otherMemberId, pageable))
                .willReturn(new PageImpl<>(List.of(otherMembership), pageable, 1));
        given(employeeChatRoomRepository.findOtherMembersByChatRoomId(roomId, otherMemberId))
                .willReturn(List.of(myMembership));
        given(chatMessageRepository.countUnread(roomId, otherMemberId, 5L)).willReturn(9L);

        Long myUnread = chatRoomService.getAllChatRoomsByEmployeeId(meId, pageable)
                .getContents().get(0).getUnReadMessageCount();
        Long otherUnread = chatRoomService.getAllChatRoomsByEmployeeId(otherMemberId, pageable)
                .getContents().get(0).getUnReadMessageCount();

        assertThat(myUnread).isEqualTo(2L);
        assertThat(otherUnread).isEqualTo(9L);
        // 같은 방인데도 서로 다른 값이 나온다는 게 핵심 — 사람별로 독립적으로 추적된다는 증거.
        assertThat(myUnread).isNotEqualTo(otherUnread);
    }

    @Test
    @DisplayName("채팅방을 나가면 내 참여 기록만 삭제된다")
    void 채팅방을_나가면_내_참여_기록만_삭제된다() {
        Long myEmployeeId = 1L;
        Long roomId = 400L;
        EmployeeChatRoom myMembership = employeeChatRoomOf(20L, roomId, myEmployeeId, "김태동");

        given(employeeChatRoomRepository.findByEmployeeIdAndChatRoomId(myEmployeeId, roomId)).willReturn(java.util.Optional.of(myMembership));

        chatRoomService.leaveChatRoom(roomId, myEmployeeId);

        org.mockito.Mockito.verify(employeeChatRoomRepository).delete(myMembership);
    }

    @Test
    @DisplayName("memberIds에 개설자 본인 id가 섞여 있으면 걸러내고, 걸러낸 뒤 남는 상대가 없으면 예외를 던진다")
    void 대화_상대에_본인_id가_섞여있으면_걸러낸다() {
        Long myEmployeeId = 1L;
        Long otherId = 2L;

        // 본인 id + 실제 상대 id가 같이 온 경우 — 본인 id는 걸러내고 상대 1명으로만 방이 만들어져야
        // 한다. (예전엔 안 걸러내서 employee_chatroom에 본인 행이 중복 저장되고, 방 목록엔
        // "다른 참여자 없음" 취급으로 "(대화 상대 없음)"이 뜨는 버그가 있었다 — 실측으로 발견)
        com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto dto =
                new com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto();
        dto.setMemberIds(List.of(myEmployeeId, otherId));

        given(employeeChatRoomRepository.findRoomIdByEmployeeIds(myEmployeeId, otherId))
                .willReturn(java.util.Optional.empty());
        ChatRoom savedRoom = ChatRoom.builder().build();
        setField(savedRoom, "id", 999L);
        given(chatRoomRepository.save(org.mockito.ArgumentMatchers.any())).willReturn(savedRoom);
        Employee me = mock(Employee.class);
        given(me.getId()).willReturn(myEmployeeId);
        Employee other = mock(Employee.class);
        given(other.getId()).willReturn(otherId);
        given(employeeRepository.findEmployeeById(myEmployeeId)).willReturn(java.util.Optional.of(me));
        given(employeeRepository.findEmployeeById(otherId)).willReturn(java.util.Optional.of(other));

        chatRoomService.saveChatRoom(dto, myEmployeeId);

        // 본인(1L)은 memberEmployees 목록에 안 들어가고 상대(2L)만 들어가서 저장 호출돼야 한다.
        org.mockito.Mockito.verify(employeeChatRoomService)
                .save(org.mockito.ArgumentMatchers.eq(savedRoom), org.mockito.ArgumentMatchers.eq(me), org.mockito.ArgumentMatchers.eq(List.of(other)));
    }

    @Test
    @DisplayName("memberIds가 본인 id뿐이면(=걸러내고 나면 상대가 없으면) 예외를 던진다")
    void 대화_상대가_본인뿐이면_예외() {
        Long myEmployeeId = 1L;
        com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto dto =
                new com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto();
        dto.setMemberIds(List.of(myEmployeeId));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> chatRoomService.saveChatRoom(dto, myEmployeeId))
                .isInstanceOf(com.example.chulgunhazabackend.exception.chatException.ChatException.class);

        org.mockito.Mockito.verify(chatRoomRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    private EmployeeChatRoom employeeChatRoomOf(Long employeeChatRoomId, Long roomId, Long employeeId, String employeeName) {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(employeeId);
        given(employee.getName()).willReturn(employeeName);
        given(employee.getEmployeeNo()).willReturn(employeeId + 10000000L);
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
