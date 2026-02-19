package io.mmo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtValidator.class);
    private final Key key;

    public JwtValidator(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) throws JwtValidationException {
        return validateToken(token).getSubject();
    }

    private Claims validateToken(String token) throws JwtValidationException {
        try {
            Claims claims = Jwts.parserBuilder()
                                .setSigningKey(key)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();

            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                throw new JwtValidationException("JWT token does not contain a subject");
            }

            return claims;

        } catch (SignatureException e) {
            LOGGER.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtValidationException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            LOGGER.error("Invalid JWT token: {}", e.getMessage());
            throw new JwtValidationException("Malformed JWT token", e);
        } catch (ExpiredJwtException e) {
            LOGGER.error("JWT token is expired: {}", e.getMessage());
            throw new JwtValidationException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.error("JWT token is unsupported: {}", e.getMessage());
            throw new JwtValidationException("Unsupported JWT token", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("JWT claims string is empty: {}", e.getMessage());
            throw new JwtValidationException("JWT claims string is empty or missing subject", e);
        }
    }
}
