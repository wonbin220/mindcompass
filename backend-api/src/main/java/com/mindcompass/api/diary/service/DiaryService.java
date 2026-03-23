package com.mindcompass.api.diary.service;

// 일기 저장과 감정 태그, AI 분석, 위험도 후처리를 함께 조율하는 서비스다.

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.common.exception.InvalidDiaryRequestException;
import com.mindcompass.api.common.exception.ResourceNotFoundException;
import com.mindcompass.api.common.logging.RequestTraceContext;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import com.mindcompass.api.diary.domain.DiaryEmotion;
import com.mindcompass.api.diary.domain.DiaryEmotionSourceType;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.EmotionTagRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryDetailResponse;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.dto.response.EmotionTagResponse;
import com.mindcompass.api.diary.repository.DiaryAiAnalysisRepository;
import com.mindcompass.api.diary.repository.DiaryEmotionRepository;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiaryService {

    private static final Logger log = LoggerFactory.getLogger(DiaryService.class);

    private final DiaryRepository diaryRepository;
    private final DiaryEmotionRepository diaryEmotionRepository;
    private final DiaryAiAnalysisRepository diaryAiAnalysisRepository;
    private final DiaryQueryRepository diaryQueryRepository;
    private final UserRepository userRepository;
    private final AiDiaryAnalysisClient aiDiaryAnalysisClient;
    private final AiSafetyClient aiSafetyClient;
    private final ObjectMapper objectMapper;
    private final AppMetricsRecorder appMetricsRecorder;

    public DiaryService(
            DiaryRepository diaryRepository,
            DiaryEmotionRepository diaryEmotionRepository,
            DiaryAiAnalysisRepository diaryAiAnalysisRepository,
            DiaryQueryRepository diaryQueryRepository,
            UserRepository userRepository,
            AiDiaryAnalysisClient aiDiaryAnalysisClient,
            AiSafetyClient aiSafetyClient,
            ObjectMapper objectMapper,
            AppMetricsRecorder appMetricsRecorder
    ) {
        this.diaryRepository = diaryRepository;
        this.diaryEmotionRepository = diaryEmotionRepository;
        this.diaryAiAnalysisRepository = diaryAiAnalysisRepository;
        this.diaryQueryRepository = diaryQueryRepository;
        this.userRepository = userRepository;
        this.aiDiaryAnalysisClient = aiDiaryAnalysisClient;
        this.aiSafetyClient = aiSafetyClient;
        this.objectMapper = objectMapper;
        this.appMetricsRecorder = appMetricsRecorder;
    }

    public DiaryDetailResponse createDiary(Long userId, CreateDiaryRequest request) {
        validateEmotionIntensity(request.emotionIntensity());
        validateWrittenAt(request.writtenAt());

        User user = getActiveUser(userId);
        Diary diary = Diary.create(
                user,
                request.title().trim(),
                request.content().trim(),
                request.primaryEmotion(),
                request.emotionIntensity(),
                request.writtenAt()
        );

        Diary savedDiary = diaryRepository.save(diary);
        replaceUserEmotionTags(savedDiary, request.emotionTags());
        enrichDiaryAiSignals(savedDiary);

        log.info(
                "Diary created. requestId={}, diaryId={}, userId={}, writtenAt={}",
                RequestTraceContext.currentRequestId(),
                savedDiary.getId(),
                userId,
                savedDiary.getWrittenAt()
        );
        appMetricsRecorder.incrementDiaryCreated();

        return buildDiaryDetailResponse(savedDiary);
    }

    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiary(Long userId, Long diaryId) {
        Diary diary = getOwnedDiary(userId, diaryId);
        return buildDiaryDetailResponse(diary);
    }

    public DiaryDetailResponse updateDiary(Long userId, Long diaryId, UpdateDiaryRequest request) {
        validateEmotionIntensity(request.emotionIntensity());
        validateWrittenAt(request.writtenAt());

        Diary diary = getOwnedDiary(userId, diaryId);
        diary.update(
                request.title().trim(),
                request.content().trim(),
                request.primaryEmotion(),
                request.emotionIntensity(),
                request.writtenAt()
        );

        replaceUserEmotionTags(diary, request.emotionTags());
        enrichDiaryAiSignals(diary);

        log.info(
                "Diary updated. requestId={}, diaryId={}, userId={}, writtenAt={}",
                RequestTraceContext.currentRequestId(),
                diary.getId(),
                userId,
                diary.getWrittenAt()
        );
        appMetricsRecorder.incrementDiaryUpdated();

        return buildDiaryDetailResponse(diary);
    }

    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = getOwnedDiary(userId, diaryId);
        diary.softDelete(LocalDateTime.now());

        log.info(
                "Diary deleted. requestId={}, diaryId={}, userId={}",
                RequestTraceContext.currentRequestId(),
                diaryId,
                userId
        );
        appMetricsRecorder.incrementDiaryDeleted();
    }

    @Transactional(readOnly = true)
    public DiaryListResponse getDiariesByDate(Long userId, LocalDate date) {
        getActiveUser(userId);
        List<DiarySummaryResponse> diaries = diaryQueryRepository.findDailySummaries(userId, date);
        Map<Long, List<EmotionTagResponse>> emotionTagsByDiaryId = loadEmotionTagsByDiaryIds(
                diaries.stream().map(DiarySummaryResponse::diaryId).toList()
        );

        List<DiarySummaryResponse> enrichedDiaries = diaries.stream()
                .map(summary -> summary.withEmotionTags(
                        emotionTagsByDiaryId.getOrDefault(summary.diaryId(), Collections.emptyList())
                ))
                .toList();

        return DiaryListResponse.of(date, enrichedDiaries);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Diary getOwnedDiary(Long userId, Long diaryId) {
        getActiveUser(userId);
        return diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diaryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("일기를 찾을 수 없습니다."));
    }

    private void validateEmotionIntensity(Integer emotionIntensity) {
        if (emotionIntensity != null && emotionIntensity < 1) {
            throw new InvalidDiaryRequestException("감정 강도는 1 이상이어야 합니다.");
        }
    }

    private void validateWrittenAt(LocalDateTime writtenAt) {
        if (writtenAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new InvalidDiaryRequestException("작성 시각은 현재보다 크게 미래일 수 없습니다.");
        }
    }

    private void replaceUserEmotionTags(Diary diary, List<EmotionTagRequest> emotionTags) {
        diaryEmotionRepository.deleteAllByDiaryIdAndSourceType(diary.getId(), DiaryEmotionSourceType.USER);

        if (emotionTags == null || emotionTags.isEmpty()) {
            return;
        }

        List<DiaryEmotion> entities = emotionTags.stream()
                .map(this::validateEmotionTag)
                .map(tag -> DiaryEmotion.create(
                        diary,
                        tag.emotionCode(),
                        tag.intensity(),
                        DiaryEmotionSourceType.USER
                ))
                .toList();

        diaryEmotionRepository.saveAll(entities);
    }

    private EmotionTagRequest validateEmotionTag(EmotionTagRequest tag) {
        if (tag.intensity() != null && tag.intensity() < 1) {
            throw new InvalidDiaryRequestException("감정 태그 강도는 1 이상이어야 합니다.");
        }
        return tag;
    }

    private DiaryDetailResponse buildDiaryDetailResponse(Diary diary) {
        return DiaryDetailResponse.from(
                diary,
                loadEmotionTags(diary.getId()),
                diaryAiAnalysisRepository.findByDiaryId(diary.getId()).orElse(null)
        );
    }

    private List<EmotionTagResponse> loadEmotionTags(Long diaryId) {
        List<DiaryEmotion> emotionTags = diaryEmotionRepository.findAllByDiaryIdOrderByCreatedAtAsc(diaryId);
        if (emotionTags.isEmpty()) {
            return Collections.emptyList();
        }

        return emotionTags.stream()
                .map(tag -> new EmotionTagResponse(
                        tag.getEmotionCode(),
                        tag.getIntensity(),
                        tag.getSourceType()
                ))
                .toList();
    }

    private Map<Long, List<EmotionTagResponse>> loadEmotionTagsByDiaryIds(List<Long> diaryIds) {
        if (diaryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return diaryEmotionRepository.findAllByDiaryIdInOrderByDiaryIdAscCreatedAtAsc(diaryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getDiary().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                tag -> new EmotionTagResponse(
                                        tag.getEmotionCode(),
                                        tag.getIntensity(),
                                        tag.getSourceType()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    private void enrichDiaryAiSignals(Diary diary) {
        saveDiaryAnalysisIfAvailable(diary);
        saveDiaryRiskIfAvailable(diary);
    }

    private void saveDiaryAnalysisIfAvailable(Diary diary) {
        try {
            AiDiaryAnalysisClient.AnalyzeDiaryResponse analysis = aiDiaryAnalysisClient.analyze(
                    new AiDiaryAnalysisClient.AnalyzeDiaryRequest(
                            diary.getUser().getId(),
                            diary.getId(),
                            diary.getContent(),
                            diary.getWrittenAt().toString()
                    )
            );

            DiaryAiAnalysis diaryAiAnalysis = getOrCreateDiaryAiAnalysis(diary);
            diaryAiAnalysis.updateAnalysis(
                    analysis.primaryEmotion(),
                    analysis.emotionIntensity(),
                    analysis.summary(),
                    analysis.confidence(),
                    buildRawPayload(analysis)
            );

            replaceAiEmotionTags(diary, analysis);
        } catch (RuntimeException exception) {
            // AI 분석 실패는 일기 저장 실패로 번지지 않도록 하고 경고 로그만 남긴다.
            log.warn(
                    "Diary AI analysis failed. requestId={}, diaryId={}",
                    RequestTraceContext.currentRequestId(),
                    diary.getId(),
                    exception
            );
            appMetricsRecorder.incrementDiaryAiAnalysisFailure();
        }
    }

    private void saveDiaryRiskIfAvailable(Diary diary) {
        try {
            AiSafetyClient.RiskScoreResponse riskScoreResponse = aiSafetyClient.scoreRisk(
                    new AiSafetyClient.RiskScoreRequest(
                            diary.getUser().getId(),
                            diary.getId(),
                            diary.getContent(),
                            "DIARY"
                    )
            );

            DiaryAiAnalysis diaryAiAnalysis = getOrCreateDiaryAiAnalysis(diary);
            diaryAiAnalysis.updateRisk(
                    riskScoreResponse == null ? null : riskScoreResponse.riskLevel(),
                    riskScoreResponse == null ? null : riskScoreResponse.riskScore(),
                    buildRiskSignals(riskScoreResponse == null ? null : riskScoreResponse.signals()),
                    riskScoreResponse == null ? null : riskScoreResponse.recommendedAction()
            );
        } catch (RuntimeException exception) {
            // 위험도 판별 실패도 일기 저장은 유지하고 경고 로그만 남긴다.
            log.warn(
                    "Diary AI risk scoring failed. requestId={}, diaryId={}",
                    RequestTraceContext.currentRequestId(),
                    diary.getId(),
                    exception
            );
            appMetricsRecorder.incrementDiaryRiskFailure();
        }
    }

    private DiaryAiAnalysis getOrCreateDiaryAiAnalysis(Diary diary) {
        return diaryAiAnalysisRepository.findByDiaryId(diary.getId())
                .orElseGet(() -> diaryAiAnalysisRepository.save(DiaryAiAnalysis.create(diary)));
    }

    private void replaceAiEmotionTags(Diary diary, AiDiaryAnalysisClient.AnalyzeDiaryResponse analysis) {
        diaryEmotionRepository.deleteAllByDiaryIdAndSourceType(diary.getId(), DiaryEmotionSourceType.AI_ANALYSIS);

        List<DiaryEmotion> aiTags = new ArrayList<>();
        PrimaryEmotion primaryEmotion = toPrimaryEmotion(analysis.primaryEmotion());

        if (primaryEmotion != null) {
            aiTags.add(DiaryEmotion.create(
                    diary,
                    primaryEmotion,
                    analysis.emotionIntensity(),
                    DiaryEmotionSourceType.AI_ANALYSIS
            ));
        }

        if (analysis.emotionTags() != null) {
            analysis.emotionTags().stream()
                    .map(this::toPrimaryEmotion)
                    .filter(emotion -> emotion != null)
                    .filter(emotion -> primaryEmotion == null || emotion != primaryEmotion)
                    .map(emotion -> DiaryEmotion.create(
                            diary,
                            emotion,
                            null,
                            DiaryEmotionSourceType.AI_ANALYSIS
                    ))
                    .forEach(aiTags::add);
        }

        if (!aiTags.isEmpty()) {
            diaryEmotionRepository.saveAll(aiTags);
        }
    }

    private String buildRawPayload(AiDiaryAnalysisClient.AnalyzeDiaryResponse analysis) {
        try {
            // 분석 결과 전문을 JSON 문자열로 저장해 추후 리포트나 디버깅에 재사용한다.
            return objectMapper.writeValueAsString(Map.of(
                    "primaryEmotion", analysis.primaryEmotion(),
                    "emotionIntensity", analysis.emotionIntensity(),
                    "emotionTags", analysis.emotionTags(),
                    "summary", analysis.summary(),
                    "confidence", analysis.confidence()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 분석 결과를 raw payload로 직렬화하지 못했습니다.", exception);
        }
    }

    private String buildRiskSignals(List<String> riskSignals) {
        if (riskSignals == null || riskSignals.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(riskSignals);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 위험 신호를 문자열로 직렬화하지 못했습니다.", exception);
        }
    }

    private PrimaryEmotion toPrimaryEmotion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return PrimaryEmotion.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            log.warn("Unsupported AI emotion tag received. value={}", value);
            return null;
        }
    }
}
