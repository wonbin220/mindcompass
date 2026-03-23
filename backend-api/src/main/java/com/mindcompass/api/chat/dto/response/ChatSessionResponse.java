package com.mindcompass.api.chat.dto.response;

import com.mindcompass.api.chat.domain.ChatSession;
import java.time.LocalDateTime;

// 세션 생성과 목록 조회에서 재사용하는 채팅 세션 응답 DTO입니다.
public record ChatSessionResponse(
        Long sessionId,
        String title,
        Long sourceDiaryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getSourceDiary() == null ? null : session.getSourceDiary().getId(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
