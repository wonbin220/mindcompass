package com.mindcompass.api.auth.controller;

import com.mindcompass.api.auth.dto.request.LoginRequest;
import com.mindcompass.api.auth.dto.request.RefreshTokenRequest;
import com.mindcompass.api.auth.dto.request.SignUpRequest;
import com.mindcompass.api.auth.dto.response.LoginResponse;
import com.mindcompass.api.auth.dto.response.SignUpResponse;
import com.mindcompass.api.auth.dto.response.TokenRefreshResponse;
import com.mindcompass.api.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
// 회원가입, 로그인, 토큰 재발급 같은 인증 공개 API를 받는 컨트롤러입니다.
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signup(@Valid @RequestBody SignUpRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenRefreshResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }
}
