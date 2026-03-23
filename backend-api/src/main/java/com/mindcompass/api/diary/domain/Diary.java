package com.mindcompass.api.diary.domain;

import com.mindcompass.api.auth.domain.User;
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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "diaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 사용자가 직접 작성한 감정 일기 본문과 대표 감정을 저장하는 엔티티입니다.
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_emotion", length = 30)
    private PrimaryEmotion primaryEmotion;

    @Column(name = "emotion_intensity")
    private Integer emotionIntensity;

    @Column(name = "written_at", nullable = false)
    private LocalDateTime writtenAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Diary(
            User user,
            String title,
            String content,
            PrimaryEmotion primaryEmotion,
            Integer emotionIntensity,
            LocalDateTime writtenAt
    ) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
        this.writtenAt = writtenAt;
    }

    public static Diary create(
            User user,
            String title,
            String content,
            PrimaryEmotion primaryEmotion,
            Integer emotionIntensity,
            LocalDateTime writtenAt
    ) {
        return new Diary(user, title, content, primaryEmotion, emotionIntensity, writtenAt);
    }

    public void update(
            String title,
            String content,
            PrimaryEmotion primaryEmotion,
            Integer emotionIntensity,
            LocalDateTime writtenAt
    ) {
        this.title = title;
        this.content = content;
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
        this.writtenAt = writtenAt;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
