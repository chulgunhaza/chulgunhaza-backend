package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.security.EmployeeCredentialDtoFactory;
import com.example.chulgunhazabackend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("loadUserByUserName 동작");

        Employee employee = employeeRepository
                .findEmployeeByEmailWithUserRoleList(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("NOT_FOUND_EMAIL")
                );

        return EmployeeCredentialDtoFactory.from(employee);
    }
}
