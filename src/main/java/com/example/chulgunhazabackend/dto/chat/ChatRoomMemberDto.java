package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.member.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomMemberDto {

    private Long id;

    private Long employeeNo;

    private String name;

    private Position position;

    private String department;

}
