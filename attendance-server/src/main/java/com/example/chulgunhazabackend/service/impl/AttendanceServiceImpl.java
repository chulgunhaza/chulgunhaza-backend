package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import com.example.chulgunhazabackend.repository.AttendanceRecordRepository;
import com.example.chulgunhazabackend.service.AttendanceRabbitMQService;
import com.example.chulgunhazabackend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceRabbitMQService attendanceRabbitMQService;

    // #100: EmployeeRepository 조회가 사라졌다 — employeeNo/employeeName은
    // 이미 컨트롤러에서 JWT 클레임으로 채워져 큐를 타고 들어온 값이라(비정규화)
    // attendance-server는 사원 존재 확인을 위해 user-server를 동기 호출할
    // 필요가 없다. 저장 후엔 Events.raise(인프로세스 이벤트) 대신 RabbitMQ로
    // user-server에 SSE 알림을 요청한다(교차 서비스).
    public void registerAttendance(AttendanceCreateRequestDto attendanceCreateRequestDto) {
        attendanceRecordRepository.save(attendanceCreateRequestDto.toEntity());
        attendanceRabbitMQService.notifyRegistered(attendanceCreateRequestDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<AttendanceListResponseDto> getAttendanceList(Long employeeNo, Pageable pageable) {
        Page<AttendanceRecord> records = (employeeNo != null)
                ? attendanceRecordRepository.findByEmployeeNoOrderByCheckInTimeDesc(employeeNo, pageable)
                : attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable);

        return new PageDto<>(records.map(AttendanceListResponseDto::fromEntity));
    }
}
