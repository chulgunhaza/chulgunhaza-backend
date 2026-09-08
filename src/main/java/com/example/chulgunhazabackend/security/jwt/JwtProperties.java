package com.example.chulgunhazabackend.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// #98: application.yml의 jwt.* 값을 그대로 바인딩. private/public key는 PEM을
// base64 한 줄로 인코딩해서 담아둔 문자열이라, 실제 PrivateKey/PublicKey 객체로
// 파싱하는 건 JwtKeyProvider가 한다 (이 클래스는 원본 문자열 보관용).
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String privateKey;

    private String publicKey;

    private long accessTokenTtlSeconds;

    private long refreshTokenTtlSeconds;

    private String cookieDomain;

    private boolean cookieSecure;
}
