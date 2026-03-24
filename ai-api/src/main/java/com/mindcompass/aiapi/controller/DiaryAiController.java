// 일기 감정 분석 내부 엔드포인트를 제공하는 컨트롤러입니다.
package com.mindcompass.aiapi.controller;

import com.mindcompass.aiapi.dto.AnalyzeDiaryRequest;
import com.mindcompass.aiapi.dto.AnalyzeDiaryResponse;
import com.mindcompass.aiapi.service.DiaryAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class DiaryAiController {

    private final DiaryAnalysisService diaryAnalysisService;

    public DiaryAiController(DiaryAnalysisService diaryAnalysisService) {
        this.diaryAnalysisService = diaryAnalysisService;
    }

    @PostMapping("/analyze-diary")
    public AnalyzeDiaryResponse analyzeDiary(@Valid @RequestBody AnalyzeDiaryRequest request) {
        return diaryAnalysisService.analyze(request);
    }
}
