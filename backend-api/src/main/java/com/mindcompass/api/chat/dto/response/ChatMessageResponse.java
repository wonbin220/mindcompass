package com.mindcompass.api.chat.dto.response;

import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatMessageRole;
import java.time.LocalDateTime;

// 채팅방 상세 화면에서 메시지 한 건을 표현하는 응답 DTO입니다.
public record ChatMessageResponse(
        Long messageId,
        ChatMessageRole role,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
