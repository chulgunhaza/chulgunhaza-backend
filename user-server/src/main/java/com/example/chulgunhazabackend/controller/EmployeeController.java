package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCreateRequestDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeModifyRequestDto;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // 사원 생성
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<?> create(
            @Valid @RequestBody EmployeeCreateRequestDto employeeCreateRequestDto,
            @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto) throws IOException {
        return ResponseEntity.status(201).body(employeeService.create(employeeCreateRequestDto,
                employeeCredentialDto.getEmployeeNo()));
    }

    // 사원 목록 조회
    @GetMapping("/list")
    public ResponseEntity<PageDto<?>> getEmployeeList(
            @PageableDefault(size = 10) Pageable pageable){
        return ResponseEntity.status(200).body(employeeService.getEmployeeList(pageable));
    }

    // 사원 조회
    @GetMapping("/list/{employeeId}")
    public ResponseEntity<?> getEmployee(@PathVariable Long employeeId){
        return ResponseEntity.status(200).body(employeeService.getEmployee(employeeId));
    }


    @PutMapping("/modify/{employeeId}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<?> modifyEmployee(@PathVariable Long employeeId,
                                            @Valid @RequestPart EmployeeModifyRequestDto employeeModifyRequestDto,
                                            @RequestParam(value = "employeeImage", required = false) MultipartFile employeeImage,
                                            @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto
                                            ) throws IOException {

        return ResponseEntity.status(200).body(
                employeeService.modifyById(employeeId, employeeModifyRequestDto,
                        employeeImage, employeeCredentialDto.getEmployeeNo())
        );

    }

    // 사원 삭제
    @PatchMapping("/delete/{employeeId}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long employeeId){
        return ResponseEntity.status(200).body(employeeService.deleteById(employeeId));
    }


}
