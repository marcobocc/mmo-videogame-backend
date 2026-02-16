package io.mmo.authentication;

import io.mmo.authentication.business.AuthService;
import io.mmo.authentication.business.exceptions.InvalidCredentialsException;
import io.mmo.authentication.business.exceptions.InvalidInputException;
import io.mmo.authentication.business.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> payload) {
        String username = payload.getOrDefault("username", "").trim();
        String password = payload.getOrDefault("password", "").trim();
        String token = authService.login(username, password);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(Map.of("jwt", token));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> payload) {
        String username = payload.getOrDefault("username", "").trim();
        String password = payload.getOrDefault("password", "").trim();
        authService.register(username, password);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(Map.of("message", "User registered successfully"));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                             .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(Map.of("error", "Username already exists"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(Map.of("error", "An unexpected error occurred"));
    }
}
