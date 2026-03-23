package com.mindcompass.api.chat.dto.response;

import java.util.List;

// 사용자의 채팅 세션 목록을 내려주는 응답 DTO입니다.
public record ChatSessionListResponse(
        int count,
        List<ChatSessionResponse> sessions
) {

    public static ChatSessionListResponse of(List<ChatSessionResponse> sessions) {
        return new ChatSessionListResponse(sessions.size(), sessions);
    }
}
