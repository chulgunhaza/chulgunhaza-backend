package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeChatRoomService {

    // INFO : senderEmployee(개설자) + memberEmployees(대화 상대, 1명 이상 — 단체 채팅 지원)를
    // 같은 채팅방에 묶어 저장한다.
    Long save(ChatRoom chatRoom, Employee senderEmployee, List<Employee> memberEmployees);

    Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable);

}
