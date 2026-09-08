package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.AttendanceRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Epic 3 — 근태 기록 등록/전체 조회의 서비스 계층 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Employee employeeOf(Long id, Long employeeNo, String name) {
        Employee employee = Employee.builder()
                .name(name)
                .email(name + "@chulgunhaza.com")
                .gender(Gender.MALE)
                .birthdate(LocalDate.of(1990, 1, 1))
                .hireDate(LocalDate.of(2020, 1, 1))
                .department("개발팀")
                .position(Position.EMPLOYEE)
                .userRoleList(List.of(UserRole.USER))
                .employeeImage(new EmployeeImage())
                .annual(new Annual())
                .build();
        try {
            var idField = Employee.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
            var noField = Employee.class.getDeclaredField("employeeNo");
            noField.setAccessible(true);
            noField.set(employee, employeeNo);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return employee;
    }

    // --- registerAttendance ---

    @Test
    @DisplayName("존재하지 않는 사원 번호로 출근 등록을 시도하면 EmployeeException(NOT_EXIST_USER)이 발생한다")
    void registerAttendance_존재하지_않는_사원이면_예외() {
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(999L, LocalDateTime.now(), null);
        given(employeeRepository.findEmployeeByEmployeeNo(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.registerAttendance(dto))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.NOT_EXIST_USER));

        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상적인 사원 번호로 출근을 등록하면 저장된다")
    void registerAttendance_정상적으로_저장한다() {
        Employee employee = employeeOf(1L, 10000001L, "홍길동");
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(
                10000001L, LocalDateTime.of(2026, 9, 8, 8, 50), null);
        given(employeeRepository.findEmployeeByEmployeeNo(10000001L)).willReturn(Optional.of(employee));

        attendanceService.registerAttendance(dto);

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployee()).isEqualTo(employee);
        assertThat(captor.getValue().getAttendanceType()).isEqualTo(AttendanceType.NORMAL);
    }

    @ParameterizedTest(name = "출근 시각 {0}시 {1}분 -> {2}")
    @CsvSource({
            "8, 59, NORMAL",
            "9, 0, NORMAL",
            "9, 1, LATE",
            "10, 30, LATE",
    })
    @DisplayName("09:00 기준으로 정시/지각이 정확히 분류되어 저장된다")
    void registerAttendance_시각에_따라_출퇴근_타입이_결정된다(int hour, int minute, AttendanceType expected) {
        Employee employee = employeeOf(1L, 10000001L, "홍길동");
        LocalDateTime checkInTime = LocalDate.of(2026, 9, 8).atTime(LocalTime.of(hour, minute));
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(10000001L, checkInTime, null);
        given(employeeRepository.findEmployeeByEmployeeNo(10000001L)).willReturn(Optional.of(employee));

        attendanceService.registerAttendance(dto);

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAttendanceType()).isEqualTo(expected);
    }

    // --- getAttendanceList ---

    @Test
    @DisplayName("employeeNo가 null이면 전체 근태 기록을 조회한다")
    void getAttendanceList_employeeNo_없으면_전체_조회() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee employee = employeeOf(1L, 10000001L, "홍길동");
        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(LocalDateTime.of(2026, 9, 8, 8, 55))
                .attendanceType(AttendanceType.NORMAL)
                .build();
        given(attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));

        PageDto<AttendanceListResponseDto> result = attendanceService.getAttendanceList(null, pageable);

        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getContents().get(0).getEmployeeName()).isEqualTo("홍길동");
        verify(attendanceRecordRepository, never())
                .findByEmployee_EmployeeNoOrderByCheckInTimeDesc(any(), any());
    }

    @Test
    @DisplayName("employeeNo가 주어지면 해당 사원의 근태 기록만 조회한다")
    void getAttendanceList_employeeNo_있으면_해당_사원만_조회() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee employee = employeeOf(1L, 10000001L, "홍길동");
        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(LocalDateTime.of(2026, 9, 8, 8, 55))
                .attendanceType(AttendanceType.NORMAL)
                .build();
        given(attendanceRecordRepository.findByEmployee_EmployeeNoOrderByCheckInTimeDesc(10000001L, pageable))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));

        PageDto<AttendanceListResponseDto> result = attendanceService.getAttendanceList(10000001L, pageable);

        assertThat(result.getContents()).hasSize(1);
        verify(attendanceRecordRepository, never()).findAllByOrderByCheckInTimeDesc(any());
    }

    @Test
    @DisplayName("근태 기록이 없으면 빈 페이지를 반환한다")
    void getAttendanceList_기록이_없으면_빈페이지() {
        Pageable pageable = PageRequest.of(0, 20);
        given(attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        PageDto<AttendanceListResponseDto> result = attendanceService.getAttendanceList(null, pageable);

        assertThat(result.getContents()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지 응답의 각 항목은 사원 번호/이름/출근시간/타입을 정확히 담는다")
    void getAttendanceList_각_필드가_정확히_매핑된다() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee employee = employeeOf(2L, 10000002L, "김철수");
        LocalDateTime checkInTime = LocalDateTime.of(2026, 9, 8, 9, 30);
        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(checkInTime)
                .attendanceType(AttendanceType.LATE)
                .build();
        given(attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));

        PageDto<AttendanceListResponseDto> result = attendanceService.getAttendanceList(null, pageable);

        AttendanceListResponseDto dto = result.getContents().get(0);
        assertThat(dto.getEmployeeNo()).isEqualTo(10000002L);
        assertThat(dto.getEmployeeName()).isEqualTo("김철수");
        assertThat(dto.getCheckInTime()).isEqualTo(checkInTime);
        assertThat(dto.getAttendanceType()).isEqualTo(AttendanceType.LATE);
    }
}
