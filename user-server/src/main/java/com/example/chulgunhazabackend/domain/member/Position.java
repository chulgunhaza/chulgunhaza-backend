package com.example.chulgunhazabackend.domain.member;

import lombok.Getter;

@Getter
public enum Position {

    EMPLOYEE("사원"), ASSISTANT_MANAGER("대리"), MANAGER("과장"),
    DEPUTY_MANAGER("차장"), TEAM_LEADER("팀장")
    ;

    private final String position;

    Position(String position) {
        this.position = position;
    }
}
