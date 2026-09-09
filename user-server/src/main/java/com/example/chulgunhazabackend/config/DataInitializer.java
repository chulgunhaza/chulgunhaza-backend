package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.domain.member.EmployeeImage;
import com.example.chulgunhazabackend.domain.member.Gender;
import com.example.chulgunhazabackend.domain.member.Position;
import com.example.chulgunhazabackend.domain.member.UserRole;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// 이 프로젝트를 처음 받은 사람이 로그인해볼 계정 하나 없었다 — data.sql이나
// CommandLineRunner 같은 시딩 코드가 전혀 없어서, DB를 새로 붙이면 회원가입 API도
// 없는 채로 로그인할 방법 자체가 없었다(온보딩 정리하다가 실측으로 발견). 서버
// 기동 시 한 번만(멱등하게) 로그인 계정 + 동료 계정을 만들어둔다.
//
// #101: 채팅방/메시지 시딩(ChatRoom/EmployeeChatRoom/ChatMessage)은 여기서 뺐다 —
// chatting-server가 물리 분리되면서 그 엔티티/리포지토리 자체가 user-server에
// 없어졌고, chulgunhaza_chatting은 완전히 다른 스키마(다른 DB 커넥션)라 같은
// @Transactional 안에서 함께 시딩할 수도 없다. 채팅 데모 데이터가 필요하면
// chatting-server 쪽에 별도 CommandLineRunner를 만들어야 하는데, 그러려면 이
// 시드 계정/동료들의 employeeId를 알아야 해서(자동증가 PK라 두 서비스가 그 값을
// 사전에 공유할 방법이 없음) 지금 단계에선 범위 밖으로 남겨둔다 — 로그인 계정이
// 시딩되는 것만으로 온보딩 목적은 달성된다.
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

    // 관리자 페이지 작업(#83) 때문에 추가 — UserRole에 MANAGER/ADMIN이 이미 정의돼
    // 있었는데도 시드 계정이 전부 USER뿐이라 관리자 권한(@PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    // 붙은 사원 생성/수정/삭제 등)을 로컬에서 테스트할 방법이 아예 없었다.
    public static final String SEED_MANAGER_EMAIL = "manager@chulgunhaza.com";
    public static final String SEED_MANAGER_PASSWORD = "test1234!";

    // 시딩 전용 동료 10명 — 실제로 있을 법한 이름이지만 이메일은 seed- 접두사로 못박아서,
    // 이미 수동으로 만들어둔 다른 테스트 계정(예: 이하나)과 절대 안 겹치게 한다.
    // (#101 전에는 이 동료들과 채팅방도 같이 시딩했었다 — 위 클래스 주석 참고.)
    private static final String[] COLLEAGUE_NAMES = {
            "박서준", "김나윤", "이도현", "정유진", "최민석",
            "강하은", "조성민", "윤지수", "임태양", "한소율",
    };

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        employeeRepository.findEmployeeByEmail(SEED_EMAIL)
                .orElseGet(this::createSeedEmployee);
        employeeRepository.findEmployeeByEmail(SEED_MANAGER_EMAIL)
                .orElseGet(this::createSeedManager);

        for (int i = 0; i < COLLEAGUE_NAMES.length; i++) {
            findOrCreateColleague(COLLEAGUE_NAMES[i], i);
        }
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

    private Employee createSeedManager() {
        Employee manager = Employee.builder()
                .name("김관리")
                .email(SEED_MANAGER_EMAIL)
                .gender(Gender.FEMALE)
                .birthdate(LocalDate.of(1988, 1, 1))
                .hireDate(LocalDate.now().minusYears(5))
                .department("태동팀")
                .position(Position.TEAM_LEADER)
                .userRoleList(List.of(UserRole.USER, UserRole.MANAGER))
                .employeeImage(new EmployeeImage())
                .annual(new Annual())
                .build();
        manager.updatePassword(passwordEncoder, SEED_MANAGER_PASSWORD);

        Employee saved = employeeRepository.save(manager);
        log.info("관리자 시드 계정 생성됨 — email: {}, password: {} (로컬 개발 전용)",
                SEED_MANAGER_EMAIL, SEED_MANAGER_PASSWORD);
        return saved;
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
}
