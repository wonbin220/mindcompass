package com.mindcompass.api.auth.repository;

import com.mindcompass.api.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 사용자 엔티티를 저장하고 이메일 기준으로 조회하는 저장소 인터페이스입니다.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
