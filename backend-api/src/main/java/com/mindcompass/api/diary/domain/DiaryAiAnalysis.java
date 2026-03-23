package com.mindcompass.api.diary.domain;

// 일기 AI 분석과 위험도 결과를 함께 저장하는 엔티티다.

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "diary_ai_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false, unique = true)
    private Diary diary;

    @Column(name = "primary_emotion", length = 30)
    private String primaryEmotion;

    @Column(name = "emotion_intensity")
    private Integer emotionIntensity;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_score", precision = 4, scale = 3)
    private BigDecimal riskScore;

    @Column(name = "risk_signals", columnDefinition = "TEXT")
    private String riskSignals;

    @Column(name = "recommended_action", length = 40)
    private String recommendedAction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private DiaryAiAnalysis(Diary diary) {
        this.diary = diary;
    }

    public static DiaryAiAnalysis create(Diary diary) {
        return new DiaryAiAnalysis(diary);
    }

    public void updateAnalysis(
            String primaryEmotion,
            Integer emotionIntensity,
            String summary,
            BigDecimal confidence,
            String rawPayload
    ) {
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
        this.summary = summary;
        this.confidence = confidence;
        this.rawPayload = rawPayload;
    }

    public void updateRisk(
            String riskLevel,
            BigDecimal riskScore,
            String riskSignals,
            String recommendedAction
    ) {
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.riskSignals = riskSignals;
        this.recommendedAction = recommendedAction;
    }
}
