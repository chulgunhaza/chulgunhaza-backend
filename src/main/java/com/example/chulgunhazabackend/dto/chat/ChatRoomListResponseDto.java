package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ChatRoomListResponseDto {

    private Long roomId;

    private Long employeeId;

    private Long employeeNo;

    private String userName;

    private Position position;

    private String department;

    private String lastMessage;

    private Long unReadMessageCount;

    private LocalDateTime lastMessageTime;

    public ChatRoomListResponseDto fromEntity(EmployeeChatRoom employeeChatRoom, String lastMessage, Long unReadMessageCount, LocalDateTime lastMessageTime) {
        // employeeChatRoom.getId()는 참여자-채팅방 연결 테이블(employee_chatroom)의 PK라
        // 실제 채팅방(chat_room.room_id)과 다르다. 여기서 잘못 채워보내면 프론트가 이 값으로
        // GET /v1/chat/find/{roomId}, POST /v1/chat/send 를 호출할 때 전부 "존재하지 않는
        // 채팅방입니다"(404)가 난다 — React로 실제 채팅방을 만들어 테스트하다가 발견함.
        return new ChatRoomListResponseDto(employeeChatRoom.getChatRoom().getId()
                , employeeChatRoom.getEmployee().getId()
                , employeeChatRoom.getEmployee().getEmployeeNo()
                , employeeChatRoom.getEmployee().getName()
                , employeeChatRoom.getEmployee().getPosition()
                , employeeChatRoom.getEmployee().getDepartment()
                , lastMessage
                , unReadMessageCount
                , lastMessageTime
        );
    }
}
