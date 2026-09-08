package com.example.chulgunhazabackend.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

// #100: JwtAuthenticationFilter가 인증 없이 통과시킬 경로 목록을 서비스마다 다르게
// 가져가기 위해 뺐다. 예전엔 이 목록이 필터 안에 하드코딩돼 있어서(로그인/재발급/
// swagger 4개 고정) attendance-server 같은 새 서비스가 자기만의 예외 경로
// (/internal, /actuator)를 추가할 방법이 없었다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt.auth")
public class JwtAuthProperties {

    private List<String> exemptPathPrefixes = List.of();
}
