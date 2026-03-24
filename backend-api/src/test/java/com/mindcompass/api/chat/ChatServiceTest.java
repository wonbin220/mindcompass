package com.mindcompass.api.chat;

// ChatService의 위험도 분기와 일반 응답 흐름을 검증하는 테스트다.

import com.mindcompass.api.auth.domain.ResponseMode;
import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.repository.UserSettingsRepository;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatMessageRole;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.chat.dto.request.SendChatMessageRequest;
import com.mindcompass.api.chat.dto.response.SendChatMessageResponse;
import com.mindcompass.api.chat.repository.ChatMessageRepository;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.chat.service.ChatService;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.repository.DiaryRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private AiSafetyClient aiSafetyClient;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AppMetricsRecorder appMetricsRecorder;

    @InjectMocks
    private ChatService chatService;

    @Test
    void sendMessageReturnsSafetyWhenRiskIsHigh() throws Exception {
        User user = createUser(1L);
        ChatSession session = createSession(10L, user);
        ChatMessage savedUserMessage = createMessage(100L, session, ChatMessageRole.USER, "다 끝내고 싶고 사라지고 싶어요.");
        ChatMessage savedAssistantMessage = createMessage(101L, session, ChatMessageRole.ASSISTANT, "safety");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(aiSafetyClient.scoreRisk(any())).thenReturn(
                new AiSafetyClient.RiskScoreResponse(
                        "HIGH",
                        BigDecimal.valueOf(0.95),
                        List.of("SELF_HARM_IMPLICIT"),
                        "SAFETY_RESPONSE"
                )
        );
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(savedUserMessage, savedAssistantMessage);

        SendChatMessageResponse response = chatService.sendMessage(1L, 10L, new SendChatMessageRequest("다 끝내고 싶고 사라지고 싶어요."));

        assertThat(response.responseType()).isEqualTo("SAFETY");
        assertThat(response.assistantReply()).contains("지금 많이 힘든 상태");
        verify(aiChatClient, never()).generateReply(any());
    }

    @Test
    void sendMessageReturnsSupportiveWhenRiskIsMedium() throws Exception {
        User user = createUser(1L);
        ChatSession session = createSession(10L, user);
        ChatMessage savedUserMessage = createMessage(100L, session, ChatMessageRole.USER, "아무도 없고 너무 힘들어서 버티기 힘들어요.");
        ChatMessage savedAssistantMessage = createMessage(101L, session, ChatMessageRole.ASSISTANT, "supportive");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(aiSafetyClient.scoreRisk(any())).thenReturn(
                new AiSafetyClient.RiskScoreResponse(
                        "MEDIUM",
                        BigDecimal.valueOf(0.65),
                        List.of("DISTRESS_ESCALATION", "ISOLATION"),
                        "SUPPORTIVE_RESPONSE"
                )
        );
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(savedUserMessage, savedAssistantMessage);

        SendChatMessageResponse response = chatService.sendMessage(1L, 10L, new SendChatMessageRequest("아무도 없고 너무 힘들어서 버티기 힘들어요."));

        assertThat(response.responseType()).isEqualTo("SUPPORTIVE");
        assertThat(response.assistantReply()).contains("지금 많이 지치고 버거운 상태");
        verify(aiChatClient, never()).generateReply(any());
    }

    @Test
    void sendMessageUsesGenerateReplyWhenRiskIsLow() throws Exception {
        User user = createUser(1L);
        ChatSession session = createSession(10L, user);
        ChatMessage historyMessage = createMessage(90L, session, ChatMessageRole.USER, "지난 메시지");
        ChatMessage savedUserMessage = createMessage(100L, session, ChatMessageRole.USER, "오늘은 좀 불안했지만 그래도 버틸 수 있었어요.");
        ChatMessage savedAssistantMessage = createMessage(101L, session, ChatMessageRole.ASSISTANT, "괜찮아요");
        UserSettings settings = UserSettings.createDefault(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(aiSafetyClient.scoreRisk(any())).thenReturn(
                new AiSafetyClient.RiskScoreResponse(
                        "LOW",
                        BigDecimal.valueOf(0.15),
                        List.of(),
                        "NORMAL_RESPONSE"
                )
        );
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));
        when(chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(historyMessage));
        when(aiChatClient.generateReply(any())).thenReturn(
                new AiChatClient.GenerateReplyResponse(
                        "괜찮아요. 오늘을 버틴 점을 먼저 인정해볼까요?",
                        BigDecimal.valueOf(0.70),
                        "NORMAL"
                )
        );
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(savedUserMessage, savedAssistantMessage);

        SendChatMessageResponse response = chatService.sendMessage(1L, 10L, new SendChatMessageRequest("오늘은 좀 불안했지만 그래도 버틸 수 있었어요."));

        assertThat(response.responseType()).isEqualTo("NORMAL");
        assertThat(response.assistantReply()).contains("오늘을 버틴");

        ArgumentCaptor<AiChatClient.GenerateReplyRequest> requestCaptor =
                ArgumentCaptor.forClass(AiChatClient.GenerateReplyRequest.class);
        verify(aiChatClient).generateReply(requestCaptor.capture());
        assertThat(requestCaptor.getValue().mode()).isEqualTo(ResponseMode.EMPATHETIC);
    }

    @Test
    void sendMessageReturnsFallbackWhenAiOrchestrationFails() throws Exception {
        User user = createUser(1L);
        ChatSession session = createSession(10L, user);
        ChatMessage savedUserMessage = createMessage(100L, session, ChatMessageRole.USER, "오늘은 너무 벅차요.");
        ChatMessage savedAssistantMessage = createMessage(101L, session, ChatMessageRole.ASSISTANT, "fallback");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(chatSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(aiSafetyClient.scoreRisk(any())).thenThrow(new RuntimeException("risk-score timeout"));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(savedUserMessage, savedAssistantMessage);

        SendChatMessageResponse response = chatService.sendMessage(1L, 10L, new SendChatMessageRequest("오늘은 너무 벅차요."));

        assertThat(response.responseType()).isEqualTo("FALLBACK");
        assertThat(response.assistantReply()).isNotBlank();
        verify(appMetricsRecorder).incrementChatAiFailure();
        verify(aiChatClient, never()).generateReply(any());
    }

    private User createUser(Long id) throws Exception {
        User user = User.create("chat@example.com", "encoded-password", "tester");
        setField(user, "id", id);
        return user;
    }

    private ChatSession createSession(Long id, User user) throws Exception {
        ChatSession session = ChatSession.create(user, null, "테스트 세션");
        setField(session, "id", id);
        return session;
    }

    private ChatMessage createMessage(Long id, ChatSession session, ChatMessageRole role, String content) throws Exception {
        ChatMessage message = ChatMessage.create(session, role, content);
        setField(message, "id", id);
        setField(message, "createdAt", LocalDateTime.of(2026, 3, 22, 10, 0).plusMinutes(id % 10));
        return message;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
