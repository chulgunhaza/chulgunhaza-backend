package com.example.chulgunhazabackend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomMemberDto {

    private Long id;

    private Long employeeNo;

    private String name;

    // #101: Position(user-server 전용 enum) → String. chatting-server는
    // user-server의 도메인 타입을 참조할 수 없고, EmployeeDirectoryClient가
    // 받아오는 EmployeeSummary.position도 이미 문자열이라 그대로 옮긴다.
    private String position;

    private String department;

}
