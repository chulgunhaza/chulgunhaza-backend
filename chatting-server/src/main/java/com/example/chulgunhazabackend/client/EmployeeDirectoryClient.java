package com.example.chulgunhazabackend.client;

import java.util.List;

// #101: chatting-server는 Employee 리포지토리가 없다 — 다른 사원의 이름/부서
// 표시와 존재 검증 모두 user-server의 내부 API를 통해서만 알 수 있다.
public interface EmployeeDirectoryClient {

    // #100의 AttendanceStatsClient와 달리 실패해도 조용히 기본값으로 대체하지
    // 않는다 — 방 목록/생성/메시지 전송 자체가 사원 정보 없이는 의미가 없어서,
    // 호출부가 명확히 실패를 알고 처리해야 한다(예외를 던짐).
    List<EmployeeSummary> findByIds(List<Long> ids);

    boolean existsById(Long id);
}
