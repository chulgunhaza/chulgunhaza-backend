package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.member.Employee;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findEmployeeByEmail(String email);

    @EntityGraph(attributePaths = {"userRoleList"})
    @Query("SELECT e FROM Employee e WHERE e.email = :email")
    Optional<Employee> findEmployeeByEmailWithUserRoleList(@Param("email") String email);

    Optional<Employee> findEmployeeById(Long id);

    @Query("SELECT e FROM Employee e " +
            "WHERE e.delFlag = false " +
            "ORDER BY e.department ASC, e.position, e.name")
    Page<Employee> selectEmployeeList(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT e FROM Employee e WHERE e.id = :employeeId")
    Optional<Employee> findEmployeeByIdForUpdate(@Param("employeeId") Long employeeId);
    // 업데이트 동시성 제어


}
