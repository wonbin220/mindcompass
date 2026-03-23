package com.mindcompass.api.diary.controller;

// 감정 일기 CRUD와 날짜별 조회 API를 받는 컨트롤러다.

import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryDetailResponse;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @Operation(summary = "일기 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiaryDetailResponse createDiary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateDiaryRequest request
    ) {
        return diaryService.createDiary(userId, request);
    }

    @Operation(summary = "일기 상세 조회")
    @GetMapping("/{diaryId}")
    public DiaryDetailResponse getDiary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일기 ID", example = "1")
            @PathVariable Long diaryId
    ) {
        return diaryService.getDiary(userId, diaryId);
    }

    @Operation(summary = "일기 수정")
    @PatchMapping("/{diaryId}")
    public DiaryDetailResponse updateDiary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일기 ID", example = "1")
            @PathVariable Long diaryId,
            @Valid @RequestBody UpdateDiaryRequest request
    ) {
        return diaryService.updateDiary(userId, diaryId, request);
    }

    @Operation(summary = "일기 삭제")
    @DeleteMapping("/{diaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일기 ID", example = "1")
            @PathVariable Long diaryId
    ) {
        diaryService.deleteDiary(userId, diaryId);
    }

    @Operation(summary = "날짜별 일기 목록 조회")
    @GetMapping
    public DiaryListResponse getDiariesByDate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 날짜", example = "2026-03-21")
            @RequestParam LocalDate date
    ) {
        return diaryService.getDiariesByDate(userId, date);
    }
}
