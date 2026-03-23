package com.mindcompass.api.diary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "diary_emotions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 한 개의 일기에 연결된 다중 감정 태그를 저장하는 확장 엔티티입니다.
public class DiaryEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_code", nullable = false, length = 30)
    private PrimaryEmotion emotionCode;

    @Column(name = "intensity")
    private Integer intensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private DiaryEmotionSourceType sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private DiaryEmotion(
            Diary diary,
            PrimaryEmotion emotionCode,
            Integer intensity,
            DiaryEmotionSourceType sourceType
    ) {
        this.diary = diary;
        this.emotionCode = emotionCode;
        this.intensity = intensity;
        this.sourceType = sourceType;
    }

    public static DiaryEmotion create(
            Diary diary,
            PrimaryEmotion emotionCode,
            Integer intensity,
            DiaryEmotionSourceType sourceType
    ) {
        return new DiaryEmotion(diary, emotionCode, intensity, sourceType);
    }
}
