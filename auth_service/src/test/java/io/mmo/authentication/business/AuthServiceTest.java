package io.mmo.authentication.business;

import io.mmo.authentication.business.exceptions.InvalidCredentialsException;
import io.mmo.authentication.business.exceptions.InvalidInputException;
import io.mmo.authentication.business.exceptions.UserAlreadyExistsException;
import io.mmo.authentication.database.UserCredentials;
import io.mmo.authentication.database.UserCredentialsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthService subject;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private UserCredentialsRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository = mock(UserCredentialsRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        subject = new AuthService(jwtService, passwordEncoder, userRepository);
    }

    @Test
    void testLoginSuccess() {
        String username = "player1";
        String password = "secret";
        UserCredentials user = new UserCredentials();
        user.setUsername(username);
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed")).thenReturn(true);
        when(jwtService.generateToken(username)).thenReturn("jwt-token");

        String token = subject.login(username, password);

        assertThat(token).isEqualTo("jwt-token");
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, "hashed");
        verify(jwtService).generateToken(username);
    }

    @Test
    void testLoginInvalidCredentials() {
        String username = "player2";
        String password = "wrong";
        UserCredentials user = new UserCredentials();
        user.setUsername(username);
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed")).thenReturn(false);

        assertThatThrownBy(() -> subject.login(username, password))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, "hashed");
        verifyNoInteractions(jwtService);
    }

    @Test
    void testLoginUserCredentialsNotFound() {
        String username = "nonexistent";
        String password = "any";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subject.login(username, password))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByUsername(username);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testLoginBlankUsernameOrPassword() {
        assertThatThrownBy(() -> subject.login("", "password"))
                .isInstanceOf(InvalidInputException.class);

        assertThatThrownBy(() -> subject.login("username", ""))
                .isInstanceOf(InvalidInputException.class);

        assertThatThrownBy(() -> subject.login(" ", " "))
                .isInstanceOf(InvalidInputException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testRegisterSuccess() {
        String username = "player1";
        String password = "secret";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashed-password");

        subject.register(username, password);

        verify(userRepository).existsByUsername(username);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals(username) &&
                        user.getPasswordHash().equals("hashed-password")
        ));
    }

    @Test
    void testRegisterUserAlreadyExists() {
        String username = "player1";
        String password = "secret";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> subject.register(username, password))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository).existsByUsername(username);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterInvalidInput() {
        assertThatThrownBy(() -> subject.register("", "password"))
                .isInstanceOf(InvalidInputException.class);

        assertThatThrownBy(() -> subject.register("username", ""))
                .isInstanceOf(InvalidInputException.class);

        assertThatThrownBy(() -> subject.register(" ", " "))
                .isInstanceOf(InvalidInputException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }
}
