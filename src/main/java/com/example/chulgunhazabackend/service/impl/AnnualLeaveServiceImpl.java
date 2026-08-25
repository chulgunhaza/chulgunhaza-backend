package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.annual.AnnualApprovalStatus;
import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;
import com.example.chulgunhazabackend.event.annual.event.AnnualUseEvent;
import com.example.chulgunhazabackend.event.common.Events;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.AnnualRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.AnnualLeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
