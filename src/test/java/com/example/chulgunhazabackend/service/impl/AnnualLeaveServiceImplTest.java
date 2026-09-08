package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.annual.AnnualApprovalStatus;
import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import com.example.chulgunhazabackend.domain.annual.AnnualType;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.annual.AnnualHistoryResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualRecordListResponseDto;
import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.exception.annualException.AnnualExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.AnnualRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 관리자 백로그 Epic 4 — 연차 결재(반려) + #79 본인 이력 조회의 서비스 계층 단위 테스트.
 * {@link AnnualLeaveServiceImplConcurrencyTest} 가 useAnnualLeave의 동시성만 다루므로,
 * 여기서는 rejectAnnualRecord / getAllAnnualRecords / getMyAnnualHistory를 Mockito로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AnnualLeaveServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AnnualRecordRepository annualRecordRepository;

    @InjectMocks
    private AnnualLeaveServiceImpl annualLeaveService;

    private AnnualRecord recordOf(Long id, Long employeeId, AnnualType type, AnnualApprovalStatus status) {
        return AnnualRecord.builder()
                .employeeId(employeeId)
                .approvedId(employeeId)
                .annualDate(LocalDate.of(2026, 9, 1))
                .annualType(type)
                .annualReason("개인 사유")
                .annualApprovalStatus(status)
                .build();
    }

    private Employee employeeWithAnnual(Long id, Annual annual) {
        Employee employee = Employee.builder()
                .name("사원" + id)
                .email("emp" + id + "@chulgunhaza.com")
                .gender(Gender.MALE)
                .birthdate(LocalDate.of(1990, 1, 1))
                .hireDate(LocalDate.of(2020, 1, 1))
                .department("개발팀")
                .position(Position.EMPLOYEE)
                .userRoleList(List.of(UserRole.USER))
                .employeeImage(new EmployeeImage())
                .annual(annual)
                .build();
        setId(employee, id);
        return employee;
    }

    private void setId(Employee employee, Long id) {
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setRecordId(AnnualRecord record, Long id) {
        try {
            var field = AnnualRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // --- rejectAnnualRecord ---

    @Test
    @DisplayName("존재하지 않는 연차 기록을 반려하려 하면 AnnualException(ANNUAL_RECORD_NOT_FOUND)이 발생한다")
    void reject_존재하지_않으면_예외() {
        given(annualRecordRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> annualLeaveService.rejectAnnualRecord(999L))
                .isInstanceOf(AnnualException.class)
                .satisfies(ex -> assertThat(((AnnualException) ex).getAnnualExceptionType())
                        .isEqualTo(AnnualExceptionType.ANNUAL_RECORD_NOT_FOUND));

        verify(employeeRepository, never()).findEmployeeByIdForUpdate(any());
    }

    @Test
    @DisplayName("이미 반려된 연차 기록을 다시 반려하려 하면 AnnualException(ALREADY_REJECTED)이 발생하고 환급이 중복 발생하지 않는다")
    void reject_이미_반려된_기록이면_예외() {
        AnnualRecord record = recordOf(1L, 10L, AnnualType.ANNUAL, AnnualApprovalStatus.REJECTED);
        setRecordId(record, 1L);
        given(annualRecordRepository.findById(1L)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> annualLeaveService.rejectAnnualRecord(1L))
                .isInstanceOf(AnnualException.class)
                .satisfies(ex -> assertThat(((AnnualException) ex).getAnnualExceptionType())
                        .isEqualTo(AnnualExceptionType.ALREADY_REJECTED));

        verify(employeeRepository, never()).findEmployeeByIdForUpdate(any());
        verify(annualRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("사원이 존재하지 않으면 EmployeeException(NOT_EXIST_USER)이 발생한다")
    void reject_사원이_없으면_예외() {
        AnnualRecord record = recordOf(1L, 999L, AnnualType.ANNUAL, AnnualApprovalStatus.APPROVED);
        setRecordId(record, 1L);
        given(annualRecordRepository.findById(1L)).willReturn(Optional.of(record));
        given(employeeRepository.findEmployeeByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> annualLeaveService.rejectAnnualRecord(1L))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.NOT_EXIST_USER));
    }

    @ParameterizedTest(name = "{0} 타입 반려 시 {1}일 환급된다")
    @CsvSource({
            "ANNUAL, 1.0",
            "ANNUAL_AM, 0.5",
            "ANNUAL_PM, 0.5",
    })
    @DisplayName("연차 종류에 따라 정확한 일수만큼 환급되고 상태가 REJECTED로 바뀐다")
    void reject_성공하면_종류별_일수만큼_환급된다(AnnualType type, double expectedRefund) {
        AnnualRecord record = recordOf(1L, 10L, type, AnnualApprovalStatus.APPROVED);
        setRecordId(record, 1L);
        Annual before = Annual.builder()
                .totalAnnualCount(15.0).useCount(5.0).remainingAnnualCount(10.0).sickAnnualCount(0.0).build();
        Employee employee = employeeWithAnnual(10L, before);

        given(annualRecordRepository.findById(1L)).willReturn(Optional.of(record));
        given(employeeRepository.findEmployeeByIdForUpdate(10L)).willReturn(Optional.of(employee));

        annualLeaveService.rejectAnnualRecord(1L);

        assertThat(employee.getAnnual().getRemainingAnnualCount()).isEqualTo(10.0 + expectedRefund);
        assertThat(employee.getAnnual().getUseCount()).isEqualTo(5.0 - expectedRefund);
        assertThat(record.getAnnualApprovalStatus()).isEqualTo(AnnualApprovalStatus.REJECTED);
        verify(annualRecordRepository).save(record);
        verify(employeeRepository).saveAndFlush(employee);
    }

    // --- getAllAnnualRecords (Epic 4 결재 화면) ---

    @Test
    @DisplayName("연차 기록이 없으면 빈 페이지를 반환하고 사원 조회는 빈 목록으로 호출된다")
    void getAllAnnualRecords_기록이_없으면_빈페이지() {
        Pageable pageable = PageRequest.of(0, 20);
        given(annualRecordRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));
        given(employeeRepository.findAllById(List.of())).willReturn(List.of());

        PageDto<AnnualRecordListResponseDto> result = annualLeaveService.getAllAnnualRecords(pageable);

        assertThat(result.getContents()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 사원의 기록이 섞여 있어도 각자 올바른 이름으로 매핑된다")
    void getAllAnnualRecords_여러_사원_이름이_정확히_매핑된다() {
        Pageable pageable = PageRequest.of(0, 20);
        AnnualRecord r1 = recordOf(1L, 10L, AnnualType.ANNUAL, AnnualApprovalStatus.APPROVED);
        AnnualRecord r2 = recordOf(2L, 20L, AnnualType.ANNUAL_AM, AnnualApprovalStatus.APPROVED);
        given(annualRecordRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(r1, r2), pageable, 2));

        Employee emp10 = employeeWithAnnual(10L, new Annual());
        Employee emp20 = employeeWithAnnual(20L, new Annual());
        given(employeeRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(emp10, emp20));

        PageDto<AnnualRecordListResponseDto> result = annualLeaveService.getAllAnnualRecords(pageable);

        assertThat(result.getContents()).hasSize(2);
        assertThat(result.getContents().get(0).getEmployeeName()).isEqualTo("사원10");
        assertThat(result.getContents().get(1).getEmployeeName()).isEqualTo("사원20");
    }

    @Test
    @DisplayName("같은 사원의 기록이 여러 건이어도 사원 조회(findAllById)는 중복 없이 한 번만 호출된다 (N+1 방지)")
    void getAllAnnualRecords_같은_사원_중복_조회_안한다() {
        Pageable pageable = PageRequest.of(0, 20);
        AnnualRecord r1 = recordOf(1L, 10L, AnnualType.ANNUAL, AnnualApprovalStatus.APPROVED);
        AnnualRecord r2 = recordOf(2L, 10L, AnnualType.ANNUAL_AM, AnnualApprovalStatus.APPROVED);
        AnnualRecord r3 = recordOf(3L, 10L, AnnualType.ANNUAL_PM, AnnualApprovalStatus.APPROVED);
        given(annualRecordRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(r1, r2, r3), pageable, 3));
        Employee emp10 = employeeWithAnnual(10L, new Annual());
        given(employeeRepository.findAllById(List.of(10L))).willReturn(List.of(emp10));

        PageDto<AnnualRecordListResponseDto> result = annualLeaveService.getAllAnnualRecords(pageable);

        assertThat(result.getContents()).hasSize(3);
        verify(employeeRepository).findAllById(List.of(10L));
    }

    // --- getMyAnnualHistory (#79 self-service) ---

    @Test
    @DisplayName("본인 연차 이력을 페이지로 정확히 매핑해서 반환한다")
    void getMyAnnualHistory_정상적으로_매핑된다() {
        Pageable pageable = PageRequest.of(0, 20);
        AnnualRecord r1 = recordOf(1L, 10L, AnnualType.ANNUAL, AnnualApprovalStatus.APPROVED);
        setRecordId(r1, 1L);
        given(annualRecordRepository.findByEmployeeIdOrderByAnnualDateDesc(10L, pageable))
                .willReturn(new PageImpl<>(List.of(r1), pageable, 1));

        PageDto<AnnualHistoryResponseDto> result = annualLeaveService.getMyAnnualHistory(10L, pageable);

        assertThat(result.getContents()).hasSize(1);
        assertThat(result.getContents().get(0).getAnnualRecordId()).isEqualTo(1L);
        assertThat(result.getContents().get(0).getAnnualType()).isEqualTo(AnnualType.ANNUAL);
        assertThat(result.getContents().get(0).getAnnualApprovalStatus()).isEqualTo(AnnualApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("이력이 없는 사원은 빈 페이지를 반환한다")
    void getMyAnnualHistory_이력이_없으면_빈페이지() {
        Pageable pageable = PageRequest.of(0, 20);
        given(annualRecordRepository.findByEmployeeIdOrderByAnnualDateDesc(30L, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        PageDto<AnnualHistoryResponseDto> result = annualLeaveService.getMyAnnualHistory(30L, pageable);

        assertThat(result.getContents()).isEmpty();
    }

    @Test
    @DisplayName("getMyAnnualHistory는 요청한 employeeId로 정확히 한 번만 조회한다 (다른 사원 id로는 호출되지 않음)")
    void getMyAnnualHistory_요청한_employeeId로만_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        given(annualRecordRepository.findByEmployeeIdOrderByAnnualDateDesc(10L, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        annualLeaveService.getMyAnnualHistory(10L, pageable);

        org.mockito.ArgumentCaptor<Long> employeeIdCaptor = org.mockito.ArgumentCaptor.forClass(Long.class);
        verify(annualRecordRepository, org.mockito.Mockito.times(1))
                .findByEmployeeIdOrderByAnnualDateDesc(employeeIdCaptor.capture(), any());
        assertThat(employeeIdCaptor.getValue()).isEqualTo(10L);
    }
}
