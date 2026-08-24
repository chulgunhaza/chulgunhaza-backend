package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;
import com.example.chulgunhazabackend.service.AnnualLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/annual")
@RequiredArgsConstructor
public class AnnualController {

    private final AnnualLeaveService annualLeaveService;

    // 연차 사용 (#48 동시성 제어 적용)
    @PostMapping("/use")
    public ResponseEntity<AnnualUsageResponseDto> useAnnualLeave(
            @Valid @RequestBody AnnualUsageRequestDto annualUsageRequestDto,
            @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto) {
        return ResponseEntity.ok(
                annualLeaveService.useAnnualLeave(employeeCredentialDto.getId(), annualUsageRequestDto)
        );
    }
}
