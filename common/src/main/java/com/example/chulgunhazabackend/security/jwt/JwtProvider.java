package com.example.chulgunhazabackend.security.jwt;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// #98: RS256 access/refresh 토큰 발급 + 검증. 클레임은 EmployeeCredentialDto의
// 공개 필드만 담는다 — password는 절대 넣지 않는다(JWT payload는 base64만 씌워진
// 것이라 토큰을 가진 누구나 디코딩해서 볼 수 있음).
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String ISSUER = "chulgunhaza-user-server";

    private final JwtKeyProvider jwtKeyProvider;

    private final JwtProperties jwtProperties;

    public String issueAccessToken(EmployeeCredentialDto credentialDto) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(credentialDto.getId()))
                .claims(publicClaims(credentialDto))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getAccessTokenTtlSeconds(), ChronoUnit.SECONDS)))
                .signWith(privateKey())
                .compact();
    }

    public IssuedRefreshToken issueRefreshToken(EmployeeCredentialDto credentialDto) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(credentialDto.getId()))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getRefreshTokenTtlSeconds(), ChronoUnit.SECONDS)))
                .signWith(privateKey())
                .compact();

        return new IssuedRefreshToken(token, jti);
    }

    // 서명 검증 + 만료 확인. 실패 시 io.jsonwebtoken.JwtException 계열(만료는
    // ExpiredJwtException) 그대로 던지므로 호출부에서 캐치해서 401로 변환한다.
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Map<String, Object> publicClaims(EmployeeCredentialDto credentialDto) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", credentialDto.getId());
        claims.put("email", credentialDto.getEmail());
        claims.put("name", credentialDto.getName());
        claims.put("employeeNo", credentialDto.getEmployeeNo());
        claims.put("roles", credentialDto.getRoles());
        claims.put("department", credentialDto.getDepartment());
        return claims;
    }

    private PrivateKey privateKey() {
        return jwtKeyProvider.getPrivateKey();
    }

    private PublicKey publicKey() {
        return jwtKeyProvider.getPublicKey();
    }

    @Getter
    public static class IssuedRefreshToken {
        private final String token;
        private final String jti;

        public IssuedRefreshToken(String token, String jti) {
            this.token = token;
            this.jti = jti;
        }
    }
}
