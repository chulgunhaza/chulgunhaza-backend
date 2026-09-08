package com.example.chulgunhazabackend.event.chat.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
@AllArgsConstructor
public class ChatCreateEvent {

    private final Long roomId;

    private final Long senderId;

    private final String senderName;

    private final Long receiverId;

    private final String message;

    private final LocalDateTime creatTime;

    private final long unReadMessageCount;

}
