package com.mindcompass.api.chat.repository;

import com.mindcompass.api.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// 세션별 메시지 저장과 최근 대화 조회를 담당하는 저장소 인터페이스입니다.
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(Long sessionId);
}
