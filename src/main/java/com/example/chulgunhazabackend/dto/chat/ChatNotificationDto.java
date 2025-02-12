package com.example.chulgunhazabackend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatNotificationDto implements Serializable {

    public Long roomId;

    public Long senderEmployeeNo;

    public String senderName;

    public Long receiverEmployeeNo;

    public String lastMessage;

    public Long unReadMessageCount;

}
