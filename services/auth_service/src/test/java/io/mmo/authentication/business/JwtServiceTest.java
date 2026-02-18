package io.mmo.authentication.business;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService subject;
    private Key key;

    @BeforeEach
    void setup() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("verylongsecretkeyforjwt1234567890");
        properties.setExpiration(60_000);
        subject = new JwtService(properties);
        key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testGenerateTokenNotNull() {
        String username = "player1";
        String token = subject.generateToken(username);
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void testGeneratedTokenContainsUsername() {
        String username = "player2";
        String token = subject.generateToken(username);
        Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
        assertThat(claims.getSubject()).isEqualTo(username);
    }

    @Test
    void testGeneratedTokenHasExpiration() {
        String username = "player3";
        String token = subject.generateToken(username);
        Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
        Date now = new Date();
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration().getTime()).isLessThanOrEqualTo(now.getTime() + 60_100);
    }
}
