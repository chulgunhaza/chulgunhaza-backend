package com.example.chulgunhazabackend.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// INFO : 필드가 1개뿐인 DTO에 @AllArgsConstructor만 있으면 Jackson이 생성자를
// "property-based creator"가 아니라 "delegating creator"(JSON 값 자체를 List<Long>으로
// 바로 변환 시도)로 오인해서 {"memberIds":[...]} 형태의 객체 JSON을 못 읽는다
// ("no delegate- or property-based Creator" 에러로 실측 확인). 필드 2개 이상인 다른
// DTO들은 이 문제가 없어서 지금까지 안 드러났다 — 기본 생성자 + setter 조합으로 우회.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequestDto {

    // INFO : 대화 상대 아이디 목록. 1명이면 1:1, 2명 이상이면 단체 채팅방이 된다.
    // 개설자(sender)는 인증 세션(EmployeeCredentialDto)에서 가져오므로 클라이언트가
    // senderId를 직접 보낼 필요/보안상 신뢰할 이유가 없어서 필드에서 제거했다.
    @NotEmpty(message = "대화 상대를 한 명 이상 선택해야 합니다.")
    private List<Long> memberIds;

}
