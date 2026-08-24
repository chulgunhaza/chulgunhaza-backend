package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCreateRequestDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeModifyRequestDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeResponseDto;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link EmployeeServiceImpl} 회귀 테스트 — 기존에는 이 클래스에 대한 테스트가
 * 하나도 없었다(#54/#48 작업 중 "모든 로직에 TDD 테스트 추가" 요청의 1차 범위).
 * 실제 DB 대신 {@link EmployeeRepository} 를 Mockito로 대체해 순수 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LocalFileServiceImpl fileService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private EmployeeCreateRequestDto createRequestDto;

    @BeforeEach
    void setUp() {
        createRequestDto = new EmployeeCreateRequestDto(
                "김태동", "new@chulgunhaza.com", Gender.MALE,
                LocalDate.of(1999, 1, 1), LocalDate.of(2025, 1, 1),
                "개발팀", Position.EMPLOYEE, List.of(UserRole.USER)
        );
    }

    @Test
    @DisplayName("이메일이 중복되지 않으면 사원을 생성하고 id를 반환한다")
    void create_성공() throws Exception {
        given(employeeRepository.findEmployeeByEmail(createRequestDto.getEmail()))
                .willReturn(Optional.empty());
        given(employeeRepository.save(any(Employee.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Long id = employeeService.create(createRequestDto, 1L);

        assertThat(id).isNull(); // 실제 DB가 아니므로 @GeneratedValue id는 채워지지 않는다 — save 인자로 검증
        verify(employeeRepository).save(argThat(employee ->
                employee.getName().equals("김태동")
                        && employee.getEmail().equals("new@chulgunhaza.com")
                        && employee.getAnnual().getRemainingAnnualCount() == 15.0 // 신규 사원 기본 연차 15일
        ));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 생성하면 EmployeeException(ALREADY_EXIST_EMAIL)이 발생하고 저장하지 않는다")
    void create_중복_이메일이면_예외() {
        given(employeeRepository.findEmployeeByEmail(createRequestDto.getEmail()))
                .willReturn(Optional.of(mock(Employee.class)));

        assertThatThrownBy(() -> employeeService.create(createRequestDto, 1L))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.ALREADY_EXIST_EMAIL));

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하는 사원을 조회하면 EmployeeResponseDto로 변환해 반환한다")
    void getEmployee_성공() {
        Employee employee = employeeOf(10L, "김태동", "kim@chulgunhaza.com", 0L);
        given(employeeRepository.findEmployeeById(10L)).willReturn(Optional.of(employee));

        EmployeeResponseDto responseDto = employeeService.getEmployee(10L);

        assertThat(responseDto.getId()).isEqualTo(10L);
        assertThat(responseDto.getName()).isEqualTo("김태동");
    }

    @Test
    @DisplayName("존재하지 않는 사원을 조회하면 EmployeeException(NOT_EXIST_USER)이 발생한다")
    void getEmployee_존재하지_않으면_예외() {
        given(employeeRepository.findEmployeeById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployee(999L))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.NOT_EXIST_USER));
    }

    @Test
    @DisplayName("요청의 version이 현재 사원의 version과 다르면 EmployeeException(ALREADY_CHANGED)이 발생하고 저장하지 않는다 — 낙관적 락 회귀 방지")
    void modifyById_버전이_다르면_예외() {
        Employee employee = employeeOf(5L, "김태동", "kim@chulgunhaza.com", 0L); // version = 0
        given(employeeRepository.findEmployeeByIdForUpdate(5L)).willReturn(Optional.of(employee));

        EmployeeModifyRequestDto modifyRequestDto = new EmployeeModifyRequestDto(
                "김태동", "kim@chulgunhaza.com", Gender.MALE,
                LocalDate.of(1999, 1, 1), LocalDate.of(2025, 1, 1),
                "개발팀", Position.EMPLOYEE, List.of(UserRole.USER),
                1L // 실제 버전(0)과 다른 값
        );
        // 버전 검증이 이미지 처리보다 먼저 일어나므로 image mock에는 스텁이 필요 없다
        MultipartFile emptyImage = mock(MultipartFile.class);

        assertThatThrownBy(() -> employeeService.modifyById(5L, modifyRequestDto, emptyImage, 1L))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.ALREADY_CHANGED));

        verify(employeeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("존재하는 사원을 삭제하면 delFlag가 true로 바뀐 상태로 저장한다 — 소프트 딜리트")
    void deleteById_소프트_딜리트_처리() {
        Employee employee = employeeOf(7L, "김태동", "kim@chulgunhaza.com", 0L);
        given(employeeRepository.findEmployeeByIdForUpdate(7L)).willReturn(Optional.of(employee));
        given(employeeRepository.save(any(Employee.class))).willAnswer(invocation -> invocation.getArgument(0));

        employeeService.deleteById(7L);

        assertThat(employee.getDelFlag()).isTrue();
        verify(employeeRepository).save(employee);
    }

    private Employee employeeOf(Long id, String name, String email, long version) {
        EmployeeImage employeeImage = new EmployeeImage("default", "path", 1L, "PNG");
        Annual annual = new Annual();
        Employee employee = new Employee(
                name, email, Gender.MALE, LocalDate.of(1999, 1, 1), LocalDate.of(2025, 1, 1),
                null, "개발팀", Position.EMPLOYEE, List.of(UserRole.USER), employeeImage, annual
        );
        setId(employee, id);
        return employee;
    }

    // 테스트 전용: @GeneratedValue id는 리플렉션으로만 세팅 가능하다 (실제 DB 없이 단위 테스트하기 위함)
    private void setId(Employee employee, Long id) {
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
