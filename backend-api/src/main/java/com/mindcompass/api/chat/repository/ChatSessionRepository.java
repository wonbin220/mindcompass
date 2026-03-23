package com.mindcompass.api.chat.repository;

import com.mindcompass.api.chat.domain.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 상담 세션을 저장하고 사용자 기준으로 조회하는 저장소 인터페이스입니다.
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSession> findByIdAndUserId(Long sessionId, Long userId);
}
