package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.domain.member.EmployeeImage;
import com.example.chulgunhazabackend.dto.Employee.EmployeeListResponseDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeModifyRequestDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeResponseDto;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCreateRequestDto;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.event.employee.event.EmployeeCreateEvent;
import com.example.chulgunhazabackend.event.employee.event.EmployeeModifyEvent;
import com.example.chulgunhazabackend.event.common.Events;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.EmployeeService;
import com.example.chulgunhazabackend.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    private final PasswordEncoder passwordEncoder;

    private final FileService fileService;

    // 사원 최초 생성 시 기본이미지, 기본 연차 생성

    public Long create(EmployeeCreateRequestDto employeeCreateRequestDto, Long executor) throws IOException{

        Events.raise(new EmployeeCreateEvent(executor)); //진행하는 사람
        checkEmail(employeeCreateRequestDto.getEmail());
        EmployeeImage employeeImage = EmployeeImage.builder()
                .imageName("default image")
                .build(); // 기본 이미지를 사용한다고 지정

        Employee employee = employeeCreateRequestDto.toEntity(employeeImage, new Annual());
        employee.setInitialPassword(passwordEncoder); // 초기 비밀번호 지정
        Employee saved = employeeRepository.save(employee);
        return saved.getId();
    }

    // 사원 목록 조회
    @Transactional(readOnly = true)
    public PageDto<?> getEmployeeList(Pageable pageable){
        Page<EmployeeListResponseDto> contents = employeeRepository.selectEmployeeList(pageable)
                .map(EmployeeListResponseDto::fromEntity);
        return new PageDto<>(contents);

    }
    // 사원 조회
    @Transactional(readOnly = true)
    @Override
    public EmployeeResponseDto getEmployee(Long id) {
        Employee employee = employeeRepository.findEmployeeById(id).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
        return EmployeeResponseDto.fromEntity(employee);
    }


    // 사원 정보 수정
    @Transactional(rollbackFor = IOException.class)
    public EmployeeResponseDto modifyById(Long id, EmployeeModifyRequestDto employeeModifyRequestDto,
                                          MultipartFile image, Long executor) throws IOException {
        Events.raise(new EmployeeModifyEvent(executor));

        Employee findEmployee = employeeRepository.findEmployeeByIdForUpdate(id).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        if(!employeeModifyRequestDto.getEmail().equals(findEmployee.getEmail())){
            checkEmail(employeeModifyRequestDto.getEmail());
        }

        checkVersion(findEmployee, employeeModifyRequestDto.getVersion());

        findEmployee.updateEmployee(employeeModifyRequestDto, resolveEmployeeImage(image));
        Employee saved = employeeRepository.saveAndFlush(findEmployee);

        return EmployeeResponseDto.fromEntity(saved);
    }


    public Long deleteById(Long id){
        Employee findEmployee = employeeRepository.findEmployeeByIdForUpdate(id).orElseThrow(()
                -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
        findEmployee.delete();
        return employeeRepository.save(findEmployee).getId();
    }
    public void checkEmail(String email){
        Optional<Employee> employeeEmail = employeeRepository.findEmployeeByEmail(email);
        if(employeeEmail.isPresent()){
            throw new EmployeeException(EmployeeExceptionType.ALREADY_EXIST_EMAIL);
        }
    }

    private void checkVersion(Employee findEmployee, long version){
        if(!findEmployee.matchVersion(version)){
            throw new EmployeeException(EmployeeExceptionType.ALREADY_CHANGED);
        }
    }


    // #58: 새 이미지가 없으면(파라미터 자체가 없거나 빈 파일) 기본 이미지를 유지하고,
    // 실제 파일이 왔으면 FileService로 디스크에 저장해서 실제 경로/크기/타입을 채운다.
    private EmployeeImage resolveEmployeeImage(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return EmployeeImage.builder()
                    .imageName("default image")
                    .build(); // 기본 이미지를 사용한다고 지정
        }
        return fileService.saveEmployeeImage(multipartFile);
    }


}
