package com.example.chulgunhazabackend.dto.chat;

import com.example.chulgunhazabackend.domain.chat.ChatMessage;
import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ChatMessageCreateRequestDto {

    @NotNull(message = "수신자 아이디가 누락되었습니다.")
    private Long receiverId;

    @NotBlank(message = "메시지가 누락되었거나, 공백입니다.")
    @Size(min = 10, max = 300, message = "채팅의 최소 글자 수는 10, 최대 300자 입니다.")
    private String message;

    @NotNull(message = "채팅방 아이디가 누락되었습니다.")
    private Long roomId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @NotNull(message = "채팅 전송 시간이 누락되었습니다.")
    private LocalDateTime createTime;


    public ChatMessage toEntity(ChatRoom chatRoom, Employee employee){
        return ChatMessage.builder()
                .employee(employee)
                .chatRoom(chatRoom)
                .message(message)
                .createTime(createTime)
                .build();
    }
}
