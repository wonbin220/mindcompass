package com.mindcompass.api.auth;

// Auth 공개 API의 HTTP 계약과 검증 실패 응답을 확인하는 WebMvc 테스트다.

import com.mindcompass.api.auth.controller.AuthController;
import com.mindcompass.api.auth.dto.response.LoginResponse;
import com.mindcompass.api.auth.dto.response.SignUpResponse;
import com.mindcompass.api.auth.dto.response.TokenRefreshResponse;
import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.auth.service.AuthService;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void signupReturnsCreatedResponse() throws Exception {
        when(authService.signup(any()))
                .thenReturn(new SignUpResponse(
                        1L,
                        "user@example.com",
                        "tester",
                        LocalDateTime.of(2026, 3, 24, 12, 0)
                ));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "tester",
                                  "email": "user@example.com",
                                  "password": "password123!",
                                  "personalInfoConsentAgreed": true,
                                  "serviceTermsAgreed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("tester"));
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponse(
                        "access-token",
                        LocalDateTime.of(2026, 3, 24, 13, 0),
                        "refresh-token",
                        LocalDateTime.of(2026, 4, 7, 13, 0),
                        new LoginResponse.UserSummary(7L, "tester")
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.userId").value(7))
                .andExpect(jsonPath("$.user.nickname").value("tester"));
    }

    @Test
    void refreshReturnsNewTokens() throws Exception {
        when(authService.refresh(any()))
                .thenReturn(new TokenRefreshResponse(
                        "new-access-token",
                        LocalDateTime.of(2026, 3, 24, 14, 0),
                        "new-refresh-token",
                        LocalDateTime.of(2026, 4, 7, 14, 0)
                ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "stored-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void signupReturnsBadRequestWhenRequiredAgreementMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "tester",
                                  "email": "user@example.com",
                                  "password": "password123!",
                                  "personalInfoConsentAgreed": false,
                                  "serviceTermsAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
