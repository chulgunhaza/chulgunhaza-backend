package com.example.chulgunhazabackend.dto.Employee;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponseDto {

    private Long id;

    private String name; // 이름

    private String email; // 이메일

    private EmployeeImage employeeImage; // 사원 이미지

    private Gender gender; // 성별

    private String department; // 부서

    private Position position; // 직책

    private List<UserRole> userRoleList;

    private Annual annual;

    private long version;

    public static EmployeeResponseDto fromEntity(Employee employee){
        return new EmployeeResponseDto(
                employee.getId(), employee.getName(), employee.getEmail(),
                employee.getEmployeeImage(), employee.getGender(), employee.getDepartment(),
                employee.getPosition(), employee.getUserRoleList(), employee.getAnnual(), employee.getVersion()
        );
    }
}
