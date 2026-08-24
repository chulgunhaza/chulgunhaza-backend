package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.security.filter.SessionCheckFilter;
import com.example.chulgunhazabackend.security.handler.LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final SessionCheckFilter sessionCheckFilter;
    private final AccessDeniedHandler accessDeniedHandler;
    private final UserDetailsService userDetailsService;

    // #54: 와일드카드("*") + allowCredentials(true) 조합은 브라우저에 따라 요청이
    // 차단되거나, 차단되지 않는 환경에서는 임의 origin이 세션 쿠키를 실어 보낼 수 있어
    // 명시적 화이트리스트로 좁혔다. 기본값은 로컬 프론트 개발 서버(localhost:3000),
    // 운영 환경에서는 CORS_ALLOWED_ORIGINS 환경변수(콤마 구분)로 덮어쓴다.
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception{

        // CORS 설정
        httpSecurity.cors(
                httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer
                        .configurationSource(corsConfigurationSource())
        );

        // CSRF 설정
        httpSecurity.csrf(
                httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable()
        );

        // SESSION 설정
        httpSecurity.sessionManagement(
                httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer
                                .sessionCreationPolicy(SessionCreationPolicy.NEVER)

        );

        // 인증 처리
        httpSecurity.formLogin(
                httpSecurityFormLoginConfigurer -> {

                    // 로그인 URL
                    httpSecurityFormLoginConfigurer.loginPage("/v1/employee/login");

                    // 인증 성공 처리
                    httpSecurityFormLoginConfigurer.successHandler(authenticationSuccessHandler);

                    // 인증 실패 처리
                    httpSecurityFormLoginConfigurer.failureHandler(authenticationFailureHandler);
                }
        );

        // 로그아웃 처리
        httpSecurity.logout(
            httpSecurityLogoutConfigurer -> {
                httpSecurityLogoutConfigurer.logoutUrl("/v1/employee/logout");

                httpSecurityLogoutConfigurer.logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json; charset=UTF-8"); // 200 OK
                    response.getWriter().write("로그아웃 성공");
                    response.getWriter().flush();
                });
                httpSecurityLogoutConfigurer.invalidateHttpSession(true);
                httpSecurityLogoutConfigurer.deleteCookies("JSESSIONID");
            }
        );


        // 권한 처리
        httpSecurity.exceptionHandling(
                httpSecurityExceptionHandlingConfigurer ->
                        httpSecurityExceptionHandlingConfigurer.accessDeniedHandler(accessDeniedHandler)
        );


        // 필터 위치 지정 
        httpSecurity.addFilterBefore(sessionCheckFilter, UsernamePasswordAuthenticationFilter.class);

        // daoAuthenticationProvider 지정
        httpSecurity.authenticationProvider(daoAuthenticationProvider());


        return httpSecurity.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return  daoAuthenticationProvider;
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new AppCorsConfigurationSource(allowedOrigins);
    }

}
