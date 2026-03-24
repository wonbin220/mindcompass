package com.mindcompass.api.chat;

// ChatController의 세션/메시지 HTTP 계약을 검증하는 WebMvc 테스트다.

import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.chat.controller.ChatController;
import com.mindcompass.api.chat.domain.ChatMessageRole;
import com.mindcompass.api.chat.dto.response.ChatDetailResponse;
import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.dto.response.SendChatMessageResponse;
import com.mindcompass.api.chat.service.ChatService;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createSessionReturnsCreatedResponse() throws Exception {
        when(chatService.createSession(isNull(), any()))
                .thenReturn(new ChatSessionResponse(
                        1L,
                        "오늘 감정 상담",
                        null,
                        LocalDateTime.of(2026, 3, 22, 16, 0),
                        LocalDateTime.of(2026, 3, 22, 16, 0)
                ));

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "오늘 감정 상담"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.title").value("오늘 감정 상담"));
    }

    @Test
    void getSessionDetailReturnsMessages() throws Exception {
        when(chatService.getSessionDetail(isNull(), eq(1L)))
                .thenReturn(new ChatDetailResponse(
                        1L,
                        "오늘 감정 상담",
                        null,
                        LocalDateTime.of(2026, 3, 22, 16, 0),
                        LocalDateTime.of(2026, 3, 22, 16, 5),
                        List.of(
                                new ChatMessageResponse(10L, ChatMessageRole.USER, "오늘 너무 불안했어요.", LocalDateTime.of(2026, 3, 22, 16, 1)),
                                new ChatMessageResponse(11L, ChatMessageRole.ASSISTANT, "많이 힘드셨겠어요.", LocalDateTime.of(2026, 3, 22, 16, 2))
                        )
                ));

        mockMvc.perform(get("/api/v1/chat/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));
    }

    @Test
    void sendMessageReturnsSupportiveResponse() throws Exception {
        when(chatService.sendMessage(isNull(), eq(1L), any()))
                .thenReturn(new SendChatMessageResponse(
                        1L,
                        100L,
                        101L,
                        "지금 많이 지치고 버거운 상태로 느껴져요.",
                        "SUPPORTIVE"
                ));

        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "아무도 없고 너무 힘들어서 버티기 힘들어요."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.responseType").value("SUPPORTIVE"))
                .andExpect(jsonPath("$.assistantReply").value("지금 많이 지치고 버거운 상태로 느껴져요."));
    }

    @Test
    void sendMessageReturnsFallbackResponse() throws Exception {
        when(chatService.sendMessage(isNull(), eq(1L), any()))
                .thenReturn(new SendChatMessageResponse(
                        1L,
                        100L,
                        101L,
                        "지금은 답변을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.",
                        "FALLBACK"
                ));

        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "오늘은 너무 벅차요."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.responseType").value("FALLBACK"))
                .andExpect(jsonPath("$.assistantReply").value("지금은 답변을 준비하지 못했어요. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void sendMessageReturnsBadRequestWhenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
