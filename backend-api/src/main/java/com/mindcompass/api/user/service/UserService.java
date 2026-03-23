package com.mindcompass.api.user.service;

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.repository.UserSettingsRepository;
import com.mindcompass.api.common.exception.ResourceNotFoundException;
import com.mindcompass.api.user.dto.response.UserMeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
// 인증된 사용자의 기본 정보와 설정을 조회하는 서비스입니다.
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    public UserService(UserRepository userRepository, UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자 설정을 찾을 수 없습니다."));

        return UserMeResponse.of(user, settings);
    }
}
