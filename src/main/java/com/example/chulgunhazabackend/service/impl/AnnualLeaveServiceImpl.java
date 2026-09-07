package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.annual.AnnualApprovalStatus;
import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.annual.AnnualHistoryResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualRecordListResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;
import com.example.chulgunhazabackend.event.annual.event.AnnualUseEvent;
import com.example.chulgunhazabackend.event.common.Events;
import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.exception.annualException.AnnualExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.AnnualRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.AnnualLeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * #48 연차 사용 동시성 제어.
 *
 * <p>같은 사원이 (거의) 동시에 연차 사용을 두 번 요청하면, 락 없이는 두 트랜잭션이
 * 모두 "차감 전" 잔여 연차를 읽어서 각자 차감한 뒤 저장하는 lost update가 발생한다.
 * {@link EmployeeRepository#findEmployeeByIdForUpdate(Long)} 로 사원 행에
 * {@code SELECT ... FOR UPDATE} 를 걸어, 같은 사원에 대한 두 번째 요청은 첫 번째
 * 트랜잭션이 커밋될 때까지 대기하도록 만들어 race condition을 막는다.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AnnualLeaveServiceImpl implements AnnualLeaveService {

    private final EmployeeRepository employeeRepository;
    private final AnnualRecordRepository annualRecordRepository;

    @Override
    public AnnualUsageResponseDto useAnnualLeave(Long employeeId, AnnualUsageRequestDto requestDto) {

        // 비관적 락으로 사원 행을 잠근다 — 동시 요청 중 하나만 통과하고 나머지는 대기한다.
        Employee employee = employeeRepository.findEmployeeByIdForUpdate(employeeId)
                .orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        double dayCost = requestDto.getAnnualType().getDayCost();

        // 잔여 부족 시 AnnualException(INSUFFICIENT_BALANCE) — 락 안에서 최신 값으로 검증한다.
        Annual usedAnnual = employee.getAnnual().use(dayCost);
        employee.updateAnnual(usedAnnual);
        employeeRepository.saveAndFlush(employee);

        AnnualRecord annualRecord = AnnualRecord.builder()
                .employeeId(employeeId)
                .approvedId(employeeId) // 별도 결재 라인이 아직 없어 본인 사용 신청을 즉시 승인 처리
                .annualDate(requestDto.getAnnualDate())
                .annualType(requestDto.getAnnualType())
                .annualReason(requestDto.getAnnualReason())
                .annualApprovalStatus(AnnualApprovalStatus.APPROVED)
                .build();
        AnnualRecord saved = annualRecordRepository.save(annualRecord);

        // #46: 연차 사용 완료를 MAIN SSE 채널로 알림.
        // MAIN 채널은 (CHAT과 달리) employeeNo로 구독을 키잉하므로 PK가 아니라 employeeNo를 넘긴다.
        Events.raise(new AnnualUseEvent(employee.getEmployeeNo(), usedAnnual.getRemainingAnnualCount()));

        return AnnualUsageResponseDto.of(saved, usedAnnual.getTotalAnnualCount(),
                usedAnnual.getUseCount(), usedAnnual.getRemainingAnnualCount());
    }

    @Override
    public PageDto<AnnualRecordListResponseDto> getAllAnnualRecords(Pageable pageable) {
        Page<AnnualRecord> records = annualRecordRepository.findAllByOrderByCreatedAtDesc(pageable);

        // AnnualRecord는 채팅/근태 기록과 같은 이유로 employeeId만 갖고 있어서(애그리게이트
        // 경계), N+1 없이 이름을 채우려면 이 페이지에 등장하는 사원들을 한 번에 배치 조회한다.
        List<Long> employeeIds = records.getContent().stream()
                .map(AnnualRecord::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameById = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));

        Page<AnnualRecordListResponseDto> contents = records
                .map(record -> AnnualRecordListResponseDto.fromEntity(record, nameById.get(record.getEmployeeId())));
        return new PageDto<>(contents);
    }

    @Override
    public void rejectAnnualRecord(Long annualRecordId) {
        AnnualRecord record = annualRecordRepository.findById(annualRecordId)
                .orElseThrow(() -> new AnnualException(AnnualExceptionType.ANNUAL_RECORD_NOT_FOUND));

        if (record.isRejected()) {
            throw new AnnualException(AnnualExceptionType.ALREADY_REJECTED);
        }

        // useAnnualLeave와 동일하게 비관적 락 위에서 환급해야 동시 반려/재사용 요청과
        // 경쟁해도 잔여 연차가 어긋나지 않는다.
        Employee employee = employeeRepository.findEmployeeByIdForUpdate(record.getEmployeeId())
                .orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        Annual refunded = employee.getAnnual().refund(record.getAnnualType().getDayCost());
        employee.updateAnnual(refunded);
        employeeRepository.saveAndFlush(employee);

        record.reject();
        annualRecordRepository.save(record);

        Events.raise(new AnnualUseEvent(employee.getEmployeeNo(), refunded.getRemainingAnnualCount()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<AnnualHistoryResponseDto> getMyAnnualHistory(Long employeeId, Pageable pageable) {
        Page<AnnualHistoryResponseDto> contents = annualRecordRepository
                .findByEmployeeIdOrderByAnnualDateDesc(employeeId, pageable)
                .map(AnnualHistoryResponseDto::fromEntity);
        return new PageDto<>(contents);
    }
}
