package com.example.chulgunhazabackend.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// #101: user-server의 GET /internal/employees?ids=...가 내려주는 응답 형태를 그대로
// 받는 DTO. user-server의 EmployeeSummaryResponseDto와 필드가 동일하지만, 이건
// "서비스 간 wire 계약"이 아니라 "클라이언트가 상대 응답을 파싱하는 모델"이라
// common에 안 두고 chatting-server에 둔다(호출하는 쪽 사정에 맞춰 자유롭게
// 바뀔 수 있어야 함 — 예: 나중에 필드를 덜 받아오게 바꿔도 user-server 응답
// 자체는 그대로일 수 있음).
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeSummary {

    private Long id;
    private Long employeeNo;
    private String name;
    private String department;
    private String position;
}
