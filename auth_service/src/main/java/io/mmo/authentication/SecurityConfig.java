package io.mmo.authentication;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                   .sessionManagement(session -> session
                           .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                   )
                   .exceptionHandling(exceptions -> exceptions
                           .authenticationEntryPoint((request, response, authException) ->
                                   response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                           .accessDeniedHandler((request, response, accessDeniedException) ->
                                   response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                   )
                   .authorizeHttpRequests(auth -> auth
                           .requestMatchers("/auth/*").permitAll()
                           .anyRequest().authenticated()
                   ).build();
    }
}
