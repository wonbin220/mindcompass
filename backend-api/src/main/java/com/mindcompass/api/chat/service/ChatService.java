package com.mindcompass.api.chat.service;

// 채팅 세션과 메시지 저장, 안전 분기, AI 응답 흐름을 조율하는 서비스다.

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.repository.UserSettingsRepository;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatMessageRole;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.chat.dto.request.CreateChatSessionRequest;
import com.mindcompass.api.chat.dto.request.SendChatMessageRequest;
import com.mindcompass.api.chat.dto.response.ChatDetailResponse;
import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionListResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.dto.response.SendChatMessageResponse;
import com.mindcompass.api.chat.repository.ChatMessageRepository;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.common.exception.ResourceNotFoundException;
import com.mindcompass.api.common.logging.RequestTraceContext;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.repository.DiaryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String AI_FALLBACK_REPLY =
            "지금은 답변을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.";
    private static final String SAFETY_REPLY =
            "지금 많이 힘든 상태로 느껴져요. 혼자 버티지 말고 가까운 사람이나 정신건강복지센터, 자살예방상담 109 같은 즉시 도움을 요청해 주세요.";
    private static final String SUPPORTIVE_REPLY =
            "지금 많이 지치고 버거운 상태로 느껴져요. 오늘을 버티게 한 작은 장면 하나를 천천히 떠올려 보고, 혼자 감당하기 어렵다면 믿을 수 있는 사람에게 도움을 요청해 보세요.";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiaryRepository diaryRepository;
    private final AiSafetyClient aiSafetyClient;
    private final AiChatClient aiChatClient;
    private final AppMetricsRecorder appMetricsRecorder;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            DiaryRepository diaryRepository,
            AiSafetyClient aiSafetyClient,
            AiChatClient aiChatClient,
            AppMetricsRecorder appMetricsRecorder
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.diaryRepository = diaryRepository;
        this.aiSafetyClient = aiSafetyClient;
        this.aiChatClient = aiChatClient;
        this.appMetricsRecorder = appMetricsRecorder;
    }

    public ChatSessionResponse createSession(Long userId, CreateChatSessionRequest request) {
        User user = getActiveUser(userId);
        Diary sourceDiary = request.sourceDiaryId() == null
                ? null
                : diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(request.sourceDiaryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("연결할 일기를 찾을 수 없습니다."));

        ChatSession session = chatSessionRepository.save(
                ChatSession.create(user, sourceDiary, request.title().trim())
        );

        log.info(
                "Chat session created. requestId={}, sessionId={}, userId={}, sourceDiaryId={}",
                RequestTraceContext.currentRequestId(),
                session.getId(),
                userId,
                sourceDiary == null ? null : sourceDiary.getId()
        );

        return ChatSessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public ChatSessionListResponse getSessions(Long userId) {
        getActiveUser(userId);
        List<ChatSessionResponse> sessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(ChatSessionResponse::from)
                .toList();
        return ChatSessionListResponse.of(sessions);
    }

    @Transactional(readOnly = true)
    public ChatDetailResponse getSessionDetail(Long userId, Long sessionId) {
        ChatSession session = getOwnedSession(userId, sessionId);
        List<ChatMessageResponse> messages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();

        return new ChatDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getSourceDiary() == null ? null : session.getSourceDiary().getId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages
        );
    }

    public SendChatMessageResponse sendMessage(Long userId, Long sessionId, SendChatMessageRequest request) {
        ChatSession session = getOwnedSession(userId, sessionId);
        String trimmedMessage = request.message().trim();
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.create(session, ChatMessageRole.USER, trimmedMessage)
        );

        String assistantReply = AI_FALLBACK_REPLY;
        String responseType = "FALLBACK";

        try {
            AiSafetyClient.RiskScoreResponse riskScoreResponse = aiSafetyClient.scoreRisk(
                    new AiSafetyClient.RiskScoreRequest(
                            userId,
                            sessionId,
                            trimmedMessage,
                            "CHAT_MESSAGE"
                    )
            );

            if (isHighRisk(riskScoreResponse)) {
                assistantReply = SAFETY_REPLY;
                responseType = "SAFETY";
            } else if (isMediumRisk(riskScoreResponse)) {
                assistantReply = SUPPORTIVE_REPLY;
                responseType = "SUPPORTIVE";
            } else {
                UserSettings userSettings = userSettingsRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("사용자 설정을 찾을 수 없습니다."));

                List<AiChatClient.ConversationTurn> history = chatMessageRepository
                        .findTop20BySessionIdOrderByCreatedAtDesc(sessionId)
                        .stream()
                        .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                        .map(message -> new AiChatClient.ConversationTurn(
                                message.getRole().name().toLowerCase(),
                                message.getContent()
                        ))
                        .collect(Collectors.toList());

                AiChatClient.GenerateReplyResponse aiResponse = aiChatClient.generateReply(
                        new AiChatClient.GenerateReplyRequest(
                                userId,
                                sessionId,
                                trimmedMessage,
                                history,
                                buildMemorySummary(session),
                                userSettings.getResponseMode()
                        )
                );

                if (aiResponse != null && aiResponse.reply() != null && !aiResponse.reply().isBlank()) {
                    assistantReply = aiResponse.reply();
                    responseType = aiResponse.responseType() == null ? "NORMAL" : aiResponse.responseType();
                }
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "Chat AI orchestration failed. requestId={}, sessionId={}, userId={}",
                    RequestTraceContext.currentRequestId(),
                    sessionId,
                    userId,
                    exception
            );
            appMetricsRecorder.incrementChatAiFailure();
        }

        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.create(session, ChatMessageRole.ASSISTANT, assistantReply)
        );

        log.info(
                "Chat message processed. requestId={}, sessionId={}, userId={}, userMessageId={}, assistantMessageId={}, responseType={}",
                RequestTraceContext.currentRequestId(),
                sessionId,
                userId,
                userMessage.getId(),
                assistantMessage.getId(),
                responseType
        );
        appMetricsRecorder.incrementChatResponse(responseType);

        return new SendChatMessageResponse(
                sessionId,
                userMessage.getId(),
                assistantMessage.getId(),
                assistantReply,
                responseType
        );
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        getActiveUser(userId);
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션을 찾을 수 없습니다."));
    }

    private String buildMemorySummary(ChatSession session) {
        if (session.getSourceDiary() == null) {
            return null;
        }

        return "sourceDiaryTitle=" + session.getSourceDiary().getTitle();
    }

    private boolean isHighRisk(AiSafetyClient.RiskScoreResponse riskScoreResponse) {
        return riskScoreResponse != null
                && "HIGH".equalsIgnoreCase(riskScoreResponse.riskLevel())
                && "SAFETY_RESPONSE".equalsIgnoreCase(riskScoreResponse.recommendedAction());
    }

    private boolean isMediumRisk(AiSafetyClient.RiskScoreResponse riskScoreResponse) {
        return riskScoreResponse != null
                && "MEDIUM".equalsIgnoreCase(riskScoreResponse.riskLevel())
                && "SUPPORTIVE_RESPONSE".equalsIgnoreCase(riskScoreResponse.recommendedAction());
    }
}
