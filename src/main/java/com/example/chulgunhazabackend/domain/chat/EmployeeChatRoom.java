package com.example.chulgunhazabackend.domain.chat;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="employee_chatroom")
@ToString
@Getter
@Builder
public class EmployeeChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_chatroom_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // INFO : 이 사람이 이 방에서 마지막으로 읽은 메시지의 id. null이면 아직 하나도 안 읽음.
    // 예전엔 ChatMessage 쪽에 메시지당 is_read(전역 boolean) 하나만 있어서, 그룹 채팅방에서
    // 한 명만 읽어도 전원 읽음 처리되는 버그가 있었다 — "사람별로"(employee_chatroom 단위)
    // 마지막으로 읽은 지점을 따로 들고 있는 방식으로 바꿔서 1:1/단체 모두 정확히 계산한다.
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

}
