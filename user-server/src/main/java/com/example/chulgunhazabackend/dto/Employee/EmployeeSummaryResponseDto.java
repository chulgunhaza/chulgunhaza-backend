package com.example.chulgunhazabackend.dto.employee;

import com.example.chulgunhazabackend.domain.member.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

// #101: chatting-server가 GET /internal/employees?ids=...로 다른 사원의 이름/부서를
// 조회할 때 쓰는 응답 형태. position은 enum(Position, user-server 전용 타입) 대신
// 그 이름(name())을 문자열로 내려서 chatting-server가 이 타입을 몰라도 되게 한다 —
// Jackson이 enum을 직렬화할 때도 기본적으로 name()을 쓰므로 와이어 포맷은 동일하다.
@Getter
@AllArgsConstructor
public class EmployeeSummaryResponseDto {

    private Long id;
    private Long employeeNo;
    private String name;
    private String department;
    private String position;

    public static EmployeeSummaryResponseDto fromEntity(Employee employee) {
        return new EmployeeSummaryResponseDto(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition().name()
        );
    }
}
