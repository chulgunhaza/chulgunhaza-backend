package com.example.chulgunhazabackend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #54 CORS 와일드카드 + credentials 조합 재검토 회귀 테스트.
 *
 * <p>고치기 전 상태({@code setAllowedOriginPatterns(Arrays.asList("*"))})라면
 * {@link #허용_목록에_없는_origin은_거부한다()} 가 실패하도록 설계했다. 즉 이 테스트가
 * 통과한다는 것 자체가 "와일드카드로 아무 origin이나 열려 있던" 원래 버그가 재발하지
 * 않았다는 증거가 된다.</p>
 */
class AppCorsConfigurationSourceTest {

    private static final List<String> ALLOWED = List.of("http://localhost:3000");

    private CorsConfiguration configFor(String origin) {
        AppCorsConfigurationSource source = new AppCorsConfigurationSource(ALLOWED);
        HttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration resolved = source.getCorsConfiguration(request);
        return resolved;
    }

    @Test
    @DisplayName("허용 목록에 있는 origin은 통과한다")
    void 허용된_origin은_통과한다() {
        CorsConfiguration config = configFor("http://localhost:3000");

        assertThat(config.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("허용 목록에 없는 origin은 거부한다 — 와일드카드 회귀 방지")
    void 허용_목록에_없는_origin은_거부한다() {
        CorsConfiguration config = configFor("http://localhost:3000");

        assertThat(config.checkOrigin("http://evil.example.com")).isNull();
    }

    @Test
    @DisplayName("와일드카드 패턴은 더 이상 설정에 포함되지 않는다")
    void 와일드카드_패턴은_없다() {
        AppCorsConfigurationSource source = new AppCorsConfigurationSource(ALLOWED);

        assertThat(source.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    @DisplayName("credentials(쿠키/세션 인증)는 계속 허용한다 — 세션 로그인 기능 유지")
    void credentials는_허용된다() {
        CorsConfiguration config = configFor("http://localhost:3000");

        assertThat(config.getAllowCredentials()).isTrue();
    }
}
