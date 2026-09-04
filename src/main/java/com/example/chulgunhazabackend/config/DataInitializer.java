package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.domain.member.EmployeeImage;
import com.example.chulgunhazabackend.domain.member.Gender;
import com.example.chulgunhazabackend.domain.member.Position;
import com.example.chulgunhazabackend.domain.member.UserRole;
import com.example.chulgunhazabackend.repository.ChatMessageRepository;
import com.example.chulgunhazabackend.repository.ChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 이 프로젝트를 처음 받은 사람이 로그인해볼 계정도, 채팅 화면에서 훑어볼 데이터도
// 하나 없었다 — data.sql이나 CommandLineRunner 같은 시딩 코드가 전혀 없어서, DB를
// 새로 붙이면 회원가입 API도 없는 채로 로그인할 방법 자체가 없고, 로그인해도 빈
// 화면만 보이는 상태였다(온보딩 정리하다가 실측으로 발견). 서버 기동 시 한 번만
// (멱등하게) 로그인 계정 + 채팅방/메시지를 만들어둔다.
//
// 로컬 개발용 편의 기능이라 기본은 켜져 있고, 운영 환경에 실제로 배포할 때는
// application-prod.yml 같은 프로필에서 app.seed-demo-account=false로 꺼두면 된다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-account", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    public static final String SEED_EMAIL = "test@chulgunhaza.com";
    public static final String SEED_PASSWORD = "test1234!";

    private static final int ROOM_COUNT = 10;
    private static final int MESSAGES_PER_ROOM = 250;

    // 시딩 전용 동료 10명 — 실제로 있을 법한 이름이지만 이메일은 seed- 접두사로 못박아서,
    // 이미 수동으로 만들어둔 다른 테스트 계정(예: 이하나)과 절대 안 겹치게 한다.
    private static final String[] COLLEAGUE_NAMES = {
            "박서준", "김나윤", "이도현", "정유진", "최민석",
            "강하은", "조성민", "윤지수", "임태양", "한소율",
    };

    private static final String[] SAMPLE_MESSAGES = {
            "네 확인했습니다.", "오늘 회의 몇 시죠?", "자료 공유드립니다.", "넵!",
            "점심 같이 하실래요?", "수고하셨습니다.", "확인 후 회신드릴게요.", "감사합니다.",
            "잠시 통화 가능하신가요?", "네 알겠습니다.", "그 건은 제가 처리할게요.",
            "일정 공유 감사합니다.", "혹시 자료 다시 보내주실 수 있나요?", "네네 좋습니다.",
            "오늘 안으로 마무리하겠습니다.", "고생 많으셨어요!", "내일 오전에 뵙겠습니다.",
            "네 그렇게 진행할게요.", "리뷰 부탁드립니다.", "확인했습니다, 감사합니다!",
    };

    private final EmployeeRepository employeeRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EmployeeChatRoomRepository employeeChatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Employee seedEmployee = employeeRepository.findEmployeeByEmail(SEED_EMAIL)
                .orElseGet(this::createSeedEmployee);

        seedChatData(seedEmployee.getId());
    }

    private Employee createSeedEmployee() {
        Employee employee = Employee.builder()
                .name("테스트")
                .email(SEED_EMAIL)
                .gender(Gender.MALE)
                .birthdate(LocalDate.of(1995, 1, 1))
                .hireDate(LocalDate.now())
                .department("태동팀")
                .position(Position.EMPLOYEE)
                .userRoleList(List.of(UserRole.USER))
                .employeeImage(new EmployeeImage())
                .annual(new Annual())
                .build();
        employee.updatePassword(passwordEncoder, SEED_PASSWORD);

        Employee saved = employeeRepository.save(employee);
        log.info("시드 계정 생성됨 — email: {}, password: {} (로컬 개발 전용, app.seed-demo-account=false로 끌 수 있음)",
                SEED_EMAIL, SEED_PASSWORD);
        return saved;
    }

    // 시드 계정이 참여한 방이 하나라도 있으면 이미 이전에 채팅까지 시딩된 것으로 보고
    // 건너뛴다 — 재기동할 때마다 방/메시지가 계속 늘어나지 않게 하는 멱등성 가드.
    private void seedChatData(Long seedEmployeeId) {
        boolean alreadySeeded = employeeChatRoomRepository
                .findByEmployeeId(seedEmployeeId, PageRequest.of(0, 1))
                .hasContent();
        if (alreadySeeded) {
            return;
        }

        for (int roomIndex = 0; roomIndex < ROOM_COUNT; roomIndex++) {
            Long colleagueId = findOrCreateColleague(COLLEAGUE_NAMES[roomIndex], roomIndex).getId();
            createRoomWithMessages(seedEmployeeId, colleagueId, roomIndex);
        }

        log.info("시드 채팅 데이터 생성됨 — 채팅방 {}개, 방당 메시지 {}개", ROOM_COUNT, MESSAGES_PER_ROOM);
    }

    private Employee findOrCreateColleague(String name, int index) {
        String email = "seed-colleague-%02d@chulgunhaza.com".formatted(index + 1);
        return employeeRepository.findEmployeeByEmail(email).orElseGet(() -> {
            Employee colleague = Employee.builder()
                    .name(name)
                    .email(email)
                    .gender(index % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                    .birthdate(LocalDate.of(1990 + index, 1, 1))
                    .hireDate(LocalDate.now().minusYears(index + 1))
                    .department("태동팀")
                    .position(Position.EMPLOYEE)
                    .userRoleList(List.of(UserRole.USER))
                    .employeeImage(new EmployeeImage())
                    .annual(new Annual())
                    .build();
            colleague.updatePassword(passwordEncoder, SEED_PASSWORD);
            return employeeRepository.save(colleague);
        });
    }

    private void createRoomWithMessages(Long seedEmployeeId, Long colleagueId, int roomIndex) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().build());

        employeeChatRoomRepository.save(EmployeeChatRoom.builder()
                .chatRoom(room)
                .employeeId(seedEmployeeId)
                .build());
        employeeChatRoomRepository.save(EmployeeChatRoom.builder()
                .chatRoom(room)
                .employeeId(colleagueId)
                .build());

        // 메시지는 "지금부터 과거로" 15분 간격으로 배치해서, 방마다 최근 며칠간 대화한
        // 것처럼 보이게 한다 — 발신자는 동료↔시드 계정이 번갈아 보낸다.
        LocalDateTime latest = LocalDateTime.now().minusHours(roomIndex);
        List<ChatMessage> messages = new ArrayList<>(MESSAGES_PER_ROOM);
        for (int i = 0; i < MESSAGES_PER_ROOM; i++) {
            long senderId = (i % 2 == 0) ? colleagueId : seedEmployeeId;
            String text = SAMPLE_MESSAGES[i % SAMPLE_MESSAGES.length];
            LocalDateTime createTime = latest.minusMinutes((long) (MESSAGES_PER_ROOM - i) * 15);

            messages.add(ChatMessage.builder()
                    .chatRoom(room)
                    .employeeId(senderId)
                    .message(text)
                    .createTime(createTime)
                    .build());
        }
        chatMessageRepository.saveAll(messages);
    }
}
