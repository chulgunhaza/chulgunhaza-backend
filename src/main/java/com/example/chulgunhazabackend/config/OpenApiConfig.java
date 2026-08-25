package com.example.chulgunhazabackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * #52 Swagger/OpenAPI 문서화.
 *
 * <p>이 프로젝트는 세션 쿠키(JSESSIONID) 기반 인증이라, springdoc이 기본으로 지원하는
 * bearer/apiKey 스킴 대신 {@code cookieAuth}(apiKey in=cookie)로 등록해서
 * Swagger UI의 "Authorize" 버튼으로도 실제 로그인 세션 쿠키를 흉내낼 수 있게 했다
 * (브라우저가 같은 오리진으로 로그인해서 쿠키를 이미 들고 있으면 Swagger UI 요청에도
 * 자동으로 실린다).</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String COOKIE_AUTH = "cookieAuth";

    @Bean
    public OpenAPI chulgunhazaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("출근하자 (chulgunhaza-backend) API")
                        .description("출퇴근 기록 + 실시간 소통 그룹웨어 API 문서. " +
                                "로그인은 JSESSIONID 세션 쿠키 방식이며, POST /v1/employee/login 으로 로그인 후 " +
                                "브라우저가 자동으로 실어 보내는 쿠키로 인증됩니다.")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")));
    }
}
