package com.example.chulgunhazabackend.security.handler;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.security.jwt.CookieUtil;
import com.example.chulgunhazabackend.security.jwt.JwtProperties;
import com.example.chulgunhazabackend.security.jwt.JwtProvider;
import com.example.chulgunhazabackend.security.jwt.RefreshTokenStore;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

// #98: 세션 attribute 기반 로그인 처리를 JWT(RS256) 발급으로 교체.
// access/refresh 토큰을 httpOnly 쿠키로 내려주고, refresh 토큰의 jti만 Redis에
// 저장해서(값 자체는 저장하지 않음) 로그아웃/재발급 시 회전·무효화할 수 있게 한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Gson gson = new Gson();

        EmployeeCredentialDto credentialDto = (EmployeeCredentialDto) authentication.getPrincipal();

        String accessToken = jwtProvider.issueAccessToken(credentialDto);
        JwtProvider.IssuedRefreshToken refreshToken = jwtProvider.issueRefreshToken(credentialDto);

        refreshTokenStore.save(credentialDto.getId(), refreshToken.getJti(), jwtProperties.getRefreshTokenTtlSeconds());

        cookieUtil.addAccessTokenCookie(response, accessToken);
        cookieUtil.addRefreshTokenCookie(response, refreshToken.getToken());

        // 응답 데이터 생성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "로그인 성공");
        // id(PK)가 빠져있으면 프론트에서 채팅방 생성(senderId 필요) 등을 만들 방법이 없어서 추가함
        responseData.put("id", credentialDto.getId());
        responseData.put("depart", credentialDto.getDepartment());
        responseData.put("name", credentialDto.getName());
        responseData.put("employeeNo", credentialDto.getEmployeeNo());
        responseData.put("employeeRoles", credentialDto.getRoles());

        // JSON 응답 생성
        String jsonStr = gson.toJson(responseData);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}
