package io.mmo.authentication.business;

import io.mmo.authentication.business.exceptions.InvalidCredentialsException;
import io.mmo.authentication.business.exceptions.InvalidInputException;
import io.mmo.authentication.business.exceptions.UserAlreadyExistsException;
import io.mmo.authentication.database.UserCredentials;
import io.mmo.authentication.database.UserCredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialsRepository userCredentialsRepository;

    public String login(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            throw new InvalidInputException("Username and password are required");
        }

        userCredentialsRepository.findByUsername(username)
                                 .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                                 .orElseThrow(InvalidCredentialsException::new);

        return jwtService.generateToken(username);
    }

    public void register(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            throw new InvalidInputException("Username and password are required");
        }

        if (userCredentialsRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException();
        }

        var user = UserCredentials.builder()
                                  .username(username)
                                  .passwordHash(passwordEncoder.encode(password))
                                  .build();

        userCredentialsRepository.save(user);
    }
}
