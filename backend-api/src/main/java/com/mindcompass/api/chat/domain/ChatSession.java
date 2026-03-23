package com.mindcompass.api.chat.domain;

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.diary.domain.Diary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 사용자별 상담 대화방 단위를 저장하는 채팅 세션 엔티티입니다.
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_diary_id")
    private Diary sourceDiary;

    @Column(nullable = false, length = 100)
    private String title;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private ChatSession(User user, Diary sourceDiary, String title) {
        this.user = user;
        this.sourceDiary = sourceDiary;
        this.title = title;
    }

    public static ChatSession create(User user, Diary sourceDiary, String title) {
        return new ChatSession(user, sourceDiary, title);
    }
}
