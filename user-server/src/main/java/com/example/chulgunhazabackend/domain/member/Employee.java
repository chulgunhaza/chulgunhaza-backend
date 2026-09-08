package com.example.chulgunhazabackend.domain.member;


import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.dto.Employee.EmployeeModifyRequestDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="employees",
    indexes = {
        @Index(name="idx_position", columnList = "position")
    }
)
@Getter
@ToString
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq_gen")
    @SequenceGenerator(name = "employee_seq_gen", sequenceName = "employee_seq", allocationSize = 1)
    @Column(name = "employee_id")
    private Long id;

    @Version
    private long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private Long employeeNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate birthdate;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Column
    private LocalDate resignationDate;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Position position;

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "userRoles", joinColumns = @JoinColumn(name = "employee_id"))
    private List<UserRole> userRoleList;

    @Embedded
    private EmployeeImage employeeImage;

    @Embedded
    private Annual annual;

    private String password;

    @Builder
    public Employee(String name, String email, Gender gender,
                    LocalDate birthdate, LocalDate hireDate, LocalDate resignationDate,
                    String department, Position position, List<UserRole> userRoleList,
                    EmployeeImage employeeImage, Annual annual) {
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.birthdate = birthdate;
        this.hireDate = hireDate;
        this.resignationDate = resignationDate;
        this.department = department;
        this.position = position;
        this.userRoleList = userRoleList;
        this.employeeImage = employeeImage;
        this.annual = annual;
    }

    @PrePersist
    public void onPrePersist(){
        this.employeeNo = this.id + 10000000L;
    }

    public void addRole(UserRole userRole){
        userRoleList.add(userRole);
    }

    public void clearRole(){
        userRoleList.clear();
    }

    public void updateEmployee(EmployeeModifyRequestDto employeeModifyRequestDto, EmployeeImage employeeImage){
        this.name = employeeModifyRequestDto.getName();
        this.email = employeeModifyRequestDto.getEmail();
        this.gender = employeeModifyRequestDto.getGender();
        this.birthdate = employeeModifyRequestDto.getBirthDate();
        this.hireDate = employeeModifyRequestDto.getHireDate();
        this.department = employeeModifyRequestDto.getDepartment();
        this.userRoleList = employeeModifyRequestDto.getUserRoleList();
        this.employeeImage = employeeImage;
    }

    public void updateAnnual(Annual annual){
        this.annual = annual;
    }


    public void updatePassword(PasswordEncoder passwordEncoder, String password){
        this.password = passwordEncoder.encode(password);
    }

    public void setInitialPassword(PasswordEncoder passwordEncoder){
        this.password =  passwordEncoder.encode("qwer!!"); // password 초기 지정
    }


    public boolean matchVersion(long version){
        return this.version == version;
    }



}
