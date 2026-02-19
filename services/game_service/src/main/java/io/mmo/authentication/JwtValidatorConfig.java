package io.mmo.authentication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtValidatorConfig {
    private final JwtValidatorProperties jwtValidatorProperties;

    public JwtValidatorConfig(JwtValidatorProperties jwtValidatorProperties) {
        this.jwtValidatorProperties = jwtValidatorProperties;
    }

    @Bean
    public JwtValidator jwtValidator() {
        return new JwtValidator(jwtValidatorProperties.getSecret());
    }
}
