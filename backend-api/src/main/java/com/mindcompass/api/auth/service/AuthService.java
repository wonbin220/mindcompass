package com.mindcompass.api.auth.service;

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
import com.mindcompass.api.common.exception.DuplicateEmailException;
import com.mindcompass.api.common.exception.InvalidCredentialsException;
import com.mindcompass.api.common.exception.InvalidRefreshTokenException;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
// 사용자 가입, 로그인, refresh token 회전을 처리하는 인증 서비스입니다.
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public SignUpResponse signup(SignUpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        userRepository.findByEmail(normalizedEmail)
                .ifPresent(existingUser -> {
                    throw new DuplicateEmailException("이미 가입된 이메일입니다.");
                });

        User user = User.create(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.nickname().trim()
        );
        User savedUser = userRepository.save(user);
        userSettingsRepository.save(UserSettings.createDefault(savedUser));

        return SignUpResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        revokeActiveRefreshTokens(user.getId());

        JwtTokenProvider.GeneratedToken accessToken = jwtTokenProvider.generateAccessToken(user);
        JwtTokenProvider.GeneratedToken refreshToken = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenRepository.save(
                RefreshToken.create(
                        user,
                        hashToken(refreshToken.token()),
                        refreshToken.expiresAt()
                )
        );

        return LoginResponse.of(user, accessToken, refreshToken);
    }

    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        String refreshTokenValue = request.refreshToken().trim();

        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
        }

        Long userId = jwtTokenProvider.extractUserId(refreshTokenValue);
        String hashedToken = hashToken(refreshTokenValue);

        User user = userRepository.findById(userId)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new InvalidRefreshTokenException("유효하지 않은 refresh token입니다."));

        RefreshToken storedToken = refreshTokenRepository
                .findByUserIdAndTokenHashAndRevokedAtIsNull(userId, hashedToken)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException("유효하지 않은 refresh token입니다."));

        storedToken.revoke(LocalDateTime.now());

        JwtTokenProvider.GeneratedToken newAccessToken = jwtTokenProvider.generateAccessToken(user);
        JwtTokenProvider.GeneratedToken newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenRepository.save(
                RefreshToken.create(
                        user,
                        hashToken(newRefreshToken.token()),
                        newRefreshToken.expiresAt()
                )
        );

        return new TokenRefreshResponse(
                newAccessToken.token(),
                newAccessToken.expiresAt(),
                newRefreshToken.token(),
                newRefreshToken.expiresAt()
        );
    }

    private void revokeActiveRefreshTokens(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime revokedAt = LocalDateTime.now();
        activeTokens.forEach(token -> token.revoke(revokedAt));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
