package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ChatRoomListResponseDto {

    private Long roomId;

    // INFO : 참여자(나 제외)가 2명 이상이면 단체 채팅방.
    private boolean group;

    // INFO : 화면에 바로 표시할 이름 — 1:1이면 상대 이름, 그룹이면 "A, B 외 N명" 형태로 조합.
    private String roomName;

    // INFO : 나를 제외한 방 참여자 전원 (그룹 채팅 대상 선택 / 나가기 UI 등에 필요).
    private List<ChatRoomMemberDto> members;

    private String lastMessage;

    private Long unReadMessageCount;

    private LocalDateTime lastMessageTime;

    public static ChatRoomListResponseDto fromEntity(Long roomId, List<EmployeeChatRoom> otherMembers, String lastMessage, Long unReadMessageCount, LocalDateTime lastMessageTime) {
        List<ChatRoomMemberDto> memberDtos = otherMembers.stream()
                .map(ecr -> new ChatRoomMemberDto(
                        ecr.getEmployee().getId(),
                        ecr.getEmployee().getEmployeeNo(),
                        ecr.getEmployee().getName(),
                        ecr.getEmployee().getPosition(),
                        ecr.getEmployee().getDepartment()))
                .toList();

        return new ChatRoomListResponseDto(
                roomId,
                memberDtos.size() > 1,
                buildRoomName(memberDtos),
                memberDtos,
                lastMessage,
                unReadMessageCount,
                lastMessageTime
        );
    }

    private static String buildRoomName(List<ChatRoomMemberDto> members) {
        if (members.isEmpty()) {
            return "(대화 상대 없음)";
        }
        if (members.size() <= 3) {
            return members.stream().map(ChatRoomMemberDto::getName).collect(Collectors.joining(", "));
        }
        String head = members.subList(0, 2).stream().map(ChatRoomMemberDto::getName).collect(Collectors.joining(", "));
        return head + " 외 " + (members.size() - 2) + "명";
    }
}
