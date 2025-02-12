package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.service.EmployeeChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeChatRoomServiceImpl implements EmployeeChatRoomService {

    private final EmployeeChatRoomRepository employeeChatRoomRepository;

    @Override
    public Long save(ChatRoom chatRoom, Employee senderEmployee, Employee receiverEmployee) {

        // INFO : senderEmployee 저장
        employeeChatRoomRepository.save(EmployeeChatRoom.builder().chatRoom(chatRoom).employee(senderEmployee).build());

        // INFO : receiverEmployee 저장
        employeeChatRoomRepository.save(EmployeeChatRoom.builder().chatRoom(chatRoom).employee(receiverEmployee).build());

        return chatRoom.getId();
    }

    // INFO : 전송 사원의 아이디로 해당 사원의 채팅방 목록 가져오기
    @Override
    public Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable) {
        return employeeChatRoomRepository.findChatRoomsExcludingEmployee(employeeId, pageable);
    }
}
