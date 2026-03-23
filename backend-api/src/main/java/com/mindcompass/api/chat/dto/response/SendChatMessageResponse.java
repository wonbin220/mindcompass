package com.mindcompass.api.chat.dto.response;

// 메시지 전송 후 사용자 메시지와 AI 답변 정보를 함께 내려주는 응답 DTO입니다.
public record SendChatMessageResponse(
        Long sessionId,
        Long userMessageId,
        Long assistantMessageId,
        String assistantReply,
        String responseType
) {
}
