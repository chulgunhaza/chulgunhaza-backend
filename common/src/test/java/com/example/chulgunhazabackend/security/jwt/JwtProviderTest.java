package com.example.chulgunhazabackend.security.jwt;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #98 — JwtProvider의 access/refresh 토큰 발급·검증 단위 테스트.
 * 실제 .env 키 대신 테스트 전용 RSA 키페어로 JwtKeyProvider를 직접 구성해서
 * Spring 컨텍스트 없이(순수 단위 테스트로) 빠르게 돈다.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        JwtKeyProvider jwtKeyProvider = new JwtKeyProvider(null) {
            {
                // JwtKeyProvider는 @PostConstruct에서 .env의 base64 PEM을 파싱하는데,
                // 테스트에선 이미 만들어둔 KeyPair를 바로 주입하면 되므로 리플렉션 없이
                // 그냥 필드를 덮어쓰는 익명 서브클래스로 우회한다.
            }
        };
        setPrivateField(jwtKeyProvider, keyPair);

        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenTtlSeconds(900);
        jwtProperties.setRefreshTokenTtlSeconds(604800);

        jwtProvider = new JwtProvider(jwtKeyProvider, jwtProperties);
    }

    private void setPrivateField(JwtKeyProvider jwtKeyProvider, KeyPair keyPair) {
        try {
            var privateKeyField = JwtKeyProvider.class.getDeclaredField("privateKey");
            privateKeyField.setAccessible(true);
            privateKeyField.set(jwtKeyProvider, keyPair.getPrivate());

            var publicKeyField = JwtKeyProvider.class.getDeclaredField("publicKey");
            publicKeyField.setAccessible(true);
            publicKeyField.set(jwtKeyProvider, keyPair.getPublic());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private EmployeeCredentialDto sampleCredentialDto() {
        return new EmployeeCredentialDto(
                1L, "test@chulgunhaza.com", "irrelevant-for-jwt", "홍길동",
                10000001L, List.of("USER", "MANAGER"), "개발팀"
        );
    }

    @Test
    @DisplayName("access 토큰을 발급하고 파싱하면 발급 시 담았던 클레임을 그대로 되돌려받는다")
    void access_토큰_발급_후_파싱하면_클레임이_그대로_보존된다() {
        EmployeeCredentialDto dto = sampleCredentialDto();

        String token = jwtProvider.issueAccessToken(dto);
        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.get("id", Long.class)).isEqualTo(1L);
        assertThat(claims.get("email", String.class)).isEqualTo("test@chulgunhaza.com");
        assertThat(claims.get("name", String.class)).isEqualTo("홍길동");
        assertThat(claims.get("employeeNo", Long.class)).isEqualTo(10000001L);
        assertThat(claims.get("department", String.class)).isEqualTo("개발팀");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) (List<?>) claims.get("roles", List.class);
        assertThat(roles).containsExactly("USER", "MANAGER");
    }

    @Test
    @DisplayName("access 토큰 클레임에는 password가 절대 포함되지 않는다")
    void access_토큰에는_password가_없다() {
        String token = jwtProvider.issueAccessToken(sampleCredentialDto());

        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.containsKey("password")).isFalse();
    }

    @Test
    @DisplayName("refresh 토큰은 subject(employeeId)와 jti만 담고, 발급할 때마다 jti가 달라진다")
    void refresh_토큰은_subject와_jti만_담고_매번_jti가_다르다() {
        EmployeeCredentialDto dto = sampleCredentialDto();

        JwtProvider.IssuedRefreshToken first = jwtProvider.issueRefreshToken(dto);
        JwtProvider.IssuedRefreshToken second = jwtProvider.issueRefreshToken(dto);

        Claims claims = jwtProvider.parseClaims(first.getToken());
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getId()).isEqualTo(first.getJti());
        assertThat(first.getJti()).isNotEqualTo(second.getJti());
    }

    @Test
    @DisplayName("서명 키가 다른 토큰(위조)은 검증에 실패한다")
    void 다른_키로_서명된_토큰은_검증에_실패한다() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair otherKeyPair = keyPairGenerator.generateKeyPair();

        String forgedToken = Jwts.builder()
                .subject("1")
                .signWith(otherKeyPair.getPrivate())
                .compact();

        assertThatThrownBy(() -> jwtProvider.parseClaims(forgedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredJwtException으로 검증에 실패한다")
    void 만료된_토큰은_검증에_실패한다() {
        // JwtProvider가 시스템 시계 기준으로 만료를 판단하므로, 이미 지나간
        // expiration을 직접 박아서 만료 케이스를 재현한다.
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(extractPrivateKeyViaAccessToken())
                .compact();

        assertThatThrownBy(() -> jwtProvider.parseClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // 테스트에서만 쓰는 편법: JwtProvider가 실제로 서명에 쓰는 private key를 다시
    // 꺼낼 공개 API가 없어서, setUp에서 심어둔 필드를 리플렉션으로 재사용한다.
    private java.security.PrivateKey extractPrivateKeyViaAccessToken() {
        try {
            var keyProviderField = JwtProvider.class.getDeclaredField("jwtKeyProvider");
            keyProviderField.setAccessible(true);
            JwtKeyProvider jwtKeyProvider = (JwtKeyProvider) keyProviderField.get(jwtProvider);
            return jwtKeyProvider.getPrivateKey();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
