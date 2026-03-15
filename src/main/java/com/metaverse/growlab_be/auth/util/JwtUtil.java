package com.metaverse.growlab_be.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {
    // @Value 애너테이션을 통해 application.properties의 환경 변수 로드
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expriation")
    private Long expirationTime;

    // SecretKey를 Base64로 인코딩하여 저장
    private Key key;

    @PostConstruct
    public void init() {
        // BASE64 인코딩된 secretKey를 바이트배열로 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        // 디코딩된 secretKey로 HMAC-SHA 알고리즘으로 키 생성
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // [[JWT 토큰 생성 메서드]]
    public String generateToken(String username){
        return Jwts.builder()
                .subject(username) // Claim에 "subject" 설정
                .issuedAt(new Date(System.currentTimeMillis())) // 생성시간
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // 만료 시간
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("만료된 JWT 토큰입니다.", e);
        } catch ( SecurityException | MalformedJwtException
                    | UnsupportedJwtException | IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않는 JWT 토큰 입니다.", e);
        }
    }

    // JWT 토큰의 특정 Claim(데이터) 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // JWT 토큰에서 사용자계정 (Username) 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // [[JWT 토큰의 유효성 검사]]
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // JWT 토큰에서 토큰 만료시간 추출
    public Date getExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // JWT 토큰이 만료 여부 확인
    private boolean isTokenExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }

}
