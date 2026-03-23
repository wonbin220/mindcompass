package com.mindcompass.api.auth.repository;

import com.mindcompass.api.auth.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// refresh token 저장과 활성 토큰 조회를 담당하는 저장소 인터페이스입니다.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    Optional<RefreshToken> findByUserIdAndTokenHashAndRevokedAtIsNull(Long userId, String tokenHash);
}
