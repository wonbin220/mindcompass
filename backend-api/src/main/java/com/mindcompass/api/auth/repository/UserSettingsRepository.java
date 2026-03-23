package com.mindcompass.api.auth.repository;

import com.mindcompass.api.auth.domain.UserSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 사용자 설정 정보를 조회하고 저장하는 저장소 인터페이스입니다.
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserId(Long userId);
}
