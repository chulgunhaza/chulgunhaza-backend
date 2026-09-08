package com.example.chulgunhazabackend.domain.chat;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
// INFO : 같은 사람이 같은 방에 참여 기록을 2개 이상 가질 수 없게 DB 레벨로 막는다.
// saveChatRoom에서 memberIds에 개설자 본인 id가 섞여 들어오는 걸 걸러내는 애플리케이션
// 레벨 방어를 이미 추가했지만, 이 제약이 없으면 "코드"만 믿게 되고 다른 경로(다음에 또
// 비슷한 실수가 생기는 경우)로 같은 중복이 또 들어갈 수 있다. 실측으로 확인된 실제 장애:
// 중복 행이 있으면 findByEmployeeIdAndChatRoomId(Optional 단건 조회)가
// IncorrectResultSizeDataAccessException을 던지는데, 그 예외가 RabbitMQ 리스너
// (ChatMessageListener.saveChatMessage) 안에서 발생하면 메시지가 계속 requeue돼서
// 초당 수백 번씩 무한 재시도하는 루프에 빠진다(로그 파일이 수천만 줄로 불어난 것으로 실측
// 확인) — 데이터 정합성이 성능/안정성 사고로 직결되는 케이스라 DB 제약으로 막아둔다.
@Table(name="employee_chatroom",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "room_id"}))
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

    // INFO : Employee는 다른 애그리게이트라 객체(@ManyToOne)로 물지 않고 id로만 참조한다.
    // 예전엔 @ManyToOne Employee였는데, 이 경우 lazy-loading이 의도치 않게 Employee의
    // 객체 그래프를 끌고 들어올 수 있고 두 애그리게이트의 트랜잭션 경계가 흐려진다.
    // 이름/부서 등 표시용 데이터가 필요한 곳은 서비스 레이어에서 EmployeeRepository를
    // 따로 조회해서 조합한다(#74 Phase 0).
    @Column(name = "employee_id")
    private Long employeeId;

    // INFO : 이 사람이 이 방에서 마지막으로 읽은 메시지의 id. null이면 아직 하나도 안 읽음.
    // 예전엔 ChatMessage 쪽에 메시지당 is_read(전역 boolean) 하나만 있어서, 그룹 채팅방에서
    // 한 명만 읽어도 전원 읽음 처리되는 버그가 있었다 — "사람별로"(employee_chatroom 단위)
    // 마지막으로 읽은 지점을 따로 들고 있는 방식으로 바꿔서 1:1/단체 모두 정확히 계산한다.
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

}
