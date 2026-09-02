package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.chat.ChatRoom;
import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import com.example.chulgunhazabackend.repository.EmployeeChatRoomRepository;
import com.example.chulgunhazabackend.service.EmployeeChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeChatRoomServiceImpl implements EmployeeChatRoomService {

    private final EmployeeChatRoomRepository employeeChatRoomRepository;

    @Override
    public Long save(ChatRoom chatRoom, Long senderEmployeeId, List<Long> memberEmployeeIds) {

        // INFO : 개설자(senderEmployeeId) 저장
        employeeChatRoomRepository.save(EmployeeChatRoom.builder().chatRoom(chatRoom).employeeId(senderEmployeeId).build());

        // INFO : 대화 상대(들) 저장 — 1명이면 1:1, 2명 이상이면 단체 채팅방이 된다.
        for (Long memberId : memberEmployeeIds) {
            employeeChatRoomRepository.save(EmployeeChatRoom.builder().chatRoom(chatRoom).employeeId(memberId).build());
        }

        return chatRoom.getId();
    }

    // INFO : 사원 아이디로 그 사원이 속한 채팅방 목록 가져오기.
    // 방마다 "내" 참여 기록 1건만 나오므로 그룹 채팅에서도 방이 중복 노출되지 않는다.
    @Override
    public Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable) {
        return employeeChatRoomRepository.findByEmployeeId(employeeId, pageable);
    }
}
