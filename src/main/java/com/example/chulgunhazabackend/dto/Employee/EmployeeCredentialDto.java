package com.example.chulgunhazabackend.dto.Employee;

import com.example.chulgunhazabackend.domain.member.Employee;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter
public class EmployeeCredentialDto extends User {

    private Long id;

    private String email;

    private String password;

    private String name;

    private Long employeeNo;

    private List<String> roles;

    private String department;

    public EmployeeCredentialDto(Long id, String email, String password, String name, Long employeeNo,
                                 List<String> roles, String department) {
        super(email, password, roles.stream().map(
                str -> new SimpleGrantedAuthority("ROLE_" + str)).collect(Collectors.toList())
        );
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.employeeNo = employeeNo;
        this.roles = roles;
        this.department = department;
    }

    // UserDetailServiceImpl(로그인)과 AuthController(refresh 재발급) 양쪽에서
    // Employee 엔티티를 이 DTO로 바꾸는 로직이 겹쳐서 뺐다. userRoleList가 로딩돼
    // 있는 Employee여야 한다(예: findEmployeeByEmailWithUserRoleList로 조회한 것).
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

    // #98: 예전엔 여기 getClaims()가 세션 attribute용 Map(password 포함)을 만들어줬는데,
    // JWT로 바뀌면서 그 로직은 JwtProvider.publicClaims()로 옮겼다(password는 절대
    // JWT payload에 넣으면 안 되므로 의도적으로 뺐다 — payload는 base64만 씌워진
    // 것이라 토큰을 가진 누구나 디코딩해서 볼 수 있음).
}
