package io.mmo.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mmo.authentication.business.AuthService;
import io.mmo.authentication.business.exceptions.InvalidCredentialsException;
import io.mmo.authentication.business.exceptions.InvalidInputException;
import io.mmo.authentication.business.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private AuthController authController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                                 .setControllerAdvice(authController) // use controller itself for exception handlers
                                 .build();
    }

    @Test
    void testLoginSuccess() throws Exception {
        String username = "player1";
        String password = "secret";
        String token = "jwt-token";

        when(authService.login(username, password)).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", username,
                               "password", password
                       ))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.jwt").value(token));

        verify(authService).login(username, password);
    }

    @Test
    void testLoginMissingUsername() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new InvalidInputException("Invalid input"));

        mockMvc.perform(post("/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "password", "secret"
                       ))))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testLoginMissingPassword() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new InvalidInputException("Invalid input"));

        mockMvc.perform(post("/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", "player1"
                       ))))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        String username = "player1";
        String password = "secret";

        when(authService.login(username, password))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", username,
                               "password", password
                       ))))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error").exists());

        verify(authService).login(username, password);
    }

    @Test
    void testRegisterSuccess() throws Exception {
        String username = "player1";
        String password = "secret";

        doNothing().when(authService).register(username, password);

        mockMvc.perform(post("/auth/register")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", username,
                               "password", password
                       ))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(authService).register(username, password);
    }

    @Test
    void testRegisterUserAlreadyExists() throws Exception {
        String username = "player1";
        String password = "secret";

        doThrow(new UserAlreadyExistsException()).when(authService).register(username, password);

        mockMvc.perform(post("/auth/register")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", username,
                               "password", password
                       ))))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.error").value("Username already exists"));

        verify(authService).register(username, password);
    }

    @Test
    void testRegisterInvalidInput() throws Exception {
        String username = "player1";
        String password = "secret";

        doThrow(new InvalidInputException("Invalid input")).when(authService).register(username, password);

        mockMvc.perform(post("/auth/register")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(Map.of(
                               "username", username,
                               "password", password
                       ))))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(jsonPath("$.error").value("Invalid input"));

        verify(authService).register(username, password);
    }
}
