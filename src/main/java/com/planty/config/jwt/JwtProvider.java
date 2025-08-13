package com.planty.config.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.regex.Pattern;


// JWT 발급 및 파싱
@Component
public class JwtProvider {

    private final String secretRaw;
    private final long accessExpMillis;
    private Key key;

    // jwt.secret, jwt.access-exp-millis 필드에 저장
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-millis}") long accessExpMillis
    ) {
        this.secretRaw = secret;
        this.accessExpMillis = accessExpMillis;
    }

    // Base64 형태인지 판별 (문자셋+패딩)
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");

    // JWT 서명용 Key 객체 생성
    @PostConstruct
    void init() {
        if (secretRaw == null || secretRaw.isBlank()) {
            throw new IllegalStateException("jwt.secret 가 비어있음 (application.yml/profiles 확인)");
        }

        // Base64의 시크릿 키를 디코딩해서 byte[]나 UTF-8로 변환
        byte[] keyBytes;
        if (looksLikeBase64(secretRaw)) {
            keyBytes = Decoders.BASE64.decode(secretRaw);
        } else {
            keyBytes = secretRaw.getBytes(StandardCharsets.UTF_8);
        }

        // HS256 최소 권장 키 길이 검증
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 길이가 너무 짧음: " + keyBytes.length + " bytes (최소 32 bytes 필요)"
            );
        }

        // HMAC-SHA용 Key 객체로 변환
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Base64 문자열 판별
    private boolean looksLikeBase64(String s) {
        // 길이 4 배수 & 허용 문자만
        int len = s.replace("\r", "").replace("\n", "").length();
        return len % 4 == 0 && BASE64_PATTERN.matcher(s).matches();
    }

    // JWT 액세스 토큰 발급
    public String createAccessToken(String userId) {  // Long → String
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId) // 문자열 user_id
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessExpMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    // 토큰 검증 및 userId 반환
    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
