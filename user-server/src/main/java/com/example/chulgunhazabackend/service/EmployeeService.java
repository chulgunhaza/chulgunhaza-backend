package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.Employee.EmployeeModifyRequestDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeResponseDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCreateRequestDto;
import com.example.chulgunhazabackend.dto.PageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface EmployeeService {

    Long create(EmployeeCreateRequestDto employeeCreateRequestDto, Long executor) throws IOException;

    PageDto<?> getEmployeeList(Pageable pageable);

    EmployeeResponseDto getEmployee(Long id);

    EmployeeResponseDto modifyById(Long id, EmployeeModifyRequestDto employeeModifyRequestDto,
                                   MultipartFile image, Long executor) throws IOException;
    Long deleteById(Long id);

}
