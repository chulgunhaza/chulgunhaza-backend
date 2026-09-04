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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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

    // INFO : SessionCheckFilter가 매 요청마다 세션의 낱개 attribute(id/email/...)만 읽어서
    // SecurityContextHolder에 새 Authentication을 꽂아넣다 보니, Spring Security 표준
    // 저장소(SPRING_SECURITY_CONTEXT 세션 attribute)엔 아무것도 안 남는다. 그래서
    // SessionManagementFilter가 "저장된 컨텍스트가 없는데 인증된 사용자가 있네" =
    // "방금 로그인했나 보다"로 매 요청마다 오판해서 세션 ID를 계속 새로 발급했다
    // (동시 요청 시 hasKey 경합 → "Session was invalidated" 500의 진짜 트리거,
    // docs/troubleshooting-concurrent-session-race.md 참고). 로그인 시점에 한 번만
    // 표준 저장소에도 저장해두면, 그 뒤로는 SessionManagementFilter가 "이미 저장된
    // 컨텍스트 있음"으로 정상 인식해서 더 이상 회전시키지 않는다.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

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

        // 표준 저장소에도 한 번만 저장 (위 INFO 참고)
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

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
