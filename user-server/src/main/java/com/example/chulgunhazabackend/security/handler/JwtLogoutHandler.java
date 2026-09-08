package com.example.chulgunhazabackend.security.handler;

import com.example.chulgunhazabackend.security.jwt.CookieUtil;
import com.example.chulgunhazabackend.security.jwt.JwtProvider;
import com.example.chulgunhazabackend.security.jwt.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

// #98: 세션 invalidate + JSESSIONID 쿠키 삭제를 대체. Redis에 저장해둔 refresh
// 토큰 jti를 지워서 무효화하고, access/refresh 쿠키를 둘 다 지운다.
//
// Authentication 파라미터를 안 쓰고 refresh_token 쿠키를 직접 파싱해서 employeeId를
// 뽑는다 — 실측해보니 Spring Security 기본 필터 체인에서 LogoutFilter가
// UsernamePasswordAuthenticationFilter보다 먼저 실행되는데, JwtAuthenticationFilter는
// addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)로만 꽂혀 있어서
// LogoutFilter 시점엔 아직 SecurityContext가 채워지기 전이라 authentication이 항상
// null로 들어왔다(로그아웃해도 Redis의 refresh_token 키가 안 지워지는 버그로 실측 확인).
// access 토큰이 이미 만료된 상태로 로그아웃을 호출하는 경우에도 이 방식이면 문제없다.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final RefreshTokenStore refreshTokenStore;
    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String refreshToken = cookieUtil.extractCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);

        if (refreshToken != null) {
            try {
                Claims claims = jwtProvider.parseClaims(refreshToken);
                Long employeeId = Long.valueOf(claims.getSubject());
                refreshTokenStore.delete(employeeId);
            } catch (JwtException e) {
                // 이미 만료됐거나 위조된 refresh 토큰이면 애초에 재사용할 수 없으니
                // Redis에서 지울 필요도 없다 — 조용히 무시하고 쿠키 삭제만 진행.
                log.warn("로그아웃 시 refresh 토큰 검증 실패(무시): {}", e.getMessage());
            }
        }

        cookieUtil.clearAuthCookies(response);
    }
}
