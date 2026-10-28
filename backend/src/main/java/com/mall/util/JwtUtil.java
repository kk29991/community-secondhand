package com.mall.util;

import com.mall.common.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析工具 (jjwt 0.12.x)
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_MERCHANT_ID = "merchantId";

    private final SecretKey secretKey;
    private final long expireMillis;

    public JwtUtil(@Value("${mall.jwt.secret}") String secret,
                   @Value("${mall.jwt.expire-hours:168}") long expireHours) {
        // HS256 要求密钥至少 256 bit, 配置中的 secret 长度需 >= 32 字节
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60 * 60 * 1000;
    }

    /**
     * 生成 token
     */
    public String createToken(Long id, String username, String role, Long merchantId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(id))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis));
        if (merchantId != null) {
            builder.claim(CLAIM_MERCHANT_ID, merchantId);
        }
        return builder.signWith(secretKey).compact();
    }

    /**
     * 解析 token, 非法或过期抛异常
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number merchantId = claims.get(CLAIM_MERCHANT_ID, Number.class);
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE, String.class),
                merchantId == null ? null : merchantId.longValue()
        );
    }

    /** token 有效期(秒), 供登录响应返回给前端 */
    public long getExpireSeconds() {
        return expireMillis / 1000;
    }
}
