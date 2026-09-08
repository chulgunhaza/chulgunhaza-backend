package com.example.chulgunhazabackend.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// #98: access/refresh 토큰을 httpOnly 쿠키로 내려주고 지우는 helper.
// jakarta.servlet.http.Cookie는 SameSite 속성을 못 다뤄서(서블릿 6.1 이전) Spring의
// ResponseCookie로 만들어 Set-Cookie 헤더를 직접 추가한다.
@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/v1/employee";

    private final JwtProperties jwtProperties;

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, ACCESS_TOKEN_COOKIE, token, "/", jwtProperties.getAccessTokenTtlSeconds());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, REFRESH_TOKEN_COOKIE, token, REFRESH_TOKEN_PATH, jwtProperties.getRefreshTokenTtlSeconds());
    }

    // 로그아웃/재발급 실패 시 두 쿠키를 모두 지운다. Max-Age=0으로 덮어써야 브라우저가
    // 지우므로, 발급할 때와 Path가 정확히 같아야 한다(다르면 새 쿠키로 취급돼 안 지워짐).
    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", "/", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, 0);
    }

    // JwtAuthenticationFilter, AuthController 양쪽에서 access_token/refresh_token
    // 쿠키 값을 읽어야 해서 공통으로 뺐다.
    public String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAgeSeconds);

        if (StringUtils.hasText(jwtProperties.getCookieDomain())) {
            builder.domain(jwtProperties.getCookieDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
