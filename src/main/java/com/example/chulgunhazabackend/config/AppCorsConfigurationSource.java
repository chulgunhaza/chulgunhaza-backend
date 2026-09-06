package com.example.chulgunhazabackend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 명시적으로 허용한 origin 목록에 대해서만 CORS를 열어주는 {@link CorsConfigurationSource}.
 *
 * <p>이전에는 {@code setAllowedOriginPatterns(Arrays.asList("*"))} 로 모든 origin을
 * 허용하면서 동시에 {@code allowCredentials(true)} 를 켜두고 있었다. 와일드카드와
 * credentials 조합은 브라우저에 따라 요청 자체가 차단되기도 하고, 차단되지 않는
 * 환경에서는 임의의 사이트가 사용자 세션 쿠키를 실어 요청을 보낼 수 있어 CSRF 계열
 * 공격에 노출된다. 그래서 허용 origin을 {@code cors.allowed-origins} 설정값으로
 * 명시적으로 좁혔다.</p>
 */
public class AppCorsConfigurationSource implements CorsConfigurationSource {

    private final List<String> allowedOrigins;
    private final CorsConfigurationSource delegate;

    public AppCorsConfigurationSource(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(allowedOrigins);
        corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // PATCH가 빠져있어서 @PatchMapping을 쓰는 엔드포인트(사원 삭제, 게시글 삭제)가
        // 전부 프리플라이트(OPTIONS) 단계에서 403으로 막히고 있었다 — curl로는 프리플라이트
        // 자체가 없어서 지금까지 발견이 안 됐다가, 실제 브라우저에서 관리자 페이지 삭제
        // 버튼을 눌러보다가 발견했다.
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
        corsConfiguration.setAllowCredentials(true); // 헤더/쿠키 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        this.delegate = source;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        return delegate.getCorsConfiguration(request);
    }
}
