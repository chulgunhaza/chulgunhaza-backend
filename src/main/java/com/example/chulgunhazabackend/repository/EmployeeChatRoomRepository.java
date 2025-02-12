package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.chat.EmployeeChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeChatRoomRepository extends JpaRepository<EmployeeChatRoom, Long> {

    Page<EmployeeChatRoom> findByEmployeeId(Long employeeId, Pageable pageable);

    @Query("SELECT ec1.chatRoom.id " +
            "FROM EmployeeChatRoom ec1 " +
            "JOIN EmployeeChatRoom ec2 ON ec1.chatRoom.id = ec2.chatRoom.id " +
            "WHERE ec1.employee.id = :senderId " +
            "AND ec2.employee.id = :receiverId" )
    Optional<Long> findRoomIdByEmployeeIds(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Query("SELECT c FROM EmployeeChatRoom c WHERE c.chatRoom.id IN " +
            "(SELECT cr.chatRoom.id FROM EmployeeChatRoom cr WHERE cr.employee.id = :employeeId) " +
            "AND c.employee.id != :employeeId")
    Page<EmployeeChatRoom> findChatRoomsExcludingEmployee(@Param("employeeId") Long employeeId, Pageable pageable);

    Optional<EmployeeChatRoom> findByEmployeeIdAndChatRoomId(Long employeeId, Long chatRoomId);

}
