package com.example.chulgunhazabackend.security.handler;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Gson gson = new Gson();
        
        EmployeeCredentialDto credentialDto = (EmployeeCredentialDto) authentication.getPrincipal();
        Map<String, Object> claims = credentialDto.getClaims();

        HttpSession httpSession = request.getSession();

        // 세션 정보 생성
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }

        // 4시간 (초 단위)
        httpSession.setMaxInactiveInterval(4 * 60 * 60);

        // 응답 데이터 생성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "로그인 성공");
        // id(PK)가 빠져있으면 프론트에서 채팅방 생성(senderId 필요) 등을 만들 방법이 없어서 추가함
        responseData.put("id", claims.get("id"));
        responseData.put("depart", claims.get("department"));
        responseData.put("name", claims.get("name"));
        responseData.put("employeeNo", claims.get("employeeNo"));
        responseData.put("employeeRoles", claims.get("roles"));

        // JSON 응답 생성
        String jsonStr = gson.toJson(responseData);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}
