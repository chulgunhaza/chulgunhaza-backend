package com.example.chulgunhazabackend.domain.member;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRole {


    USER("사원"),
    MANAGER("근태 관리자"),
    ADMIN("관리자");


    private final String role;
    UserRole(String role) {
        this.role = role;
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }

}
