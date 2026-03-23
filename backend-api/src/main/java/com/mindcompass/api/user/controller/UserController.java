package com.mindcompass.api.user.controller;

import com.mindcompass.api.user.dto.response.UserMeResponse;
import com.mindcompass.api.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
// 로그인 사용자의 내 정보 조회 API를 제공하는 컨트롤러입니다.
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserMeResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }
}
