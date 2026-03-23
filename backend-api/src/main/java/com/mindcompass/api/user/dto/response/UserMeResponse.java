package com.mindcompass.api.user.dto.response;

import com.mindcompass.api.auth.domain.ResponseMode;
import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.domain.UserStatus;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 내 정보 화면에 필요한 사용자 정보와 설정을 담는 응답 DTO입니다.
public record UserMeResponse(
        Long userId,
        String email,
        String nickname,
        UserStatus status,
        LocalDateTime createdAt,
        Settings settings
) {

    public static UserMeResponse of(User user, UserSettings settings) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getCreatedAt(),
                new Settings(
                        settings.isAppLockEnabled(),
                        settings.isNotificationEnabled(),
                        settings.getDailyReminderTime(),
                        settings.getResponseMode()
                )
        );
    }

    // 사용자 설정 영역을 묶어 내려주는 내부 응답 DTO입니다.
    public record Settings(
            boolean appLockEnabled,
            boolean notificationEnabled,
            LocalTime dailyReminderTime,
            ResponseMode responseMode
    ) {
    }
}
