package io.mmo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void validateTokenShouldReturnClaimsForValidToken() throws JwtValidationException {
        String username = "player1";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        String result = validator.getUsernameFromToken(token); // you can still test getUsernameFromToken

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(username);
    }

    @Test
    void getUsernameFromTokenShouldReturnCorrectSubject() throws JwtValidationException {
        String username = "player1";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        String extracted = validator.getUsernameFromToken(token);
        assertThat(extracted).isEqualTo(username);
    }

    @Test
    void validateTokenShouldThrowForExpiredToken() {
        String username = "player1";
        Date now = new Date();
        Date past = new Date(now.getTime() - 1_000);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(past)
                           .setExpiration(past)
                           .signWith(key, SignatureAlgorithm.HS256)
                           .compact();

        assertThatThrownBy(() -> validator.getUsernameFromToken(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateTokenShouldThrowForInvalidSignature() {
        String username = "player1";
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000);
        Key wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String token = Jwts.builder()
                           .setSubject(username)
                           .setIssuedAt(now)
                           .setExpiration(expiry)
                           .signWith(wrongKey, SignatureAlgorithm.HS256)
                           .compact();

        assertThatThrownBy(() -> validator.getUsernameFromToken(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Invalid JWT signature");
    }
}
