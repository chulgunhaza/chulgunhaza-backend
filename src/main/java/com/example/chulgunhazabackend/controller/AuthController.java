package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.security.jwt.CookieUtil;
import com.example.chulgunhazabackend.security.jwt.JwtProperties;
import com.example.chulgunhazabackend.security.jwt.JwtProvider;
import com.example.chulgunhazabackend.security.jwt.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// #98: 세션 → JWT 전환에 따라 새로 필요해진 두 엔드포인트.
// - /me: 프론트가 새로고침 시 "지금 로그인 상태인지"를 서버에 직접 물어볼 방법이
//   예전엔 없어서(sessionStorage 캐시만 믿었음) 이번에 실제로 만들었다.
// - /token/refresh: access 토큰(15분) 만료 시 재로그인 없이 재발급받는 엔드포인트.
//   프론트 axios interceptor가 401을 받으면 이걸 호출한다.
@RestController
@RequestMapping("/v1/employee")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;
    private final RefreshTokenStore refreshTokenStore;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal EmployeeCredentialDto credentialDto) {
        return ResponseEntity.ok(toResponseBody(credentialDto, "인증됨"));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null) {
            throw new EmployeeException(EmployeeExceptionType.INVALID_REFRESH_TOKEN);
        }

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(refreshToken);
        } catch (JwtException e) {
            cookieUtil.clearAuthCookies(response);
            throw new EmployeeException(EmployeeExceptionType.INVALID_REFRESH_TOKEN);
        }

        Long employeeId = Long.valueOf(claims.getSubject());
        String jti = claims.getId();

        boolean matchesStoredJti = refreshTokenStore.find(employeeId)
                .map(storedJti -> storedJti.equals(jti))
                .orElse(false);

        // 저장된 jti와 다르면 로그아웃됐거나, 이미 한 번 회전된 refresh 토큰을
        // 재사용하려는 시도(탈취 가능성) — 둘 다 재로그인을 요구한다.
        if (!matchesStoredJti) {
            cookieUtil.clearAuthCookies(response);
            throw new EmployeeException(EmployeeExceptionType.INVALID_REFRESH_TOKEN);
        }

        // refresh 토큰 자체엔 role/부서가 없어서(재발급 시점 기준 최신 값을 반영하려고
        // 일부러 안 담았다) 여기서 다시 조회한다.
        Employee employee = employeeRepository.findEmployeeByIdWithUserRoleList(employeeId)
                .orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));

        EmployeeCredentialDto credentialDto = EmployeeCredentialDto.from(employee);

        String newAccessToken = jwtProvider.issueAccessToken(credentialDto);
        JwtProvider.IssuedRefreshToken newRefreshToken = jwtProvider.issueRefreshToken(credentialDto);
        refreshTokenStore.save(employeeId, newRefreshToken.getJti(), jwtProperties.getRefreshTokenTtlSeconds());

        cookieUtil.addAccessTokenCookie(response, newAccessToken);
        cookieUtil.addRefreshTokenCookie(response, newRefreshToken.getToken());

        return ResponseEntity.ok(toResponseBody(credentialDto, "토큰 재발급 완료"));
    }

    // LoginSuccessHandler의 로그인 응답과 동일한 shape — 프론트가 하나의
    // LoginResponse 타입으로 로그인/새로고침/재발급 응답을 모두 받을 수 있게 한다.
    private Map<String, Object> toResponseBody(EmployeeCredentialDto credentialDto, String message) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", message);
        responseData.put("id", credentialDto.getId());
        responseData.put("depart", credentialDto.getDepartment());
        responseData.put("name", credentialDto.getName());
        responseData.put("employeeNo", credentialDto.getEmployeeNo());
        responseData.put("employeeRoles", credentialDto.getRoles());
        return responseData;
    }
}
