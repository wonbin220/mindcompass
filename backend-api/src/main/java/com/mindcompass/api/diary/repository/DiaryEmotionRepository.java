package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.DiaryEmotion;
import com.mindcompass.api.diary.domain.DiaryEmotionSourceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// 일기별 감정 태그를 저장하고 조회하는 저장소 인터페이스입니다.
public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, Long> {

    List<DiaryEmotion> findAllByDiaryIdOrderByCreatedAtAsc(Long diaryId);

    List<DiaryEmotion> findAllByDiaryIdInOrderByDiaryIdAscCreatedAtAsc(List<Long> diaryIds);

    void deleteAllByDiaryId(Long diaryId);

    void deleteAllByDiaryIdAndSourceType(Long diaryId, DiaryEmotionSourceType sourceType);
}
