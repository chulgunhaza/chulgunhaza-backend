package com.example.chulgunhazabackend.security.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

// #98: jwt.private-key/jwt.public-key(PEM을 base64 한 줄로 인코딩한 문자열)를
// 실제 java.security.PrivateKey/PublicKey 객체로 파싱해서 애플리케이션 기동 시
// 한 번만 만들어둔다. 키 생성 방법은 .env.example 참고.
@Getter
@Component
@RequiredArgsConstructor
public class JwtKeyProvider {

    private final JwtProperties jwtProperties;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    @jakarta.annotation.PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey = parsePrivateKey(jwtProperties.getPrivateKey());
        this.publicKey = parsePublicKey(jwtProperties.getPublicKey());
    }

    private PrivateKey parsePrivateKey(String base64Pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = stripPemAndDecode(base64Pem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey parsePublicKey(String base64Pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = stripPemAndDecode(base64Pem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(der));
    }

    // .env엔 "PEM 텍스트 전체를 base64 한 줄로 인코딩한 값"이 들어있다 (PEM 헤더/푸터/줄바꿈 포함째로).
    // 한 번 base64 디코딩하면 PEM 텍스트가 나오고, 거기서 헤더/푸터/줄바꿈을 걷어내면
    // KeyFactory가 바로 먹는 순수 DER 바이트가 된다.
    private byte[] stripPemAndDecode(String base64Pem) {
        String pem = new String(Base64.getDecoder().decode(base64Pem.trim()));
        String cleaned = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
