package com.mindcompass.api.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 사용자별 앱 설정과 응답 모드를 저장하는 엔티티입니다.
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "app_lock_enabled", nullable = false)
    private boolean appLockEnabled;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "daily_reminder_time")
    private LocalTime dailyReminderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_mode", nullable = false, length = 30)
    private ResponseMode responseMode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private UserSettings(
            User user,
            boolean appLockEnabled,
            boolean notificationEnabled,
            LocalTime dailyReminderTime,
            ResponseMode responseMode
    ) {
        this.user = user;
        this.appLockEnabled = appLockEnabled;
        this.notificationEnabled = notificationEnabled;
        this.dailyReminderTime = dailyReminderTime;
        this.responseMode = responseMode;
    }

    public static UserSettings createDefault(User user) {
        return new UserSettings(user, false, true, LocalTime.of(21, 0), ResponseMode.EMPATHETIC);
    }
}
