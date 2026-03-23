package com.mindcompass.api.chat.controller;

// 상담 세션 생성, 세션 조회, 메시지 전송 API를 받는 컨트롤러다.

import com.mindcompass.api.chat.dto.request.CreateChatSessionRequest;
import com.mindcompass.api.chat.dto.request.SendChatMessageRequest;
import com.mindcompass.api.chat.dto.response.ChatDetailResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionListResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.dto.response.SendChatMessageResponse;
import com.mindcompass.api.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/sessions")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "채팅 세션 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionResponse createSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateChatSessionRequest request
    ) {
        return chatService.createSession(userId, request);
    }

    @Operation(summary = "채팅 세션 목록 조회")
    @GetMapping
    public ChatSessionListResponse getSessions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return chatService.getSessions(userId);
    }

    @Operation(summary = "채팅 세션 상세 조회")
    @GetMapping("/{sessionId}")
    public ChatDetailResponse getSessionDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "채팅 세션 ID", example = "1")
            @PathVariable Long sessionId
    ) {
        return chatService.getSessionDetail(userId, sessionId);
    }

    @Operation(summary = "채팅 메시지 전송")
    @PostMapping("/{sessionId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SendChatMessageResponse sendMessage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "채팅 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @Valid @RequestBody SendChatMessageRequest request
    ) {
        return chatService.sendMessage(userId, sessionId, request);
    }
}
