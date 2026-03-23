package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 일기 AI 분석 결과를 저장하고 일기 기준으로 조회하는 저장소 인터페이스입니다.
public interface DiaryAiAnalysisRepository extends JpaRepository<DiaryAiAnalysis, Long> {

    Optional<DiaryAiAnalysis> findByDiaryId(Long diaryId);
}
