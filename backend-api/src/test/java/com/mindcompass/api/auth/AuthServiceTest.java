package com.mindcompass.api.auth;

import com.mindcompass.api.auth.domain.RefreshToken;
import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.dto.request.LoginRequest;
import com.mindcompass.api.auth.dto.request.RefreshTokenRequest;
import com.mindcompass.api.auth.dto.request.SignUpRequest;
import com.mindcompass.api.auth.dto.response.LoginResponse;
import com.mindcompass.api.auth.dto.response.SignUpResponse;
import com.mindcompass.api.auth.dto.response.TokenRefreshResponse;
import com.mindcompass.api.auth.repository.RefreshTokenRepository;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.repository.UserSettingsRepository;
import com.mindcompass.api.auth.security.JwtTokenProvider;
import com.mindcompass.api.auth.service.AuthService;
import com.mindcompass.api.common.exception.DuplicateEmailException;
import com.mindcompass.api.common.exception.InvalidCredentialsException;
import com.mindcompass.api.common.exception.InvalidRefreshTokenException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupCreatesUserAndDefaultSettings() throws Exception {
        SignUpRequest request = new SignUpRequest("냥집사", "user@example.com", "Abcd1234!", true, true);
        User savedUser = User.create("user@example.com", "encoded-password", "냥집사");
        setField(savedUser, "id", 1L);
        setField(savedUser, "createdAt", LocalDateTime.of(2026, 3, 18, 10, 0));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        SignUpResponse response = authService.signup(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("냥집사");
        verify(userSettingsRepository).save(any());
    }

    @Test
    void signupRejectsDuplicateEmail() {
        SignUpRequest request = new SignUpRequest("냥집사", "user@example.com", "Abcd1234!", true, true);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(User.create("user@example.com", "encoded-password", "existing")));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokensAndStoresRefreshTokenHash() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Abcd1234!");
        User user = User.create("user@example.com", "encoded-password", "냥집사");
        setField(user, "id", 7L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Abcd1234!", "encoded-password")).thenReturn(true);
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(7L)).thenReturn(List.of());
        when(jwtTokenProvider.generateAccessToken(user))
                .thenReturn(new JwtTokenProvider.GeneratedToken(
                        "access-token",
                        LocalDateTime.of(2026, 3, 18, 11, 0)
                ));
        when(jwtTokenProvider.generateRefreshToken(user))
                .thenReturn(new JwtTokenProvider.GeneratedToken(
                        "refresh-token",
                        LocalDateTime.of(2026, 4, 1, 10, 0)
                ));

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().userId()).isEqualTo(7L);
        assertThat(response.user().nickname()).isEqualTo("냥집사");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash()).isNotBlank();
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = User.create("user@example.com", "encoded-password", "냥집사");
        setField(user, "id", 7L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void refreshRotatesRefreshTokenAndReturnsNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("stored-refresh-token");
        User user = User.create("user@example.com", "encoded-password", "냥집사");
        setField(user, "id", 9L);

        RefreshToken storedToken = RefreshToken.create(user, "hashed-old-token", LocalDateTime.now().plusDays(7));

        when(jwtTokenProvider.validateToken("stored-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("stored-refresh-token")).thenReturn(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserIdAndTokenHashAndRevokedAtIsNull(9L, sha256("stored-refresh-token")))
                .thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken(user))
                .thenReturn(new JwtTokenProvider.GeneratedToken("new-access-token", LocalDateTime.now().plusHours(1)));
        when(jwtTokenProvider.generateRefreshToken(user))
                .thenReturn(new JwtTokenProvider.GeneratedToken("new-refresh-token", LocalDateTime.now().plusDays(14)));

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshRejectsInvalidStoredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("stored-refresh-token");

        when(jwtTokenProvider.validateToken("stored-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("stored-refresh-token")).thenReturn(9L);
        when(userRepository.findById(9L))
                .thenReturn(Optional.of(User.create("user@example.com", "encoded-password", "냥집사")));
        when(refreshTokenRepository.findByUserIdAndTokenHashAndRevokedAtIsNull(9L, sha256("stored-refresh-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void signupSavesDefaultSettings() throws Exception {
        SignUpRequest request = new SignUpRequest("감정러", "settings@example.com", "Abcd1234!", true, true);
        User savedUser = User.create("settings@example.com", "encoded-password", "감정러");
        setField(savedUser, "id", 3L);
        setField(savedUser, "createdAt", LocalDateTime.of(2026, 3, 18, 10, 0));

        when(userRepository.findByEmail("settings@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.signup(request);

        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(settingsCaptor.capture());
        assertThat(settingsCaptor.getValue().getUser()).isEqualTo(savedUser);
    }

    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
