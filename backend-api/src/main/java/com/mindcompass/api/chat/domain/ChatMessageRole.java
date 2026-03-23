package com.mindcompass.api.chat.domain;

// 채팅 메시지가 사용자 발화인지 AI 답변인지 구분하는 enum입니다.
public enum ChatMessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
