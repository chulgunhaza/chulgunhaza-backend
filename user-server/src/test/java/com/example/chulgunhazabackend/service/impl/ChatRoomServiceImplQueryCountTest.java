package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.domain.member.EmployeeImage;
import com.example.chulgunhazabackend.domain.member.Gender;
import com.example.chulgunhazabackend.domain.member.Position;
import com.example.chulgunhazabackend.domain.member.UserRole;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.ChatRoomService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #77(Chat이 Employee를 @ManyToOne 객체 참조 대신 id로만 참조하도록 전환)에서
 * "방 목록 표시용 이름/부서는 EmployeeRepository.findAllById로 한 번에 조회해서
 * N+1 대신 IN 쿼리 한 번으로 끝난다"고 주장했던 걸 실제로 Hibernate Statistics로
 * 확인하고, 방 목록 페이지 조회의 쿼리 수를 성능 지표로 기록해서 회귀를 잡는 테스트.
 *
 * <p>실제 DB가 필요한 통합 테스트라 {@code generate_statistics}를 이 테스트에서만
 * 켠다(운영 설정은 안 건드림) — {@code hibernate.SessionFactory.getStatistics()}로
 * 이번 트랜잭션(정확히는 이 호출)에서 실행된 prepared statement 개수를 그대로 잰다.</p>
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
    private EmployeeRepository employeeRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // INFO : chat_message.employee_id / employee_chatroom.employee_id에는 여전히 DB
    // FK 제약이 걸려 있다(Phase 0에서 JPA 매핑만 id 참조로 바꿨고, ddl-auto: update는
    // 더 이상 필요 없어진 제약을 알아서 지워주진 않는다 — 실측으로 확인) — 그래서 정리할 때
    // 자식(메시지 → 참여 기록 → 방)부터 지우고 마지막에 사원을 지워야 FK 위반이 안 난다.
    private final List<Long> createdMessageIds = new ArrayList<>();
    private final List<Long> createdEmployeeChatRoomIds = new ArrayList<>();
    private final List<Long> createdRoomIds = new ArrayList<>();
    private final List<Long> createdEmployeeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdMessageIds.forEach(chatMessageRepository::deleteById);
        createdEmployeeChatRoomIds.forEach(employeeChatRoomRepository::deleteById);
        createdRoomIds.forEach(chatRoomRepository::deleteById);
        createdEmployeeIds.forEach(employeeRepository::deleteById);
        createdMessageIds.clear();
        createdEmployeeChatRoomIds.clear();
        createdRoomIds.clear();
        createdEmployeeIds.clear();
    }

    @Test
    @DisplayName("한 방의 참여자가 1명이든 5명이든, 이름/부서 조회 쿼리 수는 같다 (findAllById 배치 조회 — N+1 아님)")
    void 방_참여자_수가_늘어도_이름_조회_쿼리는_안_늘어난다() {
        // 뷰어를 방마다 따로 둬서 "그 뷰어의 방 목록 = 그 방 하나"가 되게 격리한다 —
        // 안 그러면 두 번째 측정 때 첫 번째 방까지 같이 잡혀서 비교가 오염된다.
        Long soloViewer = createEmployee("혼자뷰어");
        Long soloPartner = createEmployee("혼자상대");
        createRoomWith(soloViewer, List.of(soloPartner));

        Long groupViewer = createEmployee("그룹뷰어");
        List<Long> fivePartners = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fivePartners.add(createEmployee("그룹멤버" + i));
        }
        createRoomWith(groupViewer, fivePartners);

        Statistics statistics = statistics();

        statistics.clear();
        chatRoomService.getAllChatRoomsByEmployeeId(soloViewer, PageRequest.of(0, 10));
        long queriesForOneMember = statistics.getPrepareStatementCount();

        statistics.clear();
        chatRoomService.getAllChatRoomsByEmployeeId(groupViewer, PageRequest.of(0, 10));
        long queriesForFiveMembers = statistics.getPrepareStatementCount();

        System.out.println("[성능 지표] 참여자 1명 방 조회 쿼리 수: " + queriesForOneMember);
        System.out.println("[성능 지표] 참여자 5명 방 조회 쿼리 수: " + queriesForFiveMembers);

        // 핵심 주장: findAllById는 참여자 수와 무관하게 쿼리 1개(IN절)로 끝나니까,
        // 참여자가 1명이든 5명이든 방 하나를 조회하는 데 드는 쿼리 수는 같아야 한다.
        // 이게 어긋나면(늘어나면) 누군가 findAllById를 다시 개별 조회 루프로 되돌린 것.
        assertThat(queriesForFiveMembers).isEqualTo(queriesForOneMember);
    }

    @Test
    @DisplayName("방 목록 페이지 조회 쿼리 수를 성능 지표로 기록한다 (방 하나당 최대 4개: 참여자·이름조회·마지막메시지·안읽음수)")
    void 방_목록_페이지_조회_쿼리_수를_기록한다() {
        Long viewer = createEmployee("성능테스트뷰어");
        int roomCount = 10;
        for (int i = 0; i < roomCount; i++) {
            Long partner = createEmployee("상대" + i);
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
        // 방 하나당: 참여자 조회(findOtherMembersByChatRoomId) + 이름 조회(findAllById)
        // + 마지막 메시지(findByChatRoomLastMessage) + 안읽음 수(countUnread) = 최대 4개.
        // 여기에 "내가 속한 방 목록" 자체를 가져오는 쿼리(findByEmployeeId) 1개를 더해
        // 여유 있게 잡는다 — 이 값을 넘으면 방 단위 N+1이 새로 생긴 것(회귀 검증용 상한선).
        assertThat(totalQueries).isLessThanOrEqualTo(roomCount * 4L + 5);
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

    private Long createEmployee(String name) {
        EmployeeImage employeeImage = new EmployeeImage("imageName", "imagePath", 1L, "JPG");
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(0.0)
                .build();
        Employee employee = new Employee(
                name, "perf-test-" + System.nanoTime() + "@test.com",
                Gender.MALE, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), null,
                "성능테스트팀", Position.EMPLOYEE, List.of(UserRole.USER), employeeImage, annual
        );
        Long id = employeeRepository.save(employee).getId();
        createdEmployeeIds.add(id);
        return id;
    }
}
