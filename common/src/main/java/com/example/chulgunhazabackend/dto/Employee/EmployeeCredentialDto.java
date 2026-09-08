package com.example.chulgunhazabackend.dto.Employee;

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

    // #99: Employee → EmployeeCredentialDto 변환(from(Employee))은 여기 있었는데
    // 멀티모듈 전환 때 옮겼다 — Employee는 JPA 엔티티라 user-server에만 있고, 이
    // DTO는 여러 서비스가 같이 쓰는 common 모듈이라 common이 user-server의
    // 도메인 타입을 참조하면 의존 방향이 거꾸로 된다(common은 누구에게도 의존하면
    // 안 됨). 변환 로직은 user-server의
    // security.EmployeeCredentialDtoFactory로 옮겼다.

    // #98: 예전엔 여기 getClaims()가 세션 attribute용 Map(password 포함)을 만들어줬는데,
    // JWT로 바뀌면서 그 로직은 JwtProvider.publicClaims()로 옮겼다(password는 절대
    // JWT payload에 넣으면 안 되므로 의도적으로 뺐다 — payload는 base64만 씌워진
    // 것이라 토큰을 가진 누구나 디코딩해서 볼 수 있음).
}
