package com.example.chulgunhazabackend.security;

import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;

import java.util.stream.Collectors;

// #99: Employee(JPA 엔티티, user-server 전용) → EmployeeCredentialDto(common 모듈,
// 여러 서비스가 공유) 변환. 원래 EmployeeCredentialDto.from(Employee)로 DTO 안에
// 있었는데, 멀티모듈 전환 때 common이 user-server의 도메인 타입을 참조하게 돼서
// (의존 방향 역전) 여기로 옮겼다. UserDetailServiceImpl(로그인)과
// AuthController(refresh 재발급) 양쪽에서 재사용한다.
public class EmployeeCredentialDtoFactory {

    private EmployeeCredentialDtoFactory() {
    }

    // userRoleList가 로딩돼 있는 Employee여야 한다
    // (예: findEmployeeByEmailWithUserRoleList/findEmployeeByIdWithUserRoleList로 조회한 것).
    public static EmployeeCredentialDto from(Employee employee) {
        return new EmployeeCredentialDto(
                employee.getId(),
                employee.getEmail(),
                employee.getPassword(),
                employee.getName(),
                employee.getEmployeeNo(),
                employee.getUserRoleList().stream().map(Enum::name).collect(Collectors.toList()),
                employee.getDepartment()
        );
    }
}
