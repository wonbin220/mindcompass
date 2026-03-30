// 감정 모델 registry 서비스의 상태 이력 조회 규칙을 검증하는 테스트다.
package com.mindcompass.aiapi.registry.service;

import com.mindcompass.aiapi.registry.client.EmotionModelServingRuntimeInfoClient;
import com.mindcompass.aiapi.registry.client.EmotionModelServingRuntimeInfoResponse;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryRecord;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatus;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatusHistoryRecord;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAdminSummaryResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAvailableTransitionsResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactHealthResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactJsonCheckResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryPromotionChecklistResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryRuntimeAlignmentResponse;
import com.mindcompass.aiapi.registry.dto.UpdateEmotionModelRegistryStatusRequest;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryStatusHistoryResponse;
import com.mindcompass.aiapi.registry.repository.EmotionModelRegistryRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EmotionModelRegistryServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private EmotionModelRegistryRepository repository;

    @Mock
    private EmotionModelServingRuntimeInfoClient servingRuntimeInfoClient;

    @InjectMocks
    private EmotionModelRegistryService service;

    @Test
    void getAllPassesFiltersToRepository() {
        given(repository.findAll(EmotionModelRegistryStatus.ACTIVE, true, false, "cpu_compare", null))
                .willReturn(List.of(sampleRegistryRecord()));

        var responses = service.getAll("active", true, false, "cpu_compare", null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo("ACTIVE");
        then(repository).should().findAll(EmotionModelRegistryStatus.ACTIVE, true, false, "cpu_compare", null);
    }

    @Test
    void getAllThrowsBadRequestWhenStatusFilterIsUnsupported() {
        assertThatThrownBy(() -> service.getAll("unknown-status", null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void getAllPassesShadowFilterToRepository() {
        given(repository.findAll(null, null, true, null, null))
                .willReturn(List.of(shadowRegistryRecord()));

        var responses = service.getAll(null, null, true, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().isShadow()).isTrue();
        then(repository).should().findAll(null, null, true, null, null);
    }

    @Test
    void getAllPassesCurrentShadowOnlyFilterToRepository() {
        given(repository.findAll(null, null, true, null, true))
                .willReturn(List.of(shadowRegistryRecord()));

        var responses = service.getAll(null, null, true, null, true);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo("SHADOW");
        then(repository).should().findAll(null, null, true, null, true);
    }

    @Test
    void getAllPassesCurrentShadowOnlyFalseFilterToRepository() {
        given(repository.findAll(null, null, true, null, false))
                .willReturn(List.of(approvedShadowRegistryRecord()));

        var responses = service.getAll(null, null, true, null, false);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo("APPROVED");
        assertThat(responses.getFirst().isShadow()).isTrue();
        then(repository).should().findAll(null, null, true, null, false);
    }

    @Test
    void getByIdReturnsRegistryWhenItExists() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleRegistryRecord()));

        var response = service.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.experimentName()).isEqualTo("cpu_compare_medium_relabel_weighted");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void getByIdThrowsNotFoundWhenRegistryDoesNotExist() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void getStatusHistoryReturnsHistoryWhenRegistryExists() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 17, 0, 5);
        given(repository.findById(1L)).willReturn(Optional.of(sampleRegistryRecord()));
        given(repository.findStatusHistoryByRegistryId(1L)).willReturn(List.of(
                new EmotionModelRegistryStatusHistoryRecord(2L, 1L, "APPROVED", "ACTIVE", "activated", now),
                new EmotionModelRegistryStatusHistoryRecord(1L, 1L, null, "APPROVED", "created", now.minusSeconds(1))
        ));

        List<EmotionModelRegistryStatusHistoryResponse> responses = service.getStatusHistory(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().toStatus()).isEqualTo("ACTIVE");
        assertThat(responses.getFirst().changeReason()).isEqualTo("activated");
        assertThat(responses.get(1).fromStatus()).isNull();
        assertThat(responses.get(1).toStatus()).isEqualTo("APPROVED");
    }

    @Test
    void getStatusHistoryThrowsNotFoundWhenRegistryDoesNotExist() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatusHistory(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void getSummaryAggregatesCountsAndActiveRow() {
        given(repository.findStatusCounts()).willReturn(List.of(
                new EmotionModelRegistryRepository.RegistryStatusCountRow("ACTIVE", 1L),
                new EmotionModelRegistryRepository.RegistryStatusCountRow("APPROVED", 1L),
                new EmotionModelRegistryRepository.RegistryStatusCountRow("REJECTED", 1L),
                new EmotionModelRegistryRepository.RegistryStatusCountRow("SHADOW", 1L)
        ));
        given(repository.countAll()).willReturn(4L);
        given(repository.countShadowLineage()).willReturn(3L);
        given(repository.findActive()).willReturn(Optional.of(sampleRegistryRecord()));

        EmotionModelRegistryAdminSummaryResponse response = service.getSummary();

        assertThat(response.totalCount()).isEqualTo(4L);
        assertThat(response.activeCount()).isEqualTo(1L);
        assertThat(response.approvedCount()).isEqualTo(1L);
        assertThat(response.rejectedCount()).isEqualTo(1L);
        assertThat(response.shadowCount()).isEqualTo(1L);
        assertThat(response.shadowLineageCount()).isEqualTo(3L);
        assertThat(response.activeRegistryId()).isEqualTo(1L);
        assertThat(response.activeExperimentName()).isEqualTo("cpu_compare_medium_relabel_weighted");
    }

    @Test
    void getAvailableTransitionsReturnsAllowedStatusesAndActivationFlag() {
        given(repository.findById(2L)).willReturn(Optional.of(shadowRegistryRecord()));

        EmotionModelRegistryAvailableTransitionsResponse response = service.getAvailableTransitions(2L);

        assertThat(response.registryId()).isEqualTo(2L);
        assertThat(response.currentStatus()).isEqualTo("SHADOW");
        assertThat(response.allowedStatusUpdates()).containsExactly("APPROVED", "REJECTED", "ARCHIVED");
        assertThat(response.canActivate()).isFalse();
    }

    @Test
    void getActiveRuntimeAlignmentReturnsAlignedWhenPathsMatch() {
        given(repository.findActive()).willReturn(Optional.of(runtimeAlignedActiveRegistryRecord()));
        given(servingRuntimeInfoClient.getRuntimeInfo()).willReturn(new EmotionModelServingRuntimeInfoResponse(
                "ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                true,
                "ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                true,
                "beomi/KcELECTRA-base",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                128
        ));
        given(servingRuntimeInfoClient.getFastApiBaseUrl()).willReturn("http://localhost:8002");

        EmotionModelRegistryRuntimeAlignmentResponse response = service.getActiveRuntimeAlignment();

        assertThat(response.activeRegistryId()).isEqualTo(1L);
        assertThat(response.artifactDirAligned()).isTrue();
        assertThat(response.overallAligned()).isTrue();
        assertThat(response.detail()).contains("matches");
    }

    @Test
    void getActiveRuntimeAlignmentReturnsMismatchWhenRuntimePathDiffers() {
        given(repository.findActive()).willReturn(Optional.of(runtimeAlignedActiveRegistryRecord()));
        given(servingRuntimeInfoClient.getRuntimeInfo()).willReturn(new EmotionModelServingRuntimeInfoResponse(
                "ai-api-fastapi/training/emotion_classifier/artifacts/best",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/best",
                true,
                "ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
                true,
                "beomi/KcELECTRA-base",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/best",
                128
        ));
        given(servingRuntimeInfoClient.getFastApiBaseUrl()).willReturn("http://localhost:8002");

        EmotionModelRegistryRuntimeAlignmentResponse response = service.getActiveRuntimeAlignment();

        assertThat(response.artifactDirAligned()).isFalse();
        assertThat(response.overallAligned()).isFalse();
        assertThat(response.detail()).contains("does not match");
    }

    @Test
    void getArtifactHealthReturnsHealthyWhenConfiguredPathsExist() throws Exception {
        EmotionModelRegistryRecord record = artifactHealthyRegistryRecord();
        given(repository.findById(10L)).willReturn(Optional.of(record));

        EmotionModelRegistryArtifactHealthResponse response = service.getArtifactHealth(10L);

        assertThat(response.registryId()).isEqualTo(10L);
        assertThat(response.requiredArtifactsHealthy()).isTrue();
        assertThat(response.overallHealthy()).isTrue();
        assertThat(response.missingRequiredItems()).isEmpty();
        assertThat(response.missingOptionalItems()).isEmpty();
        assertThat(response.items()).hasSize(5);
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("artifactDir"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.exists()).isTrue();
                    assertThat(item.directoryExpected()).isTrue();
                    assertThat(item.directory()).isTrue();
                });
    }

    @Test
    void getArtifactHealthReturnsMissingListsWhenPathsDoNotExist() throws Exception {
        given(repository.findById(11L)).willReturn(Optional.of(artifactMissingRegistryRecord()));

        EmotionModelRegistryArtifactHealthResponse response = service.getArtifactHealth(11L);

        assertThat(response.requiredArtifactsHealthy()).isFalse();
        assertThat(response.overallHealthy()).isFalse();
        assertThat(response.missingRequiredItems()).containsExactly("artifactDir", "metricsJsonPath");
        assertThat(response.missingOptionalItems()).containsExactly("labelMetadataPath");
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("trainingConfigPath"))
                .singleElement()
                .satisfies(item -> assertThat(item.configured()).isFalse());
    }

    @Test
    void getArtifactJsonCheckReturnsHealthyWhenJsonMatchesExpectedShape() throws Exception {
        given(repository.findById(12L)).willReturn(Optional.of(artifactJsonHealthyRegistryRecord()));

        EmotionModelRegistryArtifactJsonCheckResponse response = service.getArtifactJsonCheck(12L);

        assertThat(response.registryId()).isEqualTo(12L);
        assertThat(response.parseHealthy()).isTrue();
        assertThat(response.requiredSchemaHealthy()).isTrue();
        assertThat(response.overallSchemaHealthy()).isTrue();
        assertThat(response.failedParseItems()).isEmpty();
        assertThat(response.failedSchemaItems()).isEmpty();
    }

    @Test
    void getArtifactJsonCheckCollectsParseAndSchemaFailures() throws Exception {
        given(repository.findById(13L)).willReturn(Optional.of(artifactJsonBrokenRegistryRecord()));

        EmotionModelRegistryArtifactJsonCheckResponse response = service.getArtifactJsonCheck(13L);

        assertThat(response.parseHealthy()).isFalse();
        assertThat(response.requiredSchemaHealthy()).isFalse();
        assertThat(response.overallSchemaHealthy()).isFalse();
        assertThat(response.failedParseItems()).contains("metricsJsonPath");
        assertThat(response.failedSchemaItems()).contains("metricsJsonPath", "labelMetadataPath");
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("labelMetadataPath"))
                .singleElement()
                .satisfies(item -> assertThat(item.missingKeys()).contains("active_labels", "num_labels"));
    }

    @Test
    void getPromotionChecklistReturnsActiveAlreadyForCurrentBaseline() throws Exception {
        EmotionModelRegistryRecord baseline = promotionBaselineRegistryRecord();
        given(repository.findById(20L)).willReturn(Optional.of(baseline));
        given(repository.findActive()).willReturn(Optional.of(baseline));

        EmotionModelRegistryPromotionChecklistResponse response = service.getPromotionChecklist(20L);

        assertThat(response.recommendation()).isEqualTo("ACTIVE_ALREADY");
        assertThat(response.readyForShadow()).isTrue();
        assertThat(response.readyForActive()).isTrue();
    }

    @Test
    void getPromotionChecklistReturnsShadowOnlyWhenHappyGateFails() throws Exception {
        EmotionModelRegistryRecord candidate = promotionCandidateRegistryRecord();
        EmotionModelRegistryRecord baseline = promotionBaselineRegistryRecord();
        given(repository.findById(21L)).willReturn(Optional.of(candidate));
        given(repository.findActive()).willReturn(Optional.of(baseline));

        EmotionModelRegistryPromotionChecklistResponse response = service.getPromotionChecklist(21L);

        assertThat(response.recommendation()).isEqualTo("SHADOW_ONLY");
        assertThat(response.readyForShadow()).isTrue();
        assertThat(response.readyForActive()).isFalse();
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("happyF1Gate"))
                .singleElement()
                .satisfies(item -> assertThat(item.passed()).isFalse());
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("happyToCalmGate"))
                .singleElement()
                .satisfies(item -> assertThat(item.passed()).isTrue());
        assertThat(response.items())
                .filteredOn(item -> item.name().equals("errorSampleCsvGate"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.passed()).isFalse();
                    assertThat(item.detail()).contains("reviewedRows=0");
                });
    }

    @Test
    void updateStatusPromotesShadowToApprovedAndStoresHistoryReason() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "APPROVED",
                "Approved after shadow observation",
                null,
                "keep inactive"
        );
        given(repository.findById(2L))
                .willReturn(Optional.of(shadowRegistryRecord()))
                .willReturn(Optional.of(approvedShadowRegistryRecord()));

        var response = service.updateStatus(2L, request);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.isShadow()).isTrue();
        assertThat(response.approvedAt()).isNotNull();

        ArgumentCaptor<LocalDateTime> approvedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(repository).should().updateStatus(
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(EmotionModelRegistryStatus.APPROVED),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq("Approved after shadow observation"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("keep inactive"),
                approvedAtCaptor.capture(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
        then(repository).should().insertStatusHistory(
                2L,
                "SHADOW",
                "APPROVED",
                "Approved after shadow observation"
        );
        assertThat(approvedAtCaptor.getValue()).isNotNull();
    }

    @Test
    void updateStatusRejectsShadowAndUsesRejectionReasonForHistory() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "REJECTED",
                null,
                "Rejected after shadow review",
                "do not activate"
        );
        given(repository.findById(3L))
                .willReturn(Optional.of(shadowFixedCompareRegistryRecord()))
                .willReturn(Optional.of(rejectedShadowRegistryRecord()));

        var response = service.updateStatus(3L, request);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.isShadow()).isTrue();
        assertThat(response.rejectedAt()).isNotNull();

        ArgumentCaptor<LocalDateTime> rejectedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(repository).should().updateStatus(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(EmotionModelRegistryStatus.REJECTED),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("Rejected after shadow review"),
                org.mockito.ArgumentMatchers.eq("do not activate"),
                org.mockito.ArgumentMatchers.isNull(),
                rejectedAtCaptor.capture(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
        then(repository).should().insertStatusHistory(
                3L,
                "SHADOW",
                "REJECTED",
                "Rejected after shadow review"
        );
        assertThat(rejectedAtCaptor.getValue()).isNotNull();
    }

    @Test
    void updateStatusMovesApprovedRowToShadowAndMarksShadowLineage() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "SHADOW",
                null,
                null,
                "observe happy calm boundary"
        );
        given(repository.findById(4L))
                .willReturn(Optional.of(approvedNonShadowRegistryRecord()))
                .willReturn(Optional.of(shadowPromotedRegistryRecord()));

        var response = service.updateStatus(4L, request);

        assertThat(response.status()).isEqualTo("SHADOW");
        assertThat(response.isShadow()).isTrue();
        then(repository).should().updateStatus(
                org.mockito.ArgumentMatchers.eq(4L),
                org.mockito.ArgumentMatchers.eq(EmotionModelRegistryStatus.SHADOW),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("observe happy calm boundary"),
                org.mockito.ArgumentMatchers.eq(approvedNonShadowRegistryRecord().approvedAt()),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    void updateStatusRejectsUnsupportedTransitionFromRejectedToApproved() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "APPROVED",
                "retry approval",
                null,
                "retry"
        );
        given(repository.findById(3L)).willReturn(Optional.of(rejectedShadowRegistryRecord()));

        assertThatThrownBy(() -> service.updateStatus(3L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).contains("REJECTED -> APPROVED");
                });
    }

    @Test
    void updateStatusRequiresApprovalNoteWhenMovingToApproved() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "APPROVED",
                " ",
                null,
                "keep inactive"
        );
        given(repository.findById(2L)).willReturn(Optional.of(shadowRegistryRecord()));

        assertThatThrownBy(() -> service.updateStatus(2L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).contains("approvalNote");
                });
    }

    @Test
    void updateStatusRequiresRejectionReasonWhenMovingToRejected() {
        UpdateEmotionModelRegistryStatusRequest request = new UpdateEmotionModelRegistryStatusRequest(
                "REJECTED",
                null,
                " ",
                "do not activate"
        );
        given(repository.findById(2L)).willReturn(Optional.of(shadowRegistryRecord()));

        assertThatThrownBy(() -> service.updateStatus(2L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).contains("rejectionReason");
                });
    }

    @Test
    void activateRequiresApprovedStatus() {
        given(repository.findById(2L)).willReturn(Optional.of(shadowRegistryRecord()));

        assertThatThrownBy(() -> service.activate(2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).contains("Only APPROVED");
                });
    }

    @Test
    void activateReturnsCurrentRowWhenAlreadyActive() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleRegistryRecord()));

        var response = service.activate(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        then(repository).should(never()).activate(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
        then(repository).should(never()).insertStatusHistory(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void activateBlocksApprovedRowWhenPromotionChecklistIsNotActiveCandidate() throws Exception {
        EmotionModelRegistryRecord candidate = promotionCandidateRegistryRecord();
        EmotionModelRegistryRecord baseline = promotionBaselineRegistryRecord();
        given(repository.findById(21L)).willReturn(Optional.of(candidate));
        given(repository.findActive()).willReturn(Optional.of(baseline));

        assertThatThrownBy(() -> service.activate(21L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).contains("Activation blocked by promotion checklist");
                    assertThat(responseStatusException.getReason()).contains("recommendation=SHADOW_ONLY");
                    assertThat(responseStatusException.getReason()).contains("happyF1Gate");
                    assertThat(responseStatusException.getReason()).contains("errorSampleCsvGate");
                });

        then(repository).should(never()).activate(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    void activateAllowsApprovedRowWhenPromotionChecklistIsActiveCandidate() throws Exception {
        EmotionModelRegistryRecord candidate = promotionActivatableCandidateRegistryRecord();
        EmotionModelRegistryRecord baseline = promotionBaselineRegistryRecord();
        EmotionModelRegistryRecord activeAfterActivation = activatedPromotionCandidateRegistryRecord();
        given(repository.findById(30L))
                .willReturn(Optional.of(candidate))
                .willReturn(Optional.of(candidate))
                .willReturn(Optional.of(candidate))
                .willReturn(Optional.of(activeAfterActivation));
        given(repository.findActive()).willReturn(Optional.of(baseline));

        var response = service.activate(30L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.isActive()).isTrue();
        then(repository).should().deactivateCurrentActive(org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        then(repository).should().activate(
                org.mockito.ArgumentMatchers.eq(30L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
        then(repository).should().insertStatusHistory(
                20L,
                "ACTIVE",
                "APPROVED",
                "deactivated by activating registry id 30"
        );
        then(repository).should().insertStatusHistory(
                30L,
                "APPROVED",
                "ACTIVE",
                "activated"
        );
    }

    private EmotionModelRegistryRecord sampleRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 17, 0, 3);
        return new EmotionModelRegistryRecord(
                1L,
                "cpu_compare_medium_relabel_weighted",
                "cpu_compare_medium_relabel_weighted_active5",
                "beomi/KcELECTRA-base",
                "artifact-dir",
                "metrics-path",
                "label-metadata-path",
                "training-config-path",
                "label-map-path",
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.ACTIVE,
                true,
                false,
                BigDecimal.valueOf(0.4267),
                BigDecimal.valueOf(0.3645),
                BigDecimal.valueOf(0.6146),
                BigDecimal.valueOf(0.2941),
                BigDecimal.valueOf(0.4457),
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.4682),
                "serving-notes",
                "approval-note",
                null,
                now,
                null,
                now.plusSeconds(2),
                now,
                now.plusSeconds(2)
        );
    }

    private EmotionModelRegistryRecord runtimeAlignedActiveRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 17, 0, 3);
        return new EmotionModelRegistryRecord(
                1L,
                "cpu_compare_medium_relabel_weighted",
                "cpu_compare_medium_relabel_weighted_active5",
                "beomi/KcELECTRA-base",
                "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5",
                "metrics-path",
                "label-metadata-path",
                "training-config-path",
                "label-map-path",
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.ACTIVE,
                true,
                false,
                BigDecimal.valueOf(0.4267),
                BigDecimal.valueOf(0.3645),
                BigDecimal.valueOf(0.6146),
                BigDecimal.valueOf(0.2941),
                BigDecimal.valueOf(0.4457),
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.4682),
                "serving-notes",
                "approval-note",
                null,
                now,
                null,
                now.plusSeconds(2),
                now,
                now.plusSeconds(2)
        );
    }

    private EmotionModelRegistryRecord shadowRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 10, 0);
        return new EmotionModelRegistryRecord(
                2L,
                "cpu_compare_medium_shadow_candidate",
                "cpu_compare_medium_shadow_candidate_active5",
                "beomi/KcELECTRA-base",
                "shadow-artifact-dir",
                "shadow-metrics-path",
                "shadow-label-metadata-path",
                "shadow-training-config-path",
                "shadow-label-map-path",
                "emotion_mvp_shadow_candidate",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.SHADOW,
                false,
                true,
                BigDecimal.valueOf(0.4100),
                BigDecimal.valueOf(0.3550),
                BigDecimal.valueOf(0.5900),
                BigDecimal.valueOf(0.2800),
                BigDecimal.valueOf(0.4300),
                BigDecimal.valueOf(0.1000),
                BigDecimal.valueOf(0.4200),
                "shadow-serving-notes",
                "shadow-approval-note",
                null,
                now,
                null,
                null,
                now,
                now.plusSeconds(5)
        );
    }

    private EmotionModelRegistryRecord approvedShadowRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 15, 0);
        return new EmotionModelRegistryRecord(
                2L,
                "cpu_compare_medium_manual_seed_v2",
                "cpu_compare_medium_manual_seed_v2",
                "beomi/KcELECTRA-base",
                "shadow-artifact-dir",
                "shadow-metrics-path",
                "shadow-label-metadata-path",
                "shadow-training-config-path",
                "shadow-label-map-path",
                "emotion_mvp_manual_seed_v2",
                "emotion_mvp_manual_seed_v2",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                true,
                BigDecimal.valueOf(0.4427),
                BigDecimal.valueOf(0.3888),
                BigDecimal.valueOf(0.2000),
                BigDecimal.valueOf(0.6318),
                BigDecimal.valueOf(0.2957),
                BigDecimal.valueOf(0.5000),
                BigDecimal.valueOf(0.3167),
                "keep inactive",
                "Approved after shadow observation",
                null,
                now,
                null,
                null,
                now.minusMinutes(10),
                now
        );
    }

    private EmotionModelRegistryRecord shadowFixedCompareRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 20, 0);
        return new EmotionModelRegistryRecord(
                3L,
                "cpu_compare_medium_manual_seed_v2_fixed_compare",
                "cpu_compare_medium_manual_seed_v2_fixed_compare",
                "beomi/KcELECTRA-base",
                "fixed-compare-artifact-dir",
                "fixed-compare-metrics-path",
                "fixed-compare-label-metadata-path",
                "fixed-compare-training-config-path",
                "fixed-compare-label-map-path",
                "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.SHADOW,
                false,
                true,
                BigDecimal.valueOf(0.3920),
                BigDecimal.valueOf(0.2974),
                BigDecimal.valueOf(0.0132),
                BigDecimal.valueOf(0.6087),
                BigDecimal.valueOf(0.2432),
                BigDecimal.valueOf(0.1749),
                BigDecimal.valueOf(0.4469),
                "shadow review only",
                "shadow observation only",
                null,
                null,
                null,
                null,
                now,
                now.plusSeconds(5)
        );
    }

    private EmotionModelRegistryRecord rejectedShadowRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 25, 0);
        return new EmotionModelRegistryRecord(
                3L,
                "cpu_compare_medium_manual_seed_v2_fixed_compare",
                "cpu_compare_medium_manual_seed_v2_fixed_compare",
                "beomi/KcELECTRA-base",
                "fixed-compare-artifact-dir",
                "fixed-compare-metrics-path",
                "fixed-compare-label-metadata-path",
                "fixed-compare-training-config-path",
                "fixed-compare-label-map-path",
                "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                "emotion_mvp_manual_seed_v2_fixed_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.REJECTED,
                false,
                true,
                BigDecimal.valueOf(0.3920),
                BigDecimal.valueOf(0.2974),
                BigDecimal.valueOf(0.0132),
                BigDecimal.valueOf(0.6087),
                BigDecimal.valueOf(0.2432),
                BigDecimal.valueOf(0.1749),
                BigDecimal.valueOf(0.4469),
                "do not activate",
                null,
                "Rejected after shadow review",
                null,
                now,
                null,
                now.minusMinutes(5),
                now
        );
    }

    private EmotionModelRegistryRecord approvedNonShadowRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 30, 0);
        return new EmotionModelRegistryRecord(
                4L,
                "cpu_compare_medium_guard_metric_v1",
                "cpu_compare_medium_guard_metric_v1_active5",
                "beomi/KcELECTRA-base",
                "guard-artifact-dir",
                "guard-metrics-path",
                "guard-label-metadata-path",
                "guard-training-config-path",
                "guard-label-map-path",
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                false,
                BigDecimal.valueOf(0.4210),
                BigDecimal.valueOf(0.3602),
                BigDecimal.valueOf(0.5400),
                BigDecimal.valueOf(0.3400),
                BigDecimal.valueOf(0.4012),
                BigDecimal.valueOf(0.3021),
                BigDecimal.valueOf(0.2178),
                "approved for shadow check",
                "approved for operational review",
                null,
                now,
                null,
                null,
                now.minusMinutes(15),
                now
        );
    }

    private EmotionModelRegistryRecord shadowPromotedRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 18, 31, 0);
        return new EmotionModelRegistryRecord(
                4L,
                "cpu_compare_medium_guard_metric_v1",
                "cpu_compare_medium_guard_metric_v1_active5",
                "beomi/KcELECTRA-base",
                "guard-artifact-dir",
                "guard-metrics-path",
                "guard-label-metadata-path",
                "guard-training-config-path",
                "guard-label-map-path",
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.SHADOW,
                false,
                true,
                BigDecimal.valueOf(0.4210),
                BigDecimal.valueOf(0.3602),
                BigDecimal.valueOf(0.5400),
                BigDecimal.valueOf(0.3400),
                BigDecimal.valueOf(0.4012),
                BigDecimal.valueOf(0.3021),
                BigDecimal.valueOf(0.2178),
                "observe happy calm boundary",
                "approved for operational review",
                null,
                now.minusMinutes(1),
                null,
                null,
                now.minusMinutes(16),
                now
        );
    }

    private EmotionModelRegistryRecord artifactHealthyRegistryRecord() throws Exception {
        Path artifactDir = Files.createDirectory(tempDir.resolve("artifact-dir"));
        Path metricsJsonPath = Files.createFile(tempDir.resolve("metrics.json"));
        Path labelMetadataPath = Files.createFile(tempDir.resolve("label-metadata.json"));
        Path trainingConfigPath = Files.createFile(tempDir.resolve("training-config.json"));
        Path labelMapPath = Files.createFile(tempDir.resolve("label-map.json"));
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 0, 0);
        return new EmotionModelRegistryRecord(
                10L,
                "artifact_health_ok",
                "artifact_health_ok_active5",
                "beomi/KcELECTRA-base",
                artifactDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                labelMapPath.toString(),
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                false,
                BigDecimal.valueOf(0.4200),
                BigDecimal.valueOf(0.3600),
                BigDecimal.valueOf(0.5000),
                BigDecimal.valueOf(0.3100),
                BigDecimal.valueOf(0.4000),
                BigDecimal.valueOf(0.2900),
                BigDecimal.valueOf(0.2100),
                "artifact check ready",
                "approved for artifact validation",
                null,
                now,
                null,
                null,
                now.minusMinutes(5),
                now
        );
    }

    private EmotionModelRegistryRecord artifactMissingRegistryRecord() throws Exception {
        Path labelMapPath = Files.createFile(tempDir.resolve("existing-label-map.json"));
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 5, 0);
        return new EmotionModelRegistryRecord(
                11L,
                "artifact_health_missing",
                "artifact_health_missing_active5",
                "beomi/KcELECTRA-base",
                tempDir.resolve("missing-artifact-dir").toString(),
                tempDir.resolve("missing-metrics.json").toString(),
                tempDir.resolve("missing-label-metadata.json").toString(),
                null,
                labelMapPath.toString(),
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.REJECTED,
                false,
                false,
                BigDecimal.valueOf(0.3900),
                BigDecimal.valueOf(0.3000),
                BigDecimal.valueOf(0.2000),
                BigDecimal.valueOf(0.6000),
                BigDecimal.valueOf(0.2400),
                BigDecimal.valueOf(0.1700),
                BigDecimal.valueOf(0.4400),
                "artifact path broken",
                null,
                "missing files",
                null,
                now,
                null,
                now.minusMinutes(5),
                now
        );
    }

    private EmotionModelRegistryRecord artifactJsonHealthyRegistryRecord() throws Exception {
        Path metricsJsonPath = Files.writeString(
                tempDir.resolve("healthy-metrics.json"),
                """
                        {
                          "classification_report": {
                            "accuracy": 0.42,
                            "macro avg": {
                              "f1-score": 0.36
                            }
                          },
                          "confusion_matrix": [[1, 0], [0, 1]]
                        }
                        """
        );
        Path labelMetadataPath = Files.writeString(
                tempDir.resolve("healthy-label-metadata.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "active_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "num_labels": 5,
                          "train_distribution": {"HAPPY": 10},
                          "valid_distribution": {"HAPPY": 5}
                        }
                        """
        );
        Path trainingConfigPath = Files.writeString(
                tempDir.resolve("healthy-training-config.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "max_length": 96,
                          "num_labels": 6,
                          "num_train_epochs": 2,
                          "learning_rate": 0.00002
                        }
                        """
        );
        Path labelMapPath = Files.writeString(
                tempDir.resolve("healthy-label-map.json"),
                """
                        {
                          "id_to_label": {"0": "HAPPY"},
                          "label_to_id": {"HAPPY": 0},
                          "label_to_tags": {"HAPPY": ["HAPPY"]}
                        }
                        """
        );
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 10, 0);
        return new EmotionModelRegistryRecord(
                12L,
                "artifact_json_check_ok",
                "artifact_json_check_ok_active5",
                "beomi/KcELECTRA-base",
                tempDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                labelMapPath.toString(),
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                false,
                BigDecimal.valueOf(0.4200),
                BigDecimal.valueOf(0.3600),
                BigDecimal.valueOf(0.5000),
                BigDecimal.valueOf(0.3100),
                BigDecimal.valueOf(0.4000),
                BigDecimal.valueOf(0.2900),
                BigDecimal.valueOf(0.2100),
                "json check ready",
                "approved for json validation",
                null,
                now,
                null,
                null,
                now.minusMinutes(5),
                now
        );
    }

    private EmotionModelRegistryRecord artifactJsonBrokenRegistryRecord() throws Exception {
        Path metricsJsonPath = Files.writeString(
                tempDir.resolve("broken-metrics.json"),
                "{ not-json"
        );
        Path labelMetadataPath = Files.writeString(
                tempDir.resolve("broken-label-metadata.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "train_distribution": {"HAPPY": 10},
                          "valid_distribution": {"HAPPY": 5}
                        }
                        """
        );
        Path trainingConfigPath = Files.writeString(
                tempDir.resolve("healthy-training-config-2.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "max_length": 96,
                          "num_labels": 6,
                          "num_train_epochs": 2,
                          "learning_rate": 0.00002
                        }
                        """
        );
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 15, 0);
        return new EmotionModelRegistryRecord(
                13L,
                "artifact_json_check_broken",
                "artifact_json_check_broken_active5",
                "beomi/KcELECTRA-base",
                tempDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                null,
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.REJECTED,
                false,
                false,
                BigDecimal.valueOf(0.3900),
                BigDecimal.valueOf(0.3000),
                BigDecimal.valueOf(0.2000),
                BigDecimal.valueOf(0.6000),
                BigDecimal.valueOf(0.2400),
                BigDecimal.valueOf(0.1700),
                BigDecimal.valueOf(0.4400),
                "json path broken",
                null,
                "schema invalid",
                null,
                now,
                null,
                now.minusMinutes(5),
                now
        );
    }

    private EmotionModelRegistryRecord promotionBaselineRegistryRecord() throws Exception {
        Path artifactDir = Files.createDirectory(tempDir.resolve("promotion-baseline-artifact"));
        Path metricsJsonPath = Files.writeString(
                tempDir.resolve("promotion-baseline-metrics.json"),
                """
                        {
                          "classification_report": {
                            "HAPPY": {"f1-score": 0.6146},
                            "CALM": {"f1-score": 0.2941},
                            "accuracy": 0.4267,
                            "macro avg": {"f1-score": 0.3645}
                          },
                          "confusion_matrix": [
                            [81, 56, 12, 0, 1],
                            [56, 82, 6, 6, 0],
                            [0, 2, 35, 113, 0],
                            [2, 5, 21, 122, 0],
                            [57, 73, 14, 6, 0]
                          ]
                        }
                        """
        );
        Path labelMetadataPath = Files.writeString(
                tempDir.resolve("promotion-baseline-label-metadata.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "active_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "num_labels": 5,
                          "train_distribution": {"HAPPY": 500},
                          "valid_distribution": {"HAPPY": 150}
                        }
                        """
        );
        Path trainingConfigPath = Files.writeString(
                tempDir.resolve("promotion-baseline-training-config.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "max_length": 96,
                          "num_labels": 6,
                          "num_train_epochs": 2,
                          "learning_rate": 0.00002
                        }
                        """
        );
        Path labelMapPath = Files.writeString(
                tempDir.resolve("promotion-baseline-label-map.json"),
                """
                        {
                          "id_to_label": {"0": "HAPPY"},
                          "label_to_id": {"HAPPY": 0},
                          "label_to_tags": {"HAPPY": ["HAPPY"]}
                        }
                        """
        );
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 20, 0);
        return new EmotionModelRegistryRecord(
                20L,
                "cpu_compare_medium_relabel_weighted",
                "cpu_compare_medium_relabel_weighted_active5",
                "beomi/KcELECTRA-base",
                artifactDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                labelMapPath.toString(),
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.ACTIVE,
                true,
                false,
                BigDecimal.valueOf(0.4267),
                BigDecimal.valueOf(0.3645),
                BigDecimal.valueOf(0.6146),
                BigDecimal.valueOf(0.2941),
                BigDecimal.valueOf(0.4457),
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.4682),
                "baseline",
                "approved",
                null,
                now.minusMinutes(10),
                null,
                now,
                now.minusMinutes(15),
                now
        );
    }

    private EmotionModelRegistryRecord promotionCandidateRegistryRecord() throws Exception {
        Path artifactDir = Files.createDirectory(tempDir.resolve("promotion-candidate-artifact"));
        Path metricsJsonPath = Files.writeString(
                tempDir.resolve("promotion-candidate-metrics.json"),
                """
                        {
                          "evaluated_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "classification_report": {
                            "HAPPY": {"f1-score": 0.2000},
                            "CALM": {"f1-score": 0.6318},
                            "accuracy": 0.4427,
                            "macro avg": {"f1-score": 0.3888}
                          },
                          "confusion_matrix": [
                            [18, 2, 2, 5, 1],
                            [12, 133, 1, 4, 0],
                            [0, 6, 34, 93, 17],
                            [0, 5, 15, 112, 18],
                            [0, 3, 28, 84, 35]
                          ]
                        }
                        """
        );
        Files.writeString(
                tempDir.resolve("happy_to_calm_errors_manual_seed_v2.csv"),
                """
                        sample_id,service_label,baseline_pred,manual_relabel_reason,v2_pred
                        valid-000001,HAPPY,HAPPY,,CALM
                        valid-000002,HAPPY,HAPPY,,CALM
                        """
        );
        Files.writeString(
                tempDir.resolve("valid_predictions_baseline_on_manual_seed_v2.csv"),
                """
                        sample_id,service_label,predicted_label
                        valid-000001,HAPPY,HAPPY
                        valid-000002,HAPPY,HAPPY
                        """
        );
        Files.writeString(
                tempDir.resolve("valid_predictions_manual_seed_v2_on_manual_seed_v2.csv"),
                """
                        sample_id,service_label,predicted_label
                        valid-000001,HAPPY,CALM
                        valid-000002,HAPPY,CALM
                        """
        );
        Path labelMetadataPath = Files.writeString(
                tempDir.resolve("promotion-candidate-label-metadata.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "active_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "num_labels": 5,
                          "train_distribution": {"HAPPY": 500},
                          "valid_distribution": {"HAPPY": 150}
                        }
                        """
        );
        Path trainingConfigPath = Files.writeString(
                tempDir.resolve("promotion-candidate-training-config.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "max_length": 96,
                          "num_labels": 6,
                          "num_train_epochs": 2,
                          "learning_rate": 0.00002
                        }
                        """
        );
        Path labelMapPath = Files.writeString(
                tempDir.resolve("promotion-candidate-label-map.json"),
                """
                        {
                          "id_to_label": {"0": "HAPPY"},
                          "label_to_id": {"HAPPY": 0},
                          "label_to_tags": {"HAPPY": ["HAPPY"]}
                        }
                        """
        );
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 21, 0);
        return new EmotionModelRegistryRecord(
                21L,
                "cpu_compare_medium_manual_seed_v2",
                "cpu_compare_medium_manual_seed_v2_active5",
                "beomi/KcELECTRA-base",
                artifactDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                labelMapPath.toString(),
                "emotion_mvp_manual_seed_v2",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                true,
                BigDecimal.valueOf(0.4427),
                BigDecimal.valueOf(0.3888),
                BigDecimal.valueOf(0.2000),
                BigDecimal.valueOf(0.6318),
                BigDecimal.valueOf(0.2957),
                BigDecimal.valueOf(0.5000),
                BigDecimal.valueOf(0.3167),
                "candidate",
                "approved as shadow candidate only",
                null,
                now.minusMinutes(5),
                null,
                null,
                now.minusMinutes(10),
                now
        );
    }

    private EmotionModelRegistryRecord promotionActivatableCandidateRegistryRecord() throws Exception {
        Path artifactDir = Files.createDirectory(tempDir.resolve("promotion-activatable-candidate-artifact"));
        Path metricsJsonPath = Files.writeString(
                tempDir.resolve("promotion-activatable-candidate-metrics.json"),
                """
                        {
                          "evaluated_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "classification_report": {
                            "HAPPY": {"f1-score": 0.7000},
                            "CALM": {"f1-score": 0.3200},
                            "accuracy": 0.4550,
                            "macro avg": {"f1-score": 0.3900}
                          },
                          "confusion_matrix": [
                            [90, 1, 10, 8, 2],
                            [48, 88, 6, 8, 0],
                            [1, 2, 40, 102, 5],
                            [2, 3, 19, 124, 2],
                            [30, 60, 20, 20, 20]
                          ]
                        }
                        """
        );
        Files.writeString(
                tempDir.resolve("happy_to_calm_errors_guarded_activation_v1.csv"),
                """
                        sample_id,service_label,manual_relabel_reason,predicted_label
                        valid-010001,HAPPY,validated as genuine boundary error,CALM
                        """
        );
        Path labelMetadataPath = Files.writeString(
                tempDir.resolve("promotion-activatable-candidate-label-metadata.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "active_labels": ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"],
                          "num_labels": 5,
                          "train_distribution": {"HAPPY": 500},
                          "valid_distribution": {"HAPPY": 150}
                        }
                        """
        );
        Path trainingConfigPath = Files.writeString(
                tempDir.resolve("promotion-activatable-candidate-training-config.json"),
                """
                        {
                          "model_name": "beomi/KcELECTRA-base",
                          "max_length": 96,
                          "num_labels": 6,
                          "num_train_epochs": 2,
                          "learning_rate": 0.00002
                        }
                        """
        );
        Path labelMapPath = Files.writeString(
                tempDir.resolve("promotion-activatable-candidate-label-map.json"),
                """
                        {
                          "id_to_label": {"0": "HAPPY"},
                          "label_to_id": {"HAPPY": 0},
                          "label_to_tags": {"HAPPY": ["HAPPY"]}
                        }
                        """
        );
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 25, 0);
        return new EmotionModelRegistryRecord(
                30L,
                "cpu_compare_medium_guarded_activation_v1",
                "cpu_compare_medium_guarded_activation_v1_active5",
                "beomi/KcELECTRA-base",
                artifactDir.toString(),
                metricsJsonPath.toString(),
                labelMetadataPath.toString(),
                trainingConfigPath.toString(),
                labelMapPath.toString(),
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.APPROVED,
                false,
                false,
                BigDecimal.valueOf(0.4550),
                BigDecimal.valueOf(0.3900),
                BigDecimal.valueOf(0.7000),
                BigDecimal.valueOf(0.3200),
                BigDecimal.valueOf(0.3100),
                BigDecimal.valueOf(0.4900),
                BigDecimal.valueOf(0.2700),
                "promotion checklist passes activation gate",
                "approved after checklist review",
                null,
                now.minusMinutes(2),
                null,
                null,
                now.minusMinutes(10),
                now
        );
    }

    private EmotionModelRegistryRecord activatedPromotionCandidateRegistryRecord() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 19, 26, 0);
        return new EmotionModelRegistryRecord(
                30L,
                "cpu_compare_medium_guarded_activation_v1",
                "cpu_compare_medium_guarded_activation_v1_active5",
                "beomi/KcELECTRA-base",
                "activated-artifact-dir",
                "activated-metrics-path",
                "activated-label-metadata-path",
                "activated-training-config-path",
                "activated-label-map-path",
                "emotion_mvp_relabel_weighted",
                "emotion_mvp_cpu_compare_medium",
                "TIRED_FALLBACK_ONLY",
                EmotionModelRegistryStatus.ACTIVE,
                true,
                false,
                BigDecimal.valueOf(0.4550),
                BigDecimal.valueOf(0.3900),
                BigDecimal.valueOf(0.7000),
                BigDecimal.valueOf(0.3200),
                BigDecimal.valueOf(0.3100),
                BigDecimal.valueOf(0.4900),
                BigDecimal.valueOf(0.2700),
                "promotion checklist passes activation gate",
                "approved after checklist review",
                null,
                now.minusMinutes(3),
                null,
                now,
                now.minusMinutes(11),
                now
        );
    }
}
