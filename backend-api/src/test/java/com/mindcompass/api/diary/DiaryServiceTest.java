package com.mindcompass.api.diary;

// Diary 생성 시 저장 우선과 AI fallback 동작을 검증하는 서비스 테스트다.

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import com.mindcompass.api.diary.domain.DiaryEmotion;
import com.mindcompass.api.diary.domain.DiaryEmotionSourceType;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.EmotionTagRequest;
import com.mindcompass.api.diary.dto.response.DiaryDetailResponse;
import com.mindcompass.api.diary.repository.DiaryAiAnalysisRepository;
import com.mindcompass.api.diary.repository.DiaryEmotionRepository;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import com.mindcompass.api.diary.service.DiaryService;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryEmotionRepository diaryEmotionRepository;

    @Mock
    private DiaryAiAnalysisRepository diaryAiAnalysisRepository;

    @Mock
    private DiaryQueryRepository diaryQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiDiaryAnalysisClient aiDiaryAnalysisClient;

    @Mock
    private AiSafetyClient aiSafetyClient;

    @Mock
    private AppMetricsRecorder appMetricsRecorder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DiaryService diaryService;

    @Test
    void createDiaryReturnsRiskFieldsWhenRiskScoreSucceeds() throws Exception {
        User user = createUser(1L);
        Diary diary = createDiary(11L, user, "I felt overwhelmed and isolated today.");
        DiaryAiAnalysis analysis = DiaryAiAnalysis.create(diary);
        List<DiaryEmotion> emotionTags = List.of(
                DiaryEmotion.create(diary, PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(diaryRepository.save(any(Diary.class))).thenReturn(diary);
        when(aiDiaryAnalysisClient.analyze(any())).thenReturn(
                new AiDiaryAnalysisClient.AnalyzeDiaryResponse(
                        "OVERWHELMED",
                        5,
                        List.of("OVERWHELMED"),
                        "High distress with some emotional overload.",
                        BigDecimal.valueOf(0.81)
                )
        );
        when(aiSafetyClient.scoreRisk(any())).thenReturn(
                new AiSafetyClient.RiskScoreResponse(
                        "MEDIUM",
                        BigDecimal.valueOf(0.65),
                        List.of("DISTRESS_ESCALATION", "ISOLATION"),
                        "SUPPORTIVE_RESPONSE"
                )
        );
        when(diaryAiAnalysisRepository.findByDiaryId(11L))
                .thenReturn(Optional.empty(), Optional.of(analysis), Optional.of(analysis));
        when(diaryAiAnalysisRepository.save(any(DiaryAiAnalysis.class))).thenReturn(analysis);
        when(diaryEmotionRepository.findAllByDiaryIdOrderByCreatedAtAsc(11L)).thenReturn(emotionTags);

        DiaryDetailResponse response = diaryService.createDiary(1L, createRequest());

        assertThat(response.diaryId()).isEqualTo(11L);
        assertThat(response.riskLevel()).isEqualTo("MEDIUM");
        assertThat(response.riskScore()).isEqualByComparingTo("0.65");
        assertThat(response.recommendedAction()).isEqualTo("SUPPORTIVE_RESPONSE");
        assertThat(response.riskSignals()).contains("DISTRESS_ESCALATION");

        ArgumentCaptor<DiaryAiAnalysis> analysisCaptor = ArgumentCaptor.forClass(DiaryAiAnalysis.class);
        verify(diaryAiAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getDiary()).isEqualTo(diary);
    }

    @Test
    void createDiaryKeepsSavedDiaryWhenAnalysisFails() throws Exception {
        User user = createUser(1L);
        Diary diary = createDiary(11L, user, "I barely made it through today.");
        DiaryAiAnalysis analysis = DiaryAiAnalysis.create(diary);
        List<DiaryEmotion> emotionTags = List.of(
                DiaryEmotion.create(diary, PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(diaryRepository.save(any(Diary.class))).thenReturn(diary);
        when(aiDiaryAnalysisClient.analyze(any())).thenThrow(new RuntimeException("ai timeout"));
        when(aiSafetyClient.scoreRisk(any())).thenReturn(
                new AiSafetyClient.RiskScoreResponse(
                        "MEDIUM",
                        BigDecimal.valueOf(0.66),
                        List.of("DISTRESS_ESCALATION"),
                        "SUPPORTIVE_RESPONSE"
                )
        );
        when(diaryAiAnalysisRepository.findByDiaryId(11L))
                .thenReturn(Optional.empty(), Optional.of(analysis));
        when(diaryAiAnalysisRepository.save(any(DiaryAiAnalysis.class))).thenReturn(analysis);
        when(diaryEmotionRepository.findAllByDiaryIdOrderByCreatedAtAsc(11L)).thenReturn(emotionTags);

        DiaryDetailResponse response = diaryService.createDiary(1L, createRequest());

        assertThat(response.diaryId()).isEqualTo(11L);
        assertThat(response.title()).isEqualTo("Diary test title");
        assertThat(response.primaryEmotion()).isEqualTo(PrimaryEmotion.OVERWHELMED);
        assertThat(response.riskLevel()).isEqualTo("MEDIUM");
        assertThat(response.recommendedAction()).isEqualTo("SUPPORTIVE_RESPONSE");

        verify(appMetricsRecorder).incrementDiaryAiAnalysisFailure();
        verify(diaryEmotionRepository, never())
                .deleteAllByDiaryIdAndSourceType(11L, DiaryEmotionSourceType.AI_ANALYSIS);
    }

    @Test
    void createDiaryKeepsSavedDiaryWhenRiskScoringFails() throws Exception {
        User user = createUser(1L);
        Diary diary = createDiary(11L, user, "I felt overloaded but managed to write it down.");
        DiaryAiAnalysis analysis = DiaryAiAnalysis.create(diary);
        List<DiaryEmotion> emotionTags = List.of(
                DiaryEmotion.create(diary, PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER),
                DiaryEmotion.create(diary, PrimaryEmotion.ANXIOUS, null, DiaryEmotionSourceType.AI_ANALYSIS)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(diaryRepository.save(any(Diary.class))).thenReturn(diary);
        when(aiDiaryAnalysisClient.analyze(any())).thenReturn(
                new AiDiaryAnalysisClient.AnalyzeDiaryResponse(
                        "OVERWHELMED",
                        5,
                        List.of("OVERWHELMED", "ANXIOUS"),
                        "Distress is present, but the diary was still captured.",
                        BigDecimal.valueOf(0.79)
                )
        );
        when(aiSafetyClient.scoreRisk(any())).thenThrow(new RuntimeException("risk timeout"));
        when(diaryAiAnalysisRepository.findByDiaryId(11L))
                .thenReturn(Optional.empty(), Optional.of(analysis), Optional.of(analysis));
        when(diaryAiAnalysisRepository.save(any(DiaryAiAnalysis.class))).thenReturn(analysis);
        when(diaryEmotionRepository.findAllByDiaryIdOrderByCreatedAtAsc(11L)).thenReturn(emotionTags);

        DiaryDetailResponse response = diaryService.createDiary(1L, createRequest());

        assertThat(response.diaryId()).isEqualTo(11L);
        assertThat(response.primaryEmotion()).isEqualTo(PrimaryEmotion.OVERWHELMED);
        assertThat(response.riskLevel()).isNull();
        assertThat(response.riskScore()).isNull();
        assertThat(response.recommendedAction()).isNull();
        assertThat(response.emotionTags()).hasSize(2);

        verify(appMetricsRecorder).incrementDiaryRiskFailure();
    }

    private CreateDiaryRequest createRequest() {
        return new CreateDiaryRequest(
                "Diary test title",
                "I felt overwhelmed and isolated today.",
                PrimaryEmotion.OVERWHELMED,
                5,
                List.of(new EmotionTagRequest(PrimaryEmotion.OVERWHELMED, 5)),
                LocalDateTime.of(2026, 3, 21, 21, 0)
        );
    }

    private User createUser(Long id) throws Exception {
        User user = User.create("diary@example.com", "encoded-password", "tester");
        setField(user, "id", id);
        return user;
    }

    private Diary createDiary(Long id, User user, String content) throws Exception {
        Diary diary = Diary.create(
                user,
                "Diary test title",
                content,
                PrimaryEmotion.OVERWHELMED,
                5,
                LocalDateTime.of(2026, 3, 21, 21, 0)
        );
        setField(diary, "id", id);
        return diary;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
