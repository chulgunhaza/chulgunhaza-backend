package com.example.chulgunhazabackend.domain.chat;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="chat_message")
@ToString
@Getter
@Builder
public class ChatMessage extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    private String message;

    // INFO : Employee는 다른 애그리게이트라 객체(@ManyToOne)로 물지 않고 id로만 참조한다.
    // 발신자 이름 등 표시용 데이터가 필요하면 서비스 레이어에서 EmployeeRepository를
    // 따로 조회한다(#74 Phase 0).
    @Column(name = "employee_id")
    private Long employeeId;

    // INFO : 예전엔 여기 메시지당 is_read(전역 boolean) 컬럼이 있었는데, 그룹 채팅방에서
    // 참여자 중 한 명만 읽어도 나머지 전원한테까지 "읽음"으로 보이는 버그가 있었다.
    // 읽음 여부는 "누가" 읽었는지가 핵심이라 메시지 쪽이 아니라 참여자 쪽
    // (EmployeeChatRoom.lastReadMessageId)에서 사람별로 추적하는 게 맞아서 옮겼다.

    @Column(nullable = false, name = "create_time")
    private LocalDateTime createTime;

}
