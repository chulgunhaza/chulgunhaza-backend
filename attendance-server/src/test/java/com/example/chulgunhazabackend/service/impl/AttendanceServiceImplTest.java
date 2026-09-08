package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import com.example.chulgunhazabackend.repository.AttendanceRecordRepository;
import com.example.chulgunhazabackend.service.AttendanceRabbitMQService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * #100 — attendance-server 물리 분리 후 근태 기록 등록/조회의 서비스 계층 단위 테스트.
 * employeeNo/employeeName이 이제 컨트롤러 단계(JWT 클레임)에서 이미 확정돼
 * 들어오는 값이라(비정규화), EmployeeRepository 의존이 완전히 없어졌다.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private AttendanceRabbitMQService attendanceRabbitMQService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    // --- registerAttendance ---

    @Test
    @DisplayName("출근을 등록하면 employeeNo/employeeName이 그대로 저장된다")
    void registerAttendance_정상적으로_저장한다() {
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(
                10000001L, "홍길동", LocalDateTime.of(2026, 9, 8, 8, 50), null);

        attendanceService.registerAttendance(dto);

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployeeNo()).isEqualTo(10000001L);
        assertThat(captor.getValue().getEmployeeName()).isEqualTo("홍길동");
        assertThat(captor.getValue().getAttendanceType()).isEqualTo(AttendanceType.NORMAL);
    }

    @Test
    @DisplayName("출근 등록 성공 후 user-server에 SSE 알림을 요청한다(교차 서비스)")
    void registerAttendance_성공하면_알림을_요청한다() {
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(
                10000001L, "홍길동", LocalDateTime.of(2026, 9, 8, 8, 50), null);

        attendanceService.registerAttendance(dto);

        verify(attendanceRabbitMQService).notifyRegistered(dto);
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
        LocalDateTime checkInTime = LocalDate.of(2026, 9, 8).atTime(LocalTime.of(hour, minute));
        AttendanceCreateRequestDto dto = new AttendanceCreateRequestDto(10000001L, "홍길동", checkInTime, null);

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
        AttendanceRecord record = AttendanceRecord.builder()
                .employeeNo(10000001L)
                .employeeName("홍길동")
                .checkInTime(LocalDateTime.of(2026, 9, 8, 8, 55))
                .attendanceType(AttendanceType.NORMAL)
                .build();
        given(attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));

        PageDto<AttendanceListResponseDto> result = attendanceService.getAttendanceList(null, pageable);

        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getContents().get(0).getEmployeeName()).isEqualTo("홍길동");
        verify(attendanceRecordRepository, never())
                .findByEmployeeNoOrderByCheckInTimeDesc(any(), any());
    }

    @Test
    @DisplayName("employeeNo가 주어지면 해당 사원의 근태 기록만 조회한다")
    void getAttendanceList_employeeNo_있으면_해당_사원만_조회() {
        Pageable pageable = PageRequest.of(0, 20);
        AttendanceRecord record = AttendanceRecord.builder()
                .employeeNo(10000001L)
                .employeeName("홍길동")
                .checkInTime(LocalDateTime.of(2026, 9, 8, 8, 55))
                .attendanceType(AttendanceType.NORMAL)
                .build();
        given(attendanceRecordRepository.findByEmployeeNoOrderByCheckInTimeDesc(10000001L, pageable))
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
        LocalDateTime checkInTime = LocalDateTime.of(2026, 9, 8, 9, 30);
        AttendanceRecord record = AttendanceRecord.builder()
                .employeeNo(10000002L)
                .employeeName("김철수")
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
