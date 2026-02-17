package io.mmo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JwtValidatorTest {

    private JwtValidator validator;
    private Key key;

    @BeforeEach
    void setup() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("verylongsecretkeyforjwt1234567890");
        validator = new JwtValidator(properties);
        key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validateTokenShouldReturnClaimsForValidToken() {
        String username = "player1";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        Claims claims = validator.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getExpiration().getTime()).isCloseTo(now.getTime() + 60_000, within(1000L));
    }

    @Test
    void getUsernameShouldReturnCorrectSubject() {
        String username = "player2";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        String extracted = validator.getUsername(token);
        assertThat(extracted).isEqualTo(username);
    }

    @Test
    void validateTokenShouldThrowForExpiredToken() {
        String username = "player3";
        Date now = new Date();
        Date past = new Date(now.getTime() - 1_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(past)
                           .setExpiration(past)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        assertThatThrownBy(() -> validator.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateTokenShouldThrowForInvalidSignature() {
        String username = "player4";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        Key wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(wrongKey, SignatureAlgorithm.HS256)
                           .compact();

        assertThatThrownBy(() -> validator.validateToken(token))
                .isInstanceOf(SignatureException.class);
    }
}
