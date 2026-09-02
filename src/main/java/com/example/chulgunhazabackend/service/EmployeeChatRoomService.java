package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeChatRoomService {

    // INFO : senderEmployeeId(개설자) + memberEmployeeIds(대화 상대, 1명 이상 — 단체 채팅 지원)를
    // 같은 채팅방에 묶어 저장한다. Employee 객체가 아니라 id로만 받는다 — 다른 애그리게이트라
    // 여기서 객체 그래프를 들고 있을 이유가 없다(#74 Phase 0).
    Long save(ChatRoom chatRoom, Long senderEmployeeId, List<Long> memberEmployeeIds);

    Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable);

}
