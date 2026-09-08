package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.dto.attendance.AttendanceListResponseDto;
import com.example.chulgunhazabackend.service.AttendanceRabbitMQService;
import com.example.chulgunhazabackend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRabbitMQService attendanceRabbitMQService;
    private final AttendanceService attendanceService;

    @PostMapping("/register")
    public ResponseEntity<?> registerAttendance(@Valid @RequestBody AttendanceCreateRequestDto attendanceCreateRequestDto){
        attendanceRabbitMQService.sendAttendanceDto(attendanceCreateRequestDto);
        return ResponseEntity.ok().body("성공");
    }

    // 관리자 페이지 근태 관리 화면 — 등록만 있고 조회가 아예 없던 걸 메운다.
    // employeeNo를 안 주면 전사 출근 기록, 주면 해당 사원만 필터링.
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<PageDto<AttendanceListResponseDto>> getAttendanceList(
            @RequestParam(required = false) Long employeeNo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attendanceService.getAttendanceList(employeeNo, pageable));
    }
}
