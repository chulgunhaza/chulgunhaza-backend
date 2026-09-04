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

import java.time.LocalDate;
import java.util.List;

// 이 프로젝트를 처음 받은 사람이 로그인해볼 계정이 하나도 없었다 — data.sql이나
// CommandLineRunner 같은 시딩 코드가 전혀 없어서, DB를 새로 붙이면 회원가입
// API도 따로 없는 채로 로그인할 방법 자체가 없는 상태였다(온보딩 정리하다가
// 실측으로 발견). 서버 기동 시 이 이메일의 계정이 없을 때만 한 번 만든다
// (멱등 — 이미 있으면 아무것도 안 함, 재기동해도 중복 생성 안 됨).
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

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (employeeRepository.findEmployeeByEmail(SEED_EMAIL).isPresent()) {
            return;
        }

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

        employeeRepository.save(employee);
        log.info("시드 계정 생성됨 — email: {}, password: {} (로컬 개발 전용, app.seed-demo-account=false로 끌 수 있음)",
                SEED_EMAIL, SEED_PASSWORD);
    }
}
