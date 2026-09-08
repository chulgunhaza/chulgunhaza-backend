package com.example.chulgunhazabackend.dto.attendance;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceListResponseDto {

    private Long employeeNo;

    private String employeeName;

    private LocalDateTime checkInTime;

    private AttendanceType attendanceType;

    // #100: employee_id로 JOIN하던 걸 비정규화된 employeeNo/employeeName 직접
    // 참조로 교체 — 스키마 분리 후엔 JOIN이 아예 불가능해졌다.
    public static AttendanceListResponseDto fromEntity(AttendanceRecord record) {
        return new AttendanceListResponseDto(
                record.getEmployeeNo(),
                record.getEmployeeName(),
                record.getCheckInTime(),
                record.getAttendanceType()
        );
    }
}
