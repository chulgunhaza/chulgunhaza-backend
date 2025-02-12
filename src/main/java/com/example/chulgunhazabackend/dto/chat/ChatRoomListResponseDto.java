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
        return new ChatRoomListResponseDto(employeeChatRoom.getId()
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
