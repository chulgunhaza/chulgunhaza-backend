package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import com.example.chulgunhazabackend.event.attendance.event.AttendanceCreateEvent;
import com.example.chulgunhazabackend.event.common.Events;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.AttendanceRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
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

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public void registerAttendance(AttendanceCreateRequestDto attendanceCreateRequestDto) {
        //TODO : 이벤트 발행 후 SSE로 이벤트 뿌리기

        Events.raise(new AttendanceCreateEvent(attendanceCreateRequestDto.getEmployeeNo()));

        Employee employee = employeeRepository
                .findEmployeeByEmployeeNo(attendanceCreateRequestDto.getEmployeeNo())
                .orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
        attendanceRecordRepository.save(attendanceCreateRequestDto.toEntity(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<AttendanceListResponseDto> getAttendanceList(Long employeeNo, Pageable pageable) {
        Page<AttendanceRecord> records = (employeeNo != null)
                ? attendanceRecordRepository.findByEmployee_EmployeeNoOrderByCheckInTimeDesc(employeeNo, pageable)
                : attendanceRecordRepository.findAllByOrderByCheckInTimeDesc(pageable);

        return new PageDto<>(records.map(AttendanceListResponseDto::fromEntity));
    }
}
