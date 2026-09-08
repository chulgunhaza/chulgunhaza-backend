package com.example.chulgunhazabackend.dto.attendance;

import com.example.chulgunhazabackend.domain.attendance.AttendanceRecord;
import com.example.chulgunhazabackend.domain.attendance.AttendanceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter
@AllArgsConstructor
@ToString
public class AttendanceCreateRequestDto implements Serializable {

    // #100: 클라이언트가 이 값을 보내도 무시된다 — AttendanceController가 인증된
    // 본인 정보(JWT 클레임)로 덮어쓴 새 DTO를 만들어서 큐에 발행한다. 예전엔 이
    // 값을 그대로 믿어서 다른 사람 사번으로 출근 등록을 요청할 수 있었다(IDOR).
    // @NotNull을 안 붙인 이유: 클라이언트가 안 보내도(혹은 아무 값이나 보내도)
    // 어차피 서버가 덮어쓰므로 검증 대상이 아니다.
    private Long employeeNo;

    // #100: employeeNo와 마찬가지로 컨트롤러가 JWT 클레임의 이름으로 채운다.
    // attendance-server는 Employee 리포지토리가 없어서 등록 시점에 이 값을
    // 그대로 저장(비정규화)하는 것 외엔 이름을 알 방법이 없다.
    private String employeeName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "출근 시간이 누락되었습니다.")
    private LocalDateTime checkInTime;

    @Setter
    private String mqFailMessage;


    public AttendanceRecord toEntity(){
        return AttendanceRecord.builder()
                .employeeNo(employeeNo)
                .employeeName(employeeName)
                .checkInTime(checkInTime)
                .attendanceType(setAttendanceType(checkInTime))
                .build();
    }

    private AttendanceType setAttendanceType(LocalDateTime checkInTime){

        // 기준 시간 (09 : 00)
        LocalTime standardTime = LocalTime.of(9, 0);

        return checkInTime.toLocalTime().isAfter(standardTime)
                ? AttendanceType.LATE : AttendanceType.NORMAL;
        // 결근 처리 추가 예정
    }

}
