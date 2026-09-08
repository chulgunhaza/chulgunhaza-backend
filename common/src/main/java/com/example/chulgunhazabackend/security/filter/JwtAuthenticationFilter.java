package com.example.chulgunhazabackend.security.filter;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.security.jwt.CookieUtil;
import com.example.chulgunhazabackend.security.jwt.JwtAuthProperties;
import com.example.chulgunhazabackend.security.jwt.JwtProvider;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

// #98: 세션(HttpSession attribute) 기반 인증 필터를 JWT(RS256) 검증으로 교체.
// access_token 쿠키를 읽어 서명·만료를 검증하고, 통과하면 SecurityContextHolder에
// Authentication을 심는다 — 그 이후는 예전 SessionCheckFilter와 동일한 지점부터
// 재사용되므로(@AuthenticationPrincipal, WebSocket 핸드셰이크의
// SecurityContextInterceptor 포함) 이 필터 밖에서는 손댈 게 없다.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT 검증 실패 시 재구성하는 EmployeeCredentialDto용 더미 password. User(email,
    // password, authorities) 상위 생성자가 비어있는 password를 거부해서 채워 넣을 뿐,
    // 이미 서명 검증으로 인증이 끝난 뒤라 이 값이 실제로 쓰이는 곳은 없다.
    private static final String JWT_AUTHENTICATED_PLACEHOLDER = "[JWT_AUTHENTICATED]";

    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    // #100: 인증 없이 통과할 경로 목록을 서비스별 application.yml(jwt.auth.exempt-path-prefixes)로
    // 뺐다 — 예전엔 여기 하드코딩(로그인/재발급/swagger 4개 고정)이라 attendance-server
    // 같은 새 서비스가 자기만의 예외 경로(/internal, /actuator)를 추가할 방법이 없었다.
    private final JwtAuthProperties jwtAuthProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return jwtAuthProperties.getExemptPathPrefixes().stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String accessToken = cookieUtil.extractCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE);

        if (accessToken == null) {
            log.warn("access_token 쿠키가 없습니다.");
            respondUnauthorized(response, "로그인을 먼저 진행해 주세요.");
            return;
        }

        try {
            Claims claims = jwtProvider.parseClaims(accessToken);
            EmployeeCredentialDto credentialDto = toCredentialDto(claims);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(credentialDto, null, credentialDto.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            respondUnauthorized(response, "로그인이 만료되었거나 유효하지 않습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private EmployeeCredentialDto toCredentialDto(Claims claims) {
        Long id = claims.get("id", Long.class);
        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);
        Long employeeNo = claims.get("employeeNo", Long.class);
        String department = claims.get("department", String.class);
        List<String> roles = (List<String>) (List<?>) claims.get("roles", List.class);

        return new EmployeeCredentialDto(id, email, JWT_AUTHENTICATED_PLACEHOLDER, name, employeeNo, roles, department);
    }

    private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("error", message));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}
