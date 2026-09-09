package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.employee.EmployeeSummaryResponseDto;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// #101: chatting-server가 다른 사원의 이름/부서 표시(채팅방 목록)와 사원 존재 검증
// (방 생성/메시지 전송 시)에 쓰는 내부 API. attendance-server의
// /internal/attendance/today-count와 같은 "/internal" 예외 경로 규약을 따른다 —
// JWT 없이 호출 가능(아직 네트워크 경계가 없는 개발 단계, 실제 분리 시 클러스터
// 내부망으로만 열어두는 것으로 대체 예정).
@RestController
@RequestMapping("/internal/employees")
@RequiredArgsConstructor
public class InternalEmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping
    public List<EmployeeSummaryResponseDto> findByIds(@RequestParam List<Long> ids) {
        List<Employee> employees = employeeRepository.findAllById(ids);
        return employees.stream().map(EmployeeSummaryResponseDto::fromEntity).toList();
    }
}
