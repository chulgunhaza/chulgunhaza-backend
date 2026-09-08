package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기존 버전은 {@code @Rollback(value = false)} 로 커밋하면서 이메일을 하드코딩해뒀다.
 * 그 결과 한 번 실행하면 DB에 그 이메일이 영구히 남고, 두 번째 실행부터는
 * unique 제약(email) 위반으로 항상 실패하는 테스트였다 — 그리고 assertion이
 * 하나도 없어서 "저장이 실제로 잘 됐는지"조차 검증하지 않았다.
 *
 * <p>#48 작업 도중 전체 테스트를 반복 실행하다가 이 결함을 발견해서 같이 고쳤다.
 * {@code @Transactional} 기본값(테스트 종료 시 롤백)으로 DB를 더럽히지 않게 하고,
 * 실행마다 겹치지 않는 이메일을 쓰고, 실제 값 검증(assertion)을 추가했다.</p>
 */
@SpringBootTest
@Transactional
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("사원을 저장하면 id가 채번되고 저장한 값 그대로 조회된다")
    void saveEmployee() {
        Employee employee = newEmployee("Kim", uniqueEmail("kim"));

        Employee saved = employeeRepository.save(employee);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmployeeNo()).isEqualTo(saved.getId() + 10000000L);

        Optional<Employee> found = employeeRepository.findEmployeeById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Kim");
        assertThat(found.get().getAnnual().getRemainingAnnualCount()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("사원 여러 명을 저장하면 전부 조회 가능하고 초기 비밀번호가 인코딩되어 저장된다")
    void saveEmployees() {
        List<Employee> saved = List.of(
                withInitialPassword(newEmployee("Kim", uniqueEmail("kim-multi-1"))),
                withInitialPassword(newEmployee("Lim", uniqueEmail("lim-multi-1")))
        );
        saved.forEach(employeeRepository::save);

        for (Employee employee : saved) {
            Optional<Employee> found = employeeRepository.findEmployeeById(employee.getId());
            assertThat(found).isPresent();
            assertThat(passwordEncoder.matches("qwer!!", found.get().getPassword())).isTrue();
        }
    }

    private Employee withInitialPassword(Employee employee) {
        employee.setInitialPassword(passwordEncoder);
        return employee;
    }

    private Employee newEmployee(String name, String email) {
        EmployeeImage employeeImage = new EmployeeImage("imageName", "imagePath", 1L, "JPG");
        Annual annual = new Annual();
        List<UserRole> userRoles = List.of(UserRole.USER, UserRole.ADMIN);
        return new Employee(
                name, email, Gender.MALE, LocalDate.of(2025, 1, 16),
                LocalDate.of(2025, 1, 16), null, "인사과", Position.EMPLOYEE,
                userRoles, employeeImage, annual
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@test.com";
    }
}
