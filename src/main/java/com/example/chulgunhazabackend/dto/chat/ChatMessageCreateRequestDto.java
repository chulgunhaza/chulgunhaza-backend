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

    // 최소 10자 제약은 "네", "넵!", "확인했습니다" 같은 실제 대화에서 흔한 짧은 답장을
    // 전부 막는 부자연스러운 규칙이었다 (React 프론트로 실제 채팅을 테스트하다가 발견).
    // 공백만 있는 메시지만 막고, 상한(300자)은 그대로 유지한다.
    @NotBlank(message = "메시지가 누락되었거나, 공백입니다.")
    @Size(max = 300, message = "채팅은 최대 300자까지 입력할 수 있습니다.")
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
