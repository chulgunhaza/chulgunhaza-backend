package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.annual.AnnualRecordListResponseDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageResponseDto;
import com.example.chulgunhazabackend.service.AnnualLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // 관리자 백로그 Epic 4 — 전사 연차 사용 내역 조회
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<PageDto<AnnualRecordListResponseDto>> getAllAnnualRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(annualLeaveService.getAllAnnualRecords(pageable));
    }

    // 관리자 백로그 Epic 4 — 연차 반려(잔여 연차 환급)
    @PatchMapping("/records/{annualRecordId}/reject")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<Void> rejectAnnualRecord(@PathVariable Long annualRecordId) {
        annualLeaveService.rejectAnnualRecord(annualRecordId);
        return ResponseEntity.ok().build();
    }
}
