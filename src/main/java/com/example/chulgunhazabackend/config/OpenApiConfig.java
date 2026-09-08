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
 * <p>#98: 세션 쿠키(JSESSIONID) → JWT(access_token httpOnly 쿠키) 전환에 맞춰
 * 스킴 이름도 갱신. 여전히 bearer가 아니라 {@code cookieAuth}(apiKey in=cookie)로
 * 등록한 이유는 동일 — 브라우저가 같은 오리진으로 로그인해서 access_token 쿠키를
 * 이미 들고 있으면 Swagger UI 요청에도 자동으로 실린다.</p>
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
                                "로그인은 JWT(access_token httpOnly 쿠키) 방식이며, POST /v1/employee/login 으로 로그인 후 " +
                                "브라우저가 자동으로 실어 보내는 쿠키로 인증됩니다. access 토큰이 만료되면 " +
                                "POST /v1/employee/token/refresh 로 재발급받습니다.")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("access_token")));
    }
}
