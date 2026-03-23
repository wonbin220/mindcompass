package com.mindcompass.api.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// 특정 채팅 세션의 메타정보와 메시지 목록을 함께 내려주는 응답 DTO입니다.
public record ChatDetailResponse(
        Long sessionId,
        String title,
        Long sourceDiaryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ChatMessageResponse> messages
) {
}
