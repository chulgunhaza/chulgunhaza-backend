package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeChatRoomRepository extends JpaRepository<EmployeeChatRoom, Long> {

    // INFO : 내가 속한 채팅방 목록 — 그룹 채팅에서도 방 하나당 정확히 1행만 나온다(내 참여 기록 기준).
    // (구 findChatRoomsExcludingEmployee 는 "다른 참여자" 기준으로 조회해서 그룹방에서
    //  참여자 수만큼 같은 방이 중복 노출되는 문제가 있었다 — 단체 채팅 지원하며 발견.)
    Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable);

    @Query("SELECT ec1.chatRoom.id " +
            "FROM EmployeeChatRoom ec1 " +
            "JOIN EmployeeChatRoom ec2 ON ec1.chatRoom.id = ec2.chatRoom.id " +
            "WHERE ec1.employee.id = :senderId " +
            "AND ec2.employee.id = :receiverId" )
    Optional<Long> findRoomIdByEmployeeIds(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    // INFO : 특정 채팅방에서 나(employeeId)를 제외한 다른 참여자 전원.
    // 방 목록 표시(그룹 이름 조합)와 메시지 브로드캐스트(WS/SSE 수신자 결정) 양쪽에 쓴다.
    @Query("SELECT ec FROM EmployeeChatRoom ec WHERE ec.chatRoom.id = :chatRoomId AND ec.employee.id != :employeeId")
    List<EmployeeChatRoom> findOtherMembersByChatRoomId(@Param("chatRoomId") Long chatRoomId, @Param("employeeId") Long employeeId);

    Optional<EmployeeChatRoom> findByEmployeeIdAndChatRoomId(Long employeeId, Long chatRoomId);

}
