// 감정분류 모델 registry 내부 관리 API를 제공하는 컨트롤러이다.
package com.mindcompass.aiapi.registry.controller;

import com.mindcompass.aiapi.registry.dto.CreateEmotionModelRegistryRequest;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAdminSummaryResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAvailableTransitionsResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactHealthResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactJsonCheckResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryPromotionChecklistResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryRuntimeAlignmentResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryStatusHistoryResponse;
import com.mindcompass.aiapi.registry.dto.UpdateEmotionModelRegistryStatusRequest;
import com.mindcompass.aiapi.registry.service.EmotionModelRegistryService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/internal/admin/emotion-models")
@Tag(name = "Emotion Model Registry Admin", description = "감정 분류 모델 registry 운영 상태를 조회하고 전환하는 내부 관리자 API")
public class EmotionModelRegistryAdminController {

    private final EmotionModelRegistryService emotionModelRegistryService;

    public EmotionModelRegistryAdminController(EmotionModelRegistryService emotionModelRegistryService) {
        this.emotionModelRegistryService = emotionModelRegistryService;
    }

    @GetMapping
    @Operation(
            summary = "감정 모델 registry 목록 조회",
            description = """
                    내부 운영자가 registry row를 필터링해서 조회하는 API이다.
                    `isShadow`는 "이 row가 shadow candidate lineage/source로 등록된 적이 있는가"를 뜻하는 이력성 플래그이고,
                    `currentShadowOnly`는 "현재 status가 SHADOW인가"를 뜻하는 현재 상태 필터이다.
                    예를 들어 `isShadow=true`는 현재 APPROVED/REJECTED 상태여도 shadow candidate 출신 row를 포함하고,
                    `currentShadowOnly=true`는 현재 shadow 관찰 중인 row만 반환한다.
                    
                    Swagger UI example combinations:
                    - historical shadow lineage lookup: `/internal/admin/emotion-models?isShadow=true`
                    - current shadow queue lookup: `/internal/admin/emotion-models?currentShadowOnly=true`
                    - shadow lineage but not currently shadow: `/internal/admin/emotion-models?isShadow=true&currentShadowOnly=false`
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조건에 맞는 registry row 목록을 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "historicalShadowLineageList",
                                            summary = "isShadow=true 결과 예시",
                                            value = """
                                                    [
                                                      {
                                                        "id": 2,
                                                        "experimentName": "cpu_compare_medium_manual_seed_v2",
                                                        "modelName": "cpu_compare_medium_manual_seed_v2_active5",
                                                        "baseModelName": "beomi/KcELECTRA-base",
                                                        "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2",
                                                        "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json",
                                                        "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2/best/label_metadata.json",
                                                        "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                                        "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                                        "trainingDatasetTag": "emotion_mvp_manual_seed_v2",
                                                        "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                                        "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                                        "status": "APPROVED",
                                                        "active": false,
                                                        "shadow": true,
                                                        "accuracy": 0.4427,
                                                        "macroF1": 0.3888,
                                                        "happyF1": 0.6250,
                                                        "calmF1": 0.3182,
                                                        "anxiousF1": 0.4021,
                                                        "sadF1": 0.3564,
                                                        "angryF1": 0.2423,
                                                        "servingNotes": "best macro F1 but watch HAPPY->CALM collapse",
                                                        "approvalNote": "approved as shadow candidate only",
                                                        "rejectionReason": null,
                                                        "approvedAt": "2026-03-29T15:10:00",
                                                        "rejectedAt": null,
                                                        "activatedAt": null,
                                                        "createdAt": "2026-03-29T14:55:00",
                                                        "updatedAt": "2026-03-29T15:10:00"
                                                      },
                                                      {
                                                        "id": 3,
                                                        "experimentName": "cpu_compare_medium_manual_seed_v2_fixed_compare",
                                                        "modelName": "cpu_compare_medium_manual_seed_v2_fixed_compare_active5",
                                                        "baseModelName": "beomi/KcELECTRA-base",
                                                        "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2_fixed_compare",
                                                        "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_final.json",
                                                        "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2_fixed_compare/best/label_metadata.json",
                                                        "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                                        "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                                        "trainingDatasetTag": "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                                                        "validationDatasetTag": "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                                                        "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                                        "status": "REJECTED",
                                                        "active": false,
                                                        "shadow": true,
                                                        "accuracy": 0.4067,
                                                        "macroF1": 0.3484,
                                                        "happyF1": 0.1946,
                                                        "calmF1": 0.6288,
                                                        "anxiousF1": 0.2744,
                                                        "sadF1": 0.2975,
                                                        "angryF1": 0.3476,
                                                        "servingNotes": "fixed compare regression against baseline",
                                                        "approvalNote": null,
                                                        "rejectionReason": "fixed compare gate did not beat current baseline",
                                                        "approvedAt": null,
                                                        "rejectedAt": "2026-03-29T16:20:00",
                                                        "activatedAt": null,
                                                        "createdAt": "2026-03-29T16:00:00",
                                                        "updatedAt": "2026-03-29T16:20:00"
                                                      }
                                                    ]
                                                    """),
                                    @ExampleObject(
                                            name = "currentShadowQueueList",
                                            summary = "currentShadowOnly=true 결과 예시",
                                            value = """
                                                    [
                                                      {
                                                        "id": 4,
                                                        "experimentName": "cpu_compare_medium_guard_metric_v1",
                                                        "modelName": "cpu_compare_medium_guard_metric_v1_active5",
                                                        "baseModelName": "beomi/KcELECTRA-base",
                                                        "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_guard_metric_v1",
                                                        "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_guard_metric_v1.json",
                                                        "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_guard_metric_v1/best/label_metadata.json",
                                                        "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu_happy_calm_guard_metric_v1.json",
                                                        "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                                        "trainingDatasetTag": "emotion_mvp_relabel_weighted",
                                                        "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                                        "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                                        "status": "SHADOW",
                                                        "active": false,
                                                        "shadow": true,
                                                        "accuracy": 0.4210,
                                                        "macroF1": 0.3602,
                                                        "happyF1": 0.5400,
                                                        "calmF1": 0.3400,
                                                        "anxiousF1": 0.4012,
                                                        "sadF1": 0.3021,
                                                        "angryF1": 0.2178,
                                                        "servingNotes": "observe HAPPY/CALM boundary in shadow queue",
                                                        "approvalNote": "approved for shadow observation",
                                                        "rejectionReason": null,
                                                        "approvedAt": "2026-03-29T17:05:00",
                                                        "rejectedAt": null,
                                                        "activatedAt": null,
                                                        "createdAt": "2026-03-29T16:50:00",
                                                        "updatedAt": "2026-03-29T17:05:00"
                                                      }
                                                    ]
                                                    """),
                                    @ExampleObject(
                                            name = "shadowLineageButNotCurrentShadowList",
                                            summary = "isShadow=true&currentShadowOnly=false 결과 예시",
                                            value = """
                                                    [
                                                      {
                                                        "id": 2,
                                                        "experimentName": "cpu_compare_medium_manual_seed_v2",
                                                        "modelName": "cpu_compare_medium_manual_seed_v2_active5",
                                                        "baseModelName": "beomi/KcELECTRA-base",
                                                        "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2",
                                                        "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json",
                                                        "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2/best/label_metadata.json",
                                                        "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                                        "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                                        "trainingDatasetTag": "emotion_mvp_manual_seed_v2",
                                                        "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                                        "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                                        "status": "APPROVED",
                                                        "active": false,
                                                        "shadow": true,
                                                        "accuracy": 0.4427,
                                                        "macroF1": 0.3888,
                                                        "happyF1": 0.6250,
                                                        "calmF1": 0.3182,
                                                        "anxiousF1": 0.4021,
                                                        "sadF1": 0.3564,
                                                        "angryF1": 0.2423,
                                                        "servingNotes": "best macro F1 but watch HAPPY->CALM collapse",
                                                        "approvalNote": "approved as shadow candidate only",
                                                        "rejectionReason": null,
                                                        "approvedAt": "2026-03-29T15:10:00",
                                                        "rejectedAt": null,
                                                        "activatedAt": null,
                                                        "createdAt": "2026-03-29T14:55:00",
                                                        "updatedAt": "2026-03-29T15:10:00"
                                                      },
                                                      {
                                                        "id": 3,
                                                        "experimentName": "cpu_compare_medium_manual_seed_v2_fixed_compare",
                                                        "modelName": "cpu_compare_medium_manual_seed_v2_fixed_compare_active5",
                                                        "baseModelName": "beomi/KcELECTRA-base",
                                                        "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2_fixed_compare",
                                                        "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_final.json",
                                                        "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2_fixed_compare/best/label_metadata.json",
                                                        "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                                        "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                                        "trainingDatasetTag": "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                                                        "validationDatasetTag": "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                                                        "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                                        "status": "REJECTED",
                                                        "active": false,
                                                        "shadow": true,
                                                        "accuracy": 0.4067,
                                                        "macroF1": 0.3484,
                                                        "happyF1": 0.1946,
                                                        "calmF1": 0.6288,
                                                        "anxiousF1": 0.2744,
                                                        "sadF1": 0.2975,
                                                        "angryF1": 0.3476,
                                                        "servingNotes": "fixed compare regression against baseline",
                                                        "approvalNote": null,
                                                        "rejectionReason": "fixed compare gate did not beat current baseline",
                                                        "approvedAt": null,
                                                        "rejectedAt": "2026-03-29T16:20:00",
                                                        "activatedAt": null,
                                                        "createdAt": "2026-03-29T16:00:00",
                                                        "updatedAt": "2026-03-29T16:20:00"
                                                      }
                                                    ]
                                                    """)
                            }))
    })
    public List<EmotionModelRegistryResponse> getAll(
            @Parameter(
                    description = "현재 registry 상태를 정확히 일치로 필터링한다. 예: ACTIVE, APPROVED, REJECTED, SHADOW",
                    example = "APPROVED")
            @RequestParam(name = "status", required = false) String status,
            @Parameter(
                    description = "현재 active serving row 여부를 필터링한다.",
                    example = "false")
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @Parameter(
                    description = "shadow candidate lineage/source 플래그 기준으로 필터링한다. 현재 status와 무관하게 과거에 shadow candidate로 등록된 row도 포함한다.",
                    example = "true")
            @RequestParam(name = "isShadow", required = false) Boolean isShadow,
            @Parameter(
                    description = "experiment_name 부분 일치 검색이다. 대소문자를 구분하지 않는다.",
                    example = "manual_seed_v2")
            @RequestParam(name = "experimentName", required = false) String experimentName,
            @Parameter(
                    description = """
                            현재 SHADOW 상태 여부로 필터링한다.
                            true면 현재 `status=SHADOW` row만,
                            false면 현재 `status!=SHADOW` row만 반환한다.
                            `isShadow`와 달리 현재 상태만 본다.
                            """,
                    example = "true")
            @RequestParam(name = "currentShadowOnly", required = false) Boolean currentShadowOnly
    ) {
        return emotionModelRegistryService.getAll(status, isActive, isShadow, experimentName, currentShadowOnly);
    }

    @GetMapping("/summary")
    @Operation(
            summary = "감정 모델 registry 운영 요약 조회",
            description = """
                    운영자가 현재 registry 전체 분포를 빠르게 확인하는 API다.
                    상태별 개수, shadow lineage 개수, 현재 active row id와 experiment 이름을 함께 반환해서
                    별도 목록 필터 검색 전에 운영 상태를 먼저 파악할 수 있게 돕는다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "registry 운영 요약 정보를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "registrySummary",
                                    summary = "운영 요약 예시",
                                    value = """
                                            {
                                              "totalCount": 4,
                                              "trainedCount": 0,
                                              "approvedCount": 1,
                                              "rejectedCount": 1,
                                              "shadowCount": 1,
                                              "activeCount": 1,
                                              "archivedCount": 0,
                                              "shadowLineageCount": 3,
                                              "activeRegistryId": 1,
                                              "activeExperimentName": "cpu_compare_medium_relabel_weighted"
                                            }
                                            """)))
    })
    public EmotionModelRegistryAdminSummaryResponse getSummary() {
        return emotionModelRegistryService.getSummary();
    }

    @GetMapping("/active")
    @Operation(
            summary = "현재 active 감정 모델 조회",
            description = """
                    현재 serving 중인 registry row 1건을 조회하는 API이다.
                    운영자가 현재 baseline serving 모델, fallback policy, 주요 metric을 빠르게 확인할 때 사용한다.
                    active row가 아직 없으면 404를 반환한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "현재 active registry row를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "activeRegistryRow",
                                    summary = "현재 serving baseline row 예시",
                                    value = """
                                            {
                                              "id": 1,
                                              "experimentName": "cpu_compare_medium_relabel_weighted",
                                              "modelName": "cpu_compare_medium_relabel_weighted_active5",
                                              "baseModelName": "beomi/KcELECTRA-base",
                                              "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                              "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted.json",
                                              "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5/best/label_metadata.json",
                                              "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                              "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                              "trainingDatasetTag": "emotion_mvp_relabel_weighted",
                                              "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                              "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                              "status": "ACTIVE",
                                              "active": true,
                                              "shadow": false,
                                              "accuracy": 0.4267,
                                              "macroF1": 0.3645,
                                              "happyF1": 0.6146,
                                              "calmF1": 0.2941,
                                              "anxiousF1": 0.4021,
                                              "sadF1": 0.0000,
                                              "angryF1": 0.4682,
                                              "servingNotes": "current baseline serving model",
                                              "approvalNote": "approved for serving",
                                              "rejectionReason": null,
                                              "approvedAt": "2026-03-29T10:15:30",
                                              "rejectedAt": null,
                                              "activatedAt": "2026-03-29T12:00:00",
                                              "createdAt": "2026-03-29T09:50:00",
                                              "updatedAt": "2026-03-29T12:00:00"
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "현재 active registry row가 없으면 반환한다.")
    })
    public EmotionModelRegistryResponse getActive() {
        return emotionModelRegistryService.getActive();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "감정 모델 registry 단건 조회",
            description = """
                    특정 registry id의 현재 snapshot을 조회하는 API이다.
                    목록에서 찾은 row를 더 자세히 보거나, activate/status 변경 전 현재 상태를 재확인할 때 사용한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "해당 registry row를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "approvedShadowCandidateRow",
                                    summary = "승인된 shadow lineage row 예시",
                                    value = """
                                            {
                                              "id": 2,
                                              "experimentName": "cpu_compare_medium_manual_seed_v2",
                                              "modelName": "cpu_compare_medium_manual_seed_v2_active5",
                                              "baseModelName": "beomi/KcELECTRA-base",
                                              "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2",
                                              "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json",
                                              "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2/best/label_metadata.json",
                                              "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                              "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                              "trainingDatasetTag": "emotion_mvp_manual_seed_v2",
                                              "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                              "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                              "status": "APPROVED",
                                              "active": false,
                                              "shadow": true,
                                              "accuracy": 0.4427,
                                              "macroF1": 0.3888,
                                              "happyF1": 0.6250,
                                              "calmF1": 0.3182,
                                              "anxiousF1": 0.4021,
                                              "sadF1": 0.3564,
                                              "angryF1": 0.2423,
                                              "servingNotes": "best macro F1 but watch HAPPY->CALM collapse",
                                              "approvalNote": "approved as shadow candidate only",
                                              "rejectionReason": null,
                                              "approvedAt": "2026-03-29T15:10:00",
                                              "rejectedAt": null,
                                              "activatedAt": null,
                                              "createdAt": "2026-03-29T14:55:00",
                                              "updatedAt": "2026-03-29T15:10:00"
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환한다.")
    })
    public EmotionModelRegistryResponse getById(
            @Parameter(description = "조회할 registry id", example = "1")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getById(id);
    }

    @GetMapping("/{id}/history")
    @Operation(
            summary = "감정 모델 registry 상태 이력 조회",
            description = """
                    특정 registry row의 상태 변경 이력을 최신순으로 조회하는 API이다.
                    APPROVED, ACTIVE, REJECTED, SHADOW 전이와 changeReason을 함께 보면서 운영 판단 근거를 확인할 때 사용한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "해당 registry row의 상태 변경 이력을 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "statusHistoryList",
                                    summary = "상태 변경 이력 목록 예시",
                                    value = """
                                            [
                                              {
                                                "id": 2,
                                                "registryId": 1,
                                                "fromStatus": "APPROVED",
                                                "toStatus": "ACTIVE",
                                                "changeReason": "activated",
                                                "changedAt": "2026-03-29T12:00:00"
                                              },
                                              {
                                                "id": 1,
                                                "registryId": 1,
                                                "fromStatus": null,
                                                "toStatus": "APPROVED",
                                                "changeReason": "created",
                                                "changedAt": "2026-03-29T10:15:30"
                                              }
                                            ]
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환한다.")
    })
    public List<EmotionModelRegistryStatusHistoryResponse> getStatusHistory(
            @Parameter(description = "이력을 조회할 registry id", example = "1")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getStatusHistory(id);
    }

    @GetMapping("/{id}/transitions")
    @Operation(
            summary = "감정 모델 registry 가능 전이 조회",
            description = """
                    운영자가 특정 row에서 어떤 다음 상태 변경이 가능한지와 activate 가능 여부를 확인하는 API다.
                    admin UI나 운영 스크립트가 전이 버튼을 제어할 때 기준으로 사용할 수 있다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "해당 row의 허용 전이 목록과 activate 가능 여부를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "allowedTransitions",
                                    summary = "SHADOW row 전이 예시",
                                    value = """
                                            {
                                              "registryId": 2,
                                              "currentStatus": "SHADOW",
                                              "active": false,
                                              "shadow": true,
                                              "allowedStatusUpdates": ["APPROVED", "REJECTED", "ARCHIVED"],
                                              "canActivate": false
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환된다.")
    })
    public EmotionModelRegistryAvailableTransitionsResponse getAvailableTransitions(
            @Parameter(description = "가능 전이를 조회할 registry id", example = "2")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getAvailableTransitions(id);
    }

    @GetMapping("/active/runtime-alignment")
    @Operation(
            summary = "active registry와 FastAPI runtime 경로 정합성 점검",
            description = """
                    현재 active registry row의 `artifactDir`와 FastAPI serving runtime의 실제 `EMOTION_MODEL_DIR` 절대 경로를 비교하는 API이다.
                    운영자가 baseline activate 이후 FastAPI가 같은 artifact 디렉터리를 바라보는지 바로 확인할 때 사용한다.
                    FastAPI 쪽 `/internal/model/runtime-info` 결과와 active registry snapshot을 함께 묶어 반환한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "active registry와 FastAPI runtime 경로 비교 결과를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "runtimeAlignment",
                                    summary = "active runtime alignment 예시",
                                    value = """
                                            {
                                              "activeRegistryId": 1,
                                              "activeExperimentName": "cpu_compare_medium_relabel_weighted",
                                              "registryArtifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                              "fastApiBaseUrl": "http://localhost:8002",
                                              "runtimeModelDirConfigured": "ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                              "runtimeModelDirResolved": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                              "runtimeModelDirExists": true,
                                              "runtimeModelLoadSource": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                              "runtimeLabelMapPathConfigured": "ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                              "runtimeLabelMapPathResolved": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                              "runtimeLabelMapPathExists": true,
                                              "runtimeModelName": "beomi/KcELECTRA-base",
                                              "runtimeMaxLength": 128,
                                              "artifactDirAligned": true,
                                              "overallAligned": true,
                                              "detail": "Active registry artifact dir matches FastAPI runtime model dir"
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "active registry row가 없으면 반환한다."),
            @ApiResponse(responseCode = "502", description = "FastAPI runtime info를 읽지 못하면 반환한다.")
    })
    public EmotionModelRegistryRuntimeAlignmentResponse getActiveRuntimeAlignment() {
        return emotionModelRegistryService.getActiveRuntimeAlignment();
    }

    @GetMapping("/{id}/artifact-health")
    @Operation(
            summary = "감정 모델 registry artifact health check",
            description = """
                    registry row에 저장된 artifact 경로들이 현재 파일시스템에서 실제로 존재하는지 점검하는 API다.
                    운영자가 activate 전후로 artifact 디렉터리와 metrics JSON, metadata 파일 경로를 빠르게 검증할 때 사용한다.
                    필수 경로는 `artifactDir`, `metricsJsonPath`이고, 나머지 경로는 설정되어 있으면 함께 점검한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "artifact 경로 점검 결과를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "artifactHealth",
                                    summary = "artifact health check 예시",
                                    value = """
                                            {
                                              "registryId": 1,
                                              "experimentName": "cpu_compare_medium_relabel_weighted",
                                              "status": "ACTIVE",
                                              "requiredArtifactsHealthy": true,
                                              "overallHealthy": true,
                                              "missingRequiredItems": [],
                                              "missingOptionalItems": [],
                                              "items": [
                                                {
                                                  "name": "artifactDir",
                                                  "path": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                                                  "required": true,
                                                  "configured": true,
                                                  "exists": true,
                                                  "directoryExpected": true,
                                                  "directory": true,
                                                  "errorMessage": null
                                                },
                                                {
                                                  "name": "metricsJsonPath",
                                                  "path": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted.json",
                                                  "required": true,
                                                  "configured": true,
                                                  "exists": true,
                                                  "directoryExpected": false,
                                                  "directory": false,
                                                  "errorMessage": null
                                                }
                                              ]
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환된다.")
    })
    public EmotionModelRegistryArtifactHealthResponse getArtifactHealth(
            @Parameter(description = "artifact 상태를 점검할 registry id", example = "1")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getArtifactHealth(id);
    }

    @GetMapping("/{id}/artifact-json-check")
    @Operation(
            summary = "감정 모델 registry artifact JSON parse/schema check",
            description = """
                    registry row에 저장된 JSON artifact 경로를 실제로 읽어서 파싱 가능 여부와 최소 스키마를 점검하는 API다.
                    운영자가 activate 전후로 metrics, label metadata, training config, label map JSON이 깨지지 않았는지 확인할 때 사용한다.
                    필수 JSON은 `metricsJsonPath`이고, 나머지 JSON은 경로가 설정되어 있으면 함께 검사한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "JSON artifact parse/schema check 결과를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "artifactJsonCheck",
                                    summary = "artifact JSON check 예시",
                                    value = """
                                            {
                                              "registryId": 1,
                                              "experimentName": "cpu_compare_medium_relabel_weighted",
                                              "status": "ACTIVE",
                                              "parseHealthy": true,
                                              "requiredSchemaHealthy": true,
                                              "overallSchemaHealthy": true,
                                              "failedParseItems": [],
                                              "failedSchemaItems": [],
                                              "items": [
                                                {
                                                  "name": "metricsJsonPath",
                                                  "path": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted.json",
                                                  "required": true,
                                                  "configured": true,
                                                  "exists": true,
                                                  "parseable": true,
                                                  "schemaValid": true,
                                                  "requiredKeys": ["classification_report", "confusion_matrix", "classification_report.accuracy", "classification_report.macro avg"],
                                                  "missingKeys": [],
                                                  "errorMessage": null
                                                }
                                              ]
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환된다.")
    })
    public EmotionModelRegistryArtifactJsonCheckResponse getArtifactJsonCheck(
            @Parameter(description = "JSON parse/schema check를 수행할 registry id", example = "1")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getArtifactJsonCheck(id);
    }

    @GetMapping("/{id}/promotion-checklist")
    @Operation(
            summary = "감정 모델 promotion checklist validation",
            description = """
                    registry row를 운영 승격 관점에서 점검하는 checklist API다.
                    artifact health, JSON parse/schema, fallback policy, active label contract, baseline 대비 metric gate를 묶어서
                    `BLOCKED`, `SHADOW_ONLY`, `ACTIVE_CANDIDATE`, `ACTIVE_ALREADY` 중 하나의 추천 결과를 반환한다.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "promotion checklist 검증 결과를 반환한다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "promotionChecklist",
                                    summary = "promotion checklist 예시",
                                    value = """
                                            {
                                              "registryId": 2,
                                              "experimentName": "cpu_compare_medium_manual_seed_v2",
                                              "status": "APPROVED",
                                              "baselineRegistryId": 1,
                                              "baselineExperimentName": "cpu_compare_medium_relabel_weighted",
                                              "recommendation": "SHADOW_ONLY",
                                              "readyForShadow": true,
                                              "readyForActive": false,
                                              "passedCount": 8,
                                              "totalCount": 10,
                                              "items": [
                                                {
                                                  "name": "happyF1Gate",
                                                  "passed": false,
                                                  "detail": "happyF1 candidate=0.2000, baseline=0.6146"
                                                }
                                              ]
                                            }
                                            """))),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환된다.")
    })
    public EmotionModelRegistryPromotionChecklistResponse getPromotionChecklist(
            @Parameter(description = "promotion checklist를 검증할 registry id", example = "2")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.getPromotionChecklist(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "감정 모델 registry 등록",
            description = """
                    새 experiment/artifact 메타데이터를 registry에 등록하는 API이다.
                    기본 status는 TRAINED이며, ACTIVE 직접 생성은 허용하지 않는다.
                    serving 전환이 필요하면 등록 후 activate API를 별도로 호출해야 한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "registry row가 생성되었다."),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 status이거나 ACTIVE 직접 생성 요청이면 반환한다.")
    })
    public EmotionModelRegistryResponse create(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "훈련 또는 검토가 끝난 emotion model artifact 메타데이터 등록 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "trainedCandidate",
                                    summary = "TRAINED shadow candidate 등록 예시",
                                    value = """
                                            {
                                              "experimentName": "cpu_compare_medium_manual_seed_v2",
                                              "modelName": "cpu_compare_medium_manual_seed_v2_active5",
                                              "baseModelName": "beomi/KcELECTRA-base",
                                              "artifactDir": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2",
                                              "metricsJsonPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json",
                                              "labelMetadataPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2/best/label_metadata.json",
                                              "trainingConfigPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json",
                                              "labelMapPath": "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                                              "trainingDatasetTag": "emotion_mvp_manual_seed_v2",
                                              "validationDatasetTag": "emotion_mvp_cpu_compare_medium",
                                              "fallbackPolicy": "TIRED_FALLBACK_ONLY",
                                              "status": "TRAINED",
                                              "isShadow": true,
                                              "accuracy": 0.4427,
                                              "macroF1": 0.3888,
                                              "happyF1": 0.6250,
                                              "calmF1": 0.3182,
                                              "anxiousF1": 0.4021,
                                              "sadF1": 0.3564,
                                              "angryF1": 0.2423,
                                              "servingNotes": "best macro F1 but watch HAPPY->CALM collapse",
                                              "approvalNote": "approved as shadow candidate only"
                                            }
                                            """)))
            @org.springframework.web.bind.annotation.RequestBody CreateEmotionModelRegistryRequest request
    ) {
        return emotionModelRegistryService.create(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "감정 모델 registry 상태 변경",
            description = """
                    특정 registry row를 APPROVED, REJECTED, SHADOW, ARCHIVED 같은 비활성 상태로 변경하는 API이다.
                    ACTIVE 전환은 이 API가 아니라 activate API로만 처리한다.
                    현재 active row를 다른 상태로 바꾸려 하면 400을 반환한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 후 최신 registry row를 반환한다."),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 status이거나 ACTIVE 전환/활성 row 직접 변경이면 반환한다."),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환한다.")
    })
    public EmotionModelRegistryResponse updateStatus(
            @Parameter(description = "상태를 변경할 registry id", example = "2")
            @PathVariable Long id,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "registry 상태 변경 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "approveCandidate",
                                            summary = "APPROVED 전환 예시",
                                            value = """
                                                    {
                                                      "status": "APPROVED",
                                                      "approvalNote": "fixed compare gate reviewed and approved for manual activation",
                                                      "servingNotes": "keep TIRED fallback-only"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "rejectCandidate",
                                            summary = "REJECTED 전환 예시",
                                            value = """
                                                    {
                                                      "status": "REJECTED",
                                                      "rejectionReason": "fixed compare gate did not beat current baseline",
                                                      "servingNotes": "do not promote to serving"
                                                    }
                                                    """)
                            }))
            @org.springframework.web.bind.annotation.RequestBody UpdateEmotionModelRegistryStatusRequest request
    ) {
        return emotionModelRegistryService.updateStatus(id, request);
    }

    @PostMapping("/{id}/activate")
    @Operation(
            summary = "감정 모델 registry 활성화",
            description = """
                    특정 registry row를 현재 active serving row로 승격하는 API이다.
                    기존 active row가 있으면 APPROVED로 내리고, 대상 row는 ACTIVE와 isActive=true로 전환한다.
                    운영자가 baseline 교체를 수행할 때 마지막 단계로 사용한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "활성화 후 최신 registry row를 반환한다."),
            @ApiResponse(responseCode = "404", description = "해당 id의 registry row가 없으면 반환한다.")
    })
    public EmotionModelRegistryResponse activate(
            @Parameter(description = "active serving row로 승격할 registry id", example = "1")
            @PathVariable Long id
    ) {
        return emotionModelRegistryService.activate(id);
    }
}
