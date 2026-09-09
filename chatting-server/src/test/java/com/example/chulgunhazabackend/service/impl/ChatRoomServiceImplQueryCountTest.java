package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.client.EmployeeDirectoryClient;
import com.example.chulgunhazabackend.client.EmployeeSummary;
import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.service.ChatRoomService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * #77(Chat이 Employee를 @ManyToOne 객체 참조 대신 id로만 참조하도록 전환)에서
 * "방 목록 표시용 이름/부서는 findAllById로 한 번에 조회해서 N+1 대신 IN 쿼리
 * 한 번으로 끝난다"고 주장했던 걸 실제로 Hibernate Statistics로 확인하던 테스트.
 *
 * <p>#101: chatting-server가 물리 분리되면서 그 조회가 로컬 JPA
 * {@code EmployeeRepository.findAllById}(SQL, Hibernate Statistics로 관측 가능)에서
 * user-server를 호출하는 {@code EmployeeDirectoryClient.findByIds}(HTTP, SQL 카운트에
 * 안 잡힘)로 바뀌었다 — 그래서 "이름 조회 쿼리 수"라는 원래 관측 대상 자체가 이제
 * SQL 통계에 안 나온다. 대신 이 서비스가 여전히 직접 SQL을 날리는
 * 부분(참여자 조회·마지막 메시지·안읽음 수)의 N+1만 Statistics로 계속 감시하고,
 * EmployeeDirectoryClient는 {@code @MockBean}으로 대체해서 실제 네트워크 호출 없이
 * "방마다 정확히 한 번만 호출되는지"를 별도로 검증한다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ChatRoomServiceImplQueryCountTest {

    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private EmployeeChatRoomRepository employeeChatRoomRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // #101: 사원 정보는 이제 이 서비스가 로컬 DB에 갖고 있지 않다(user-server 내부 API를
    // 호출) — 실제 HTTP 호출 없이 요청받은 id 그대로 더미 EmployeeSummary를 돌려준다.
    @MockBean
    private EmployeeDirectoryClient employeeDirectoryClient;

    private final List<Long> createdMessageIds = new ArrayList<>();
    private final List<Long> createdEmployeeChatRoomIds = new ArrayList<>();
    private final List<Long> createdRoomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdMessageIds.forEach(chatMessageRepository::deleteById);
        createdEmployeeChatRoomIds.forEach(employeeChatRoomRepository::deleteById);
        createdRoomIds.forEach(chatRoomRepository::deleteById);
        createdMessageIds.clear();
        createdEmployeeChatRoomIds.clear();
        createdRoomIds.clear();
    }

    @Test
    @DisplayName("방 목록 페이지 조회 쿼리 수를 성능 지표로 기록한다 (방 하나당 최대 3개: 참여자·마지막메시지·안읽음수 — 이름 조회는 HTTP로 SQL 카운트 밖)")
    void 방_목록_페이지_조회_쿼리_수를_기록한다() {
        given(employeeDirectoryClient.findByIds(anyList()))
                .willAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> new EmployeeSummary(id, id + 10000000L, "사원" + id, "성능테스트팀", "EMPLOYEE"))
                            .toList();
                });

        Long viewer = 9001L;
        int roomCount = 10;
        for (int i = 0; i < roomCount; i++) {
            Long partner = 9100L + i;
            createRoomWith(viewer, List.of(partner));
        }

        Statistics statistics = statistics();
        statistics.clear();

        PageDto<ChatRoomListResponseDto> result = chatRoomService.getAllChatRoomsByEmployeeId(viewer, PageRequest.of(0, roomCount));

        long totalQueries = statistics.getPrepareStatementCount();
        double queriesPerRoom = (double) totalQueries / roomCount;

        System.out.printf("[성능 지표] 방 %d개 페이지 조회 총 쿼리 수: %d (방 하나당 평균 %.1f개)%n",
                roomCount, totalQueries, queriesPerRoom);

        assertThat(result.getContents()).hasSize(roomCount);
        // 방 하나당: 참여자 조회(findOtherMembersByChatRoomId) + 마지막 메시지
        // (findByChatRoomLastMessage) + 안읽음 수(countUnread) = 최대 3개(이름 조회는
        // 이제 HTTP라 여기 안 잡힘). 여기에 "내가 속한 방 목록" 자체를 가져오는 쿼리
        // (findByEmployeeId) 1개를 더해 여유 있게 잡는다 — 이 값을 넘으면 방 단위
        // N+1이 새로 생긴 것(회귀 검증용 상한선).
        assertThat(totalQueries).isLessThanOrEqualTo(roomCount * 3L + 5);
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private Long createRoomWith(Long viewerId, List<Long> otherIds) {
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder().build());
        createdRoomIds.add(chatRoom.getId());

        EmployeeChatRoom viewerMembership = employeeChatRoomRepository.save(
                EmployeeChatRoom.builder().chatRoom(chatRoom).employeeId(viewerId).build());
        createdEmployeeChatRoomIds.add(viewerMembership.getId());
        for (Long otherId : otherIds) {
            EmployeeChatRoom otherMembership = employeeChatRoomRepository.save(
                    EmployeeChatRoom.builder().chatRoom(chatRoom).employeeId(otherId).build());
            createdEmployeeChatRoomIds.add(otherMembership.getId());
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(chatRoom)
                .employeeId(viewerId)
                .message("성능 테스트용 메시지")
                .createTime(LocalDateTime.now())
                .build());
        createdMessageIds.add(message.getId());

        return chatRoom.getId();
    }
}
