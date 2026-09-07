package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import org.springframework.data.domain.Pageable;

public interface AttendanceService {

    void registerAttendance(AttendanceCreateRequestDto attendanceCreateRequestDto);

    // employeeNo가 null이면 전사 출근 기록, 있으면 해당 사원만 필터링.
    PageDto<AttendanceListResponseDto> getAttendanceList(Long employeeNo, Pageable pageable);
}
