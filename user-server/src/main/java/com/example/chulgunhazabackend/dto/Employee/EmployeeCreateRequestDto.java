package com.example.chulgunhazabackend.dto.Employee;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
public class EmployeeCreateRequestDto {
    
    @NotBlank(message = "이름을 입력해야 합니다.")
    @Pattern(regexp = "^[가-힣]{1,20}$", message = "한글 1~20자 이내로 작성해주세요")
    private String name;  // 사원 이름

    @Email(message = "이메일 형식으로 작성해 주세요")
    @NotBlank
    private String email;            // 사원 이메일

    @NotNull(message = "성별을 입력해야 합니다.")
    private Gender gender;           // 성별

    @NotNull(message = "생일을 입력해야 합니다.")
    private LocalDate birthDate;     // 생일

    @NotNull(message = "고용일을 입력해야 합니다.")
    private LocalDate hireDate;      // 고용일

    @NotBlank(message = "부서를 입력해야 합니다.")
    private String department;       // 부서
    
    @NotNull(message = "직급을 입력해야 합니다.")
    private Position position;         // 직급 => Enum 변환 예정

    @NotNull(message = "권한을 입력해야 합니다.")
    private List<UserRole> userRoleList; // 권한

    public Employee toEntity(EmployeeImage employeeImage, Annual annual){
        return Employee.builder()
                .name(name)
                .email(email)
                .gender(gender)
                .birthdate(birthDate)
                .hireDate(hireDate)
                .department(department)
                .position(position)
                .userRoleList(userRoleList)
                .employeeImage(employeeImage)
                .annual(annual)
                .build();
    }

}
