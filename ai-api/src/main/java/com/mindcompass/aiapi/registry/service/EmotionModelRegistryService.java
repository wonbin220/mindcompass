// 감정분류 모델 registry 조회와 상태 변경을 담당하는 서비스이다.
package com.mindcompass.aiapi.registry.service;

import com.mindcompass.aiapi.registry.client.EmotionModelServingRuntimeInfoClient;
import com.mindcompass.aiapi.registry.client.EmotionModelServingRuntimeInfoResponse;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryRecord;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatusHistoryRecord;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatus;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactHealthItemResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactHealthResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactJsonCheckItemResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryArtifactJsonCheckResponse;
import com.mindcompass.aiapi.registry.dto.CreateEmotionModelRegistryRequest;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAdminSummaryResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryAvailableTransitionsResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryPromotionChecklistItemResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryPromotionChecklistResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryRuntimeAlignmentResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryResponse;
import com.mindcompass.aiapi.registry.dto.EmotionModelRegistryStatusHistoryResponse;
import com.mindcompass.aiapi.registry.dto.UpdateEmotionModelRegistryStatusRequest;
import com.mindcompass.aiapi.registry.repository.EmotionModelRegistryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmotionModelRegistryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> ACTIVE_FIVE_LABELS = List.of("HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY");
    private static final String HAPPY_TO_CALM_ERROR_SAMPLE_PREFIX = "happy_to_calm_errors_";
    private static final String MANUAL_RELABEL_REASON_COLUMN = "manual_relabel_reason";

    private final EmotionModelRegistryRepository repository;
    private final EmotionModelServingRuntimeInfoClient servingRuntimeInfoClient;

    public EmotionModelRegistryService(
            EmotionModelRegistryRepository repository,
            EmotionModelServingRuntimeInfoClient servingRuntimeInfoClient
    ) {
        this.repository = repository;
        this.servingRuntimeInfoClient = servingRuntimeInfoClient;
    }

    @Transactional(readOnly = true)
    public List<EmotionModelRegistryResponse> getAll(
            String status,
            Boolean isActive,
            Boolean isShadow,
            String experimentName,
            Boolean currentShadowOnly
    ) {
        EmotionModelRegistryStatus parsedStatus = status == null || status.isBlank()
                ? null
                : parseStatus(status);
        return repository.findAll(parsedStatus, isActive, isShadow, experimentName, currentShadowOnly).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryResponse getActive() {
        return repository.findActive()
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active emotion model not found"));
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryResponse getById(Long id) {
        return toResponse(getRecordById(id));
    }

    @Transactional(readOnly = true)
    public List<EmotionModelRegistryStatusHistoryResponse> getStatusHistory(Long id) {
        getRecordById(id);
        return repository.findStatusHistoryByRegistryId(id).stream()
                .map(this::toStatusHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryAdminSummaryResponse getSummary() {
        Map<EmotionModelRegistryStatus, Long> counts = new EnumMap<>(EmotionModelRegistryStatus.class);
        for (EmotionModelRegistryRepository.RegistryStatusCountRow row : repository.findStatusCounts()) {
            counts.put(parseStatus(row.status()), row.count());
        }

        EmotionModelRegistryRecord activeRecord = repository.findActive().orElse(null);
        return new EmotionModelRegistryAdminSummaryResponse(
                repository.countAll(),
                counts.getOrDefault(EmotionModelRegistryStatus.TRAINED, 0L),
                counts.getOrDefault(EmotionModelRegistryStatus.APPROVED, 0L),
                counts.getOrDefault(EmotionModelRegistryStatus.REJECTED, 0L),
                counts.getOrDefault(EmotionModelRegistryStatus.SHADOW, 0L),
                counts.getOrDefault(EmotionModelRegistryStatus.ACTIVE, 0L),
                counts.getOrDefault(EmotionModelRegistryStatus.ARCHIVED, 0L),
                repository.countShadowLineage(),
                activeRecord == null ? null : activeRecord.id(),
                activeRecord == null ? null : activeRecord.experimentName()
        );
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryAvailableTransitionsResponse getAvailableTransitions(Long id) {
        EmotionModelRegistryRecord record = getRecordById(id);
        return new EmotionModelRegistryAvailableTransitionsResponse(
                record.id(),
                record.status().name(),
                record.isActive(),
                record.isShadow(),
                allowedStatusUpdates(record).stream().map(Enum::name).toList(),
                canActivate(record)
        );
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryRuntimeAlignmentResponse getActiveRuntimeAlignment() {
        EmotionModelRegistryRecord activeRecord = repository.findActive()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active emotion model not found"));
        EmotionModelServingRuntimeInfoResponse runtimeInfo = servingRuntimeInfoClient.getRuntimeInfo();
        String normalizedRegistryArtifactDir = normalizePathForComparison(activeRecord.artifactDir());
        String normalizedRuntimeModelDir = normalizePathForComparison(runtimeInfo.modelDirResolved());
        boolean artifactDirAligned = normalizedRegistryArtifactDir != null
                && normalizedRegistryArtifactDir.equals(normalizedRuntimeModelDir);
        boolean overallAligned = artifactDirAligned && runtimeInfo.modelDirExists();
        String detail = overallAligned
                ? "Active registry artifact dir matches FastAPI runtime model dir"
                : "Active registry artifact dir does not match FastAPI runtime model dir";

        return new EmotionModelRegistryRuntimeAlignmentResponse(
                activeRecord.id(),
                activeRecord.experimentName(),
                activeRecord.artifactDir(),
                servingRuntimeInfoClient.getFastApiBaseUrl(),
                runtimeInfo.modelDirConfigured(),
                runtimeInfo.modelDirResolved(),
                runtimeInfo.modelDirExists(),
                runtimeInfo.modelLoadSource(),
                runtimeInfo.labelMapPathConfigured(),
                runtimeInfo.labelMapPathResolved(),
                runtimeInfo.labelMapPathExists(),
                runtimeInfo.modelName(),
                runtimeInfo.maxLength(),
                artifactDirAligned,
                overallAligned,
                detail
        );
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryArtifactHealthResponse getArtifactHealth(Long id) {
        EmotionModelRegistryRecord record = getRecordById(id);
        List<EmotionModelRegistryArtifactHealthItemResponse> items = List.of(
                buildArtifactHealthItem("artifactDir", record.artifactDir(), true, true),
                buildArtifactHealthItem("metricsJsonPath", record.metricsJsonPath(), true, false),
                buildArtifactHealthItem("labelMetadataPath", record.labelMetadataPath(), false, false),
                buildArtifactHealthItem("trainingConfigPath", record.trainingConfigPath(), false, false),
                buildArtifactHealthItem("labelMapPath", record.labelMapPath(), false, false)
        );

        List<String> missingRequiredItems = items.stream()
                .filter(EmotionModelRegistryArtifactHealthItemResponse::required)
                .filter(item -> !isHealthy(item))
                .map(EmotionModelRegistryArtifactHealthItemResponse::name)
                .toList();
        List<String> missingOptionalItems = items.stream()
                .filter(item -> !item.required())
                .filter(EmotionModelRegistryArtifactHealthItemResponse::configured)
                .filter(item -> !isHealthy(item))
                .map(EmotionModelRegistryArtifactHealthItemResponse::name)
                .toList();

        return new EmotionModelRegistryArtifactHealthResponse(
                record.id(),
                record.experimentName(),
                record.status().name(),
                missingRequiredItems.isEmpty(),
                items.stream()
                        .filter(EmotionModelRegistryArtifactHealthItemResponse::configured)
                        .allMatch(this::isHealthy),
                missingRequiredItems,
                missingOptionalItems,
                items
        );
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryArtifactJsonCheckResponse getArtifactJsonCheck(Long id) {
        EmotionModelRegistryRecord record = getRecordById(id);
        List<EmotionModelRegistryArtifactJsonCheckItemResponse> items = List.of(
                buildJsonCheckItem(
                        "metricsJsonPath",
                        record.metricsJsonPath(),
                        true,
                        List.of("classification_report", "confusion_matrix", "classification_report.accuracy", "classification_report.macro avg"),
                        node -> node.has("classification_report") && node.has("confusion_matrix")
                ),
                buildJsonCheckItem(
                        "labelMetadataPath",
                        record.labelMetadataPath(),
                        false,
                        List.of("model_name", "active_labels", "num_labels", "train_distribution", "valid_distribution"),
                        node -> node.has("model_name")
                                && node.has("active_labels")
                                && node.has("num_labels")
                                && node.has("train_distribution")
                                && node.has("valid_distribution")
                                && node.get("active_labels").isArray()
                                && node.get("num_labels").canConvertToInt()
                ),
                buildJsonCheckItem(
                        "trainingConfigPath",
                        record.trainingConfigPath(),
                        false,
                        List.of("model_name", "max_length", "num_labels", "num_train_epochs", "learning_rate"),
                        node -> node.has("model_name")
                                && node.has("max_length")
                                && node.has("num_labels")
                                && node.has("num_train_epochs")
                                && node.has("learning_rate")
                ),
                buildJsonCheckItem(
                        "labelMapPath",
                        record.labelMapPath(),
                        false,
                        List.of("id_to_label", "label_to_id", "label_to_tags"),
                        node -> node.has("id_to_label")
                                && node.has("label_to_id")
                                && node.has("label_to_tags")
                                && node.get("id_to_label").isObject()
                                && node.get("label_to_id").isObject()
                                && node.get("label_to_tags").isObject()
                )
        );

        List<String> failedParseItems = items.stream()
                .filter(EmotionModelRegistryArtifactJsonCheckItemResponse::configured)
                .filter(item -> !item.parseable())
                .map(EmotionModelRegistryArtifactJsonCheckItemResponse::name)
                .toList();
        List<String> failedSchemaItems = items.stream()
                .filter(EmotionModelRegistryArtifactJsonCheckItemResponse::configured)
                .filter(item -> !item.schemaValid())
                .map(EmotionModelRegistryArtifactJsonCheckItemResponse::name)
                .toList();

        return new EmotionModelRegistryArtifactJsonCheckResponse(
                record.id(),
                record.experimentName(),
                record.status().name(),
                failedParseItems.isEmpty(),
                items.stream()
                        .filter(EmotionModelRegistryArtifactJsonCheckItemResponse::required)
                        .allMatch(EmotionModelRegistryArtifactJsonCheckItemResponse::schemaValid),
                items.stream()
                        .filter(EmotionModelRegistryArtifactJsonCheckItemResponse::configured)
                        .allMatch(EmotionModelRegistryArtifactJsonCheckItemResponse::schemaValid),
                failedParseItems,
                failedSchemaItems,
                items
        );
    }

    @Transactional(readOnly = true)
    public EmotionModelRegistryPromotionChecklistResponse getPromotionChecklist(Long id) {
        EmotionModelRegistryRecord candidate = getRecordById(id);
        EmotionModelRegistryRecord baseline = repository.findActive().orElse(null);
        return buildPromotionChecklist(candidate, baseline);
    }

    private EmotionModelRegistryPromotionChecklistResponse buildPromotionChecklist(
            EmotionModelRegistryRecord candidate,
            EmotionModelRegistryRecord baseline
    ) {
        PromotionChecklistComputation checklist = computePromotionChecklist(candidate, baseline);

        return new EmotionModelRegistryPromotionChecklistResponse(
                candidate.id(),
                candidate.experimentName(),
                candidate.status().name(),
                baseline == null ? null : baseline.id(),
                baseline == null ? null : baseline.experimentName(),
                checklist.recommendation(),
                checklist.readyForShadow(),
                checklist.readyForActive(),
                checklist.passedCount(),
                checklist.items().size(),
                checklist.items()
        );
    }

    private PromotionChecklistComputation computePromotionChecklist(
            EmotionModelRegistryRecord candidate,
            EmotionModelRegistryRecord baseline
    ) {
        EmotionModelRegistryArtifactHealthResponse artifactHealth = getArtifactHealth(candidate.id());
        EmotionModelRegistryArtifactJsonCheckResponse jsonCheck = getArtifactJsonCheck(candidate.id());
        ParsedLabelMetadata labelMetadata = parseLabelMetadata(candidate.labelMetadataPath());
        ParsedMetrics candidateMetrics = parseMetrics(candidate.metricsJsonPath(), labelMetadata == null ? ACTIVE_FIVE_LABELS : labelMetadata.activeLabels());
        ParsedMetrics baselineMetrics = baseline == null
                ? null
                : parseMetrics(baseline.metricsJsonPath(), parseActiveLabelsFromRegistry(baseline));

        List<EmotionModelRegistryPromotionChecklistItemResponse> items = new ArrayList<>();
        items.add(checklistItem(
                "artifactHealth",
                artifactHealth.requiredArtifactsHealthy(),
                artifactHealth.requiredArtifactsHealthy()
                        ? "required artifact paths are healthy"
                        : "missing required artifact paths: " + artifactHealth.missingRequiredItems()
        ));
        items.add(checklistItem(
                "jsonSchema",
                jsonCheck.requiredSchemaHealthy(),
                jsonCheck.requiredSchemaHealthy()
                        ? "required JSON artifacts passed parse/schema check"
                        : "failed JSON checks: " + jsonCheck.failedSchemaItems()
        ));
        items.add(checklistItem(
                "fallbackPolicy",
                "TIRED_FALLBACK_ONLY".equals(candidate.fallbackPolicy()),
                "fallback policy = " + candidate.fallbackPolicy()
        ));

        boolean labelContractPassed = labelMetadata != null
                && labelMetadata.numLabels() == ACTIVE_FIVE_LABELS.size()
                && labelMetadata.activeLabels().equals(ACTIVE_FIVE_LABELS);
        items.add(checklistItem(
                "activeLabelContract",
                labelContractPassed,
                labelMetadata == null
                        ? "label metadata is missing or invalid"
                        : "activeLabels=" + labelMetadata.activeLabels() + ", numLabels=" + labelMetadata.numLabels()
        ));

        boolean statusOpenForPromotion = candidate.status() != EmotionModelRegistryStatus.REJECTED
                && candidate.status() != EmotionModelRegistryStatus.ARCHIVED;
        items.add(checklistItem(
                "statusOpenForPromotion",
                statusOpenForPromotion,
                "current status = " + candidate.status().name()
        ));

        boolean statusApprovedForActive = candidate.status() == EmotionModelRegistryStatus.APPROVED
                || candidate.status() == EmotionModelRegistryStatus.ACTIVE;
        items.add(checklistItem(
                "statusApprovedForActive",
                statusApprovedForActive,
                "current status = " + candidate.status().name()
        ));

        if (baselineMetrics != null && candidateMetrics != null) {
            items.add(checklistItem(
                    "macroF1Gate",
                    candidateMetrics.macroF1() >= baselineMetrics.macroF1(),
                    formatMetricCompare("macroF1", candidateMetrics.macroF1(), baselineMetrics.macroF1())
            ));
            items.add(checklistItem(
                    "happyF1Gate",
                    candidateMetrics.happyF1() >= baselineMetrics.happyF1(),
                    formatMetricCompare("happyF1", candidateMetrics.happyF1(), baselineMetrics.happyF1())
            ));
            items.add(checklistItem(
                    "calmF1Gate",
                    candidateMetrics.calmF1() >= baselineMetrics.calmF1(),
                    formatMetricCompare("calmF1", candidateMetrics.calmF1(), baselineMetrics.calmF1())
            ));
            items.add(checklistItem(
                    "happyToCalmGate",
                    candidateMetrics.happyToCalmCount() <= baselineMetrics.happyToCalmCount(),
                    "candidate HAPPY->CALM=" + candidateMetrics.happyToCalmCount()
                            + ", baseline HAPPY->CALM=" + baselineMetrics.happyToCalmCount()
            ));
        } else {
            items.add(checklistItem("macroF1Gate", false, "baseline or candidate metrics could not be parsed"));
            items.add(checklistItem("happyF1Gate", false, "baseline or candidate metrics could not be parsed"));
            items.add(checklistItem("calmF1Gate", false, "baseline or candidate metrics could not be parsed"));
            items.add(checklistItem("happyToCalmGate", false, "baseline or candidate metrics could not be parsed"));
        }
        ParsedErrorSampleCsvReview errorSampleReview = evaluateHappyToCalmErrorSampleGate(candidate, candidateMetrics);
        items.add(checklistItem(
                "errorSampleCsvGate",
                errorSampleReview.passed(),
                errorSampleReview.detail()
        ));

        boolean readyForShadow = getChecklistPassed(items, "artifactHealth")
                && getChecklistPassed(items, "jsonSchema")
                && getChecklistPassed(items, "fallbackPolicy")
                && getChecklistPassed(items, "activeLabelContract")
                && getChecklistPassed(items, "statusOpenForPromotion");
        boolean readyForActive = readyForShadow
                && getChecklistPassed(items, "statusApprovedForActive")
                && getChecklistPassed(items, "macroF1Gate")
                && getChecklistPassed(items, "happyF1Gate")
                && getChecklistPassed(items, "calmF1Gate")
                && getChecklistPassed(items, "happyToCalmGate")
                && getChecklistPassed(items, "errorSampleCsvGate");

        String recommendation;
        if (candidate.isActive()) {
            recommendation = "ACTIVE_ALREADY";
        } else if (readyForActive) {
            recommendation = "ACTIVE_CANDIDATE";
        } else if (readyForShadow) {
            recommendation = "SHADOW_ONLY";
        } else {
            recommendation = "BLOCKED";
        }

        int passedCount = (int) items.stream().filter(EmotionModelRegistryPromotionChecklistItemResponse::passed).count();
        return new PromotionChecklistComputation(
                recommendation,
                readyForShadow,
                readyForActive,
                passedCount,
                items
        );
    }

    @Transactional
    public EmotionModelRegistryResponse create(CreateEmotionModelRegistryRequest request) {
        EmotionModelRegistryStatus status = parseStatus(defaultIfBlank(request.status(), "TRAINED"));
        if (status == EmotionModelRegistryStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use activate endpoint instead of creating ACTIVE directly");
        }
        LocalDateTime now = LocalDateTime.now();

        EmotionModelRegistryRecord record = new EmotionModelRegistryRecord(
                null,
                request.experimentName(),
                request.modelName(),
                request.baseModelName(),
                request.artifactDir(),
                request.metricsJsonPath(),
                request.labelMetadataPath(),
                request.trainingConfigPath(),
                request.labelMapPath(),
                request.trainingDatasetTag(),
                request.validationDatasetTag(),
                request.fallbackPolicy(),
                status,
                false,
                Boolean.TRUE.equals(request.isShadow()),
                request.accuracy(),
                request.macroF1(),
                request.happyF1(),
                request.calmF1(),
                request.anxiousF1(),
                request.sadF1(),
                request.angryF1(),
                request.servingNotes(),
                request.approvalNote(),
                request.rejectionReason(),
                status == EmotionModelRegistryStatus.APPROVED ? now : null,
                status == EmotionModelRegistryStatus.REJECTED ? now : null,
                null,
                now,
                now
        );

        Long id = repository.insert(record);
        repository.insertStatusHistory(id, null, status.name(), "created");
        return getById(id);
    }

    @Transactional
    public EmotionModelRegistryResponse updateStatus(Long id, UpdateEmotionModelRegistryStatusRequest request) {
        EmotionModelRegistryRecord current = getRecordById(id);
        EmotionModelRegistryStatus nextStatus = parseStatus(request.status());
        validateStatusUpdate(current, nextStatus, request);
        LocalDateTime now = LocalDateTime.now();

        repository.updateStatus(
                id,
                nextStatus,
                shouldMarkShadow(current, nextStatus),
                request.approvalNote(),
                request.rejectionReason(),
                request.servingNotes(),
                nextStatus == EmotionModelRegistryStatus.APPROVED ? now : current.approvedAt(),
                nextStatus == EmotionModelRegistryStatus.REJECTED ? now : current.rejectedAt(),
                now
        );
        repository.insertStatusHistory(id, current.status().name(), nextStatus.name(), buildStatusReason(request));
        return getById(id);
    }

    @Transactional
    public EmotionModelRegistryResponse activate(Long id) {
        EmotionModelRegistryRecord target = getRecordById(id);
        if (target.isActive()) {
            return toResponse(target);
        }
        if (!canActivate(target)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only APPROVED registry rows can be activated"
            );
        }
        EmotionModelRegistryRecord baseline = repository.findActive().orElse(null);
        PromotionChecklistComputation checklist = computePromotionChecklist(target, baseline);
        if (!"ACTIVE_CANDIDATE".equals(checklist.recommendation())) {
            List<String> failedItems = checklist.items().stream()
                    .filter(item -> !item.passed())
                    .map(EmotionModelRegistryPromotionChecklistItemResponse::name)
                    .toList();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Activation blocked by promotion checklist: recommendation="
                            + checklist.recommendation()
                            + ", failedItems="
                            + failedItems
            );
        }
        LocalDateTime now = LocalDateTime.now();

        repository.findActive().ifPresent(active -> {
            if (!active.id().equals(id)) {
                repository.deactivateCurrentActive(now);
                repository.insertStatusHistory(active.id(), active.status().name(), EmotionModelRegistryStatus.APPROVED.name(),
                        "deactivated by activating registry id " + id);
            }
        });

        repository.activate(id, now, now);
        repository.insertStatusHistory(id, target.status().name(), EmotionModelRegistryStatus.ACTIVE.name(), "activated");
        return getById(id);
    }

    private void validateStatusUpdate(
            EmotionModelRegistryRecord current,
            EmotionModelRegistryStatus nextStatus,
            UpdateEmotionModelRegistryStatusRequest request
    ) {
        if (nextStatus == EmotionModelRegistryStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use activate endpoint for ACTIVE transition");
        }
        if (current.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deactivate via activate endpoint flow before changing active record status");
        }
        if (current.status() == nextStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registry status is already " + nextStatus.name());
        }
        if (!allowedStatusUpdates(current).contains(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported registry transition: " + current.status().name() + " -> " + nextStatus.name()
            );
        }
        if (nextStatus == EmotionModelRegistryStatus.APPROVED
                && (request.approvalNote() == null || request.approvalNote().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "approvalNote is required when moving to APPROVED");
        }
        if (nextStatus == EmotionModelRegistryStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rejectionReason is required when moving to REJECTED");
        }
    }

    private EnumSet<EmotionModelRegistryStatus> allowedStatusUpdates(EmotionModelRegistryRecord record) {
        return switch (record.status()) {
            case TRAINED -> EnumSet.of(
                    EmotionModelRegistryStatus.APPROVED,
                    EmotionModelRegistryStatus.REJECTED,
                    EmotionModelRegistryStatus.SHADOW,
                    EmotionModelRegistryStatus.ARCHIVED
            );
            case APPROVED -> EnumSet.of(
                    EmotionModelRegistryStatus.REJECTED,
                    EmotionModelRegistryStatus.SHADOW,
                    EmotionModelRegistryStatus.ARCHIVED
            );
            case SHADOW -> EnumSet.of(
                    EmotionModelRegistryStatus.APPROVED,
                    EmotionModelRegistryStatus.REJECTED,
                    EmotionModelRegistryStatus.ARCHIVED
            );
            case REJECTED -> EnumSet.of(EmotionModelRegistryStatus.ARCHIVED);
            case ARCHIVED, ACTIVE -> EnumSet.noneOf(EmotionModelRegistryStatus.class);
        };
    }

    private boolean canActivate(EmotionModelRegistryRecord record) {
        return !record.isActive() && record.status() == EmotionModelRegistryStatus.APPROVED;
    }

    private boolean shouldMarkShadow(EmotionModelRegistryRecord current, EmotionModelRegistryStatus nextStatus) {
        return current.isShadow() || nextStatus == EmotionModelRegistryStatus.SHADOW;
    }

    private EmotionModelRegistryArtifactJsonCheckItemResponse buildJsonCheckItem(
            String name,
            String rawPath,
            boolean required,
            List<String> requiredKeys,
            Predicate<JsonNode> schemaValidator
    ) {
        if (rawPath == null || rawPath.isBlank()) {
            return new EmotionModelRegistryArtifactJsonCheckItemResponse(
                    name,
                    rawPath,
                    required,
                    false,
                    false,
                    false,
                    false,
                    requiredKeys,
                    requiredKeys,
                    null
            );
        }

        try {
            Path path = Path.of(rawPath);
            if (!Files.exists(path)) {
                return new EmotionModelRegistryArtifactJsonCheckItemResponse(
                        name,
                        rawPath,
                        required,
                        true,
                        false,
                        false,
                        false,
                        requiredKeys,
                        requiredKeys,
                        null
                );
            }
            JsonNode node = OBJECT_MAPPER.readTree(path.toFile());
            List<String> missingKeys = requiredKeys.stream()
                    .filter(requiredKey -> !hasJsonPath(node, requiredKey))
                    .toList();
            boolean schemaValid = missingKeys.isEmpty() && schemaValidator.test(node);
            return new EmotionModelRegistryArtifactJsonCheckItemResponse(
                    name,
                    rawPath,
                    required,
                    true,
                    true,
                    true,
                    schemaValid,
                    requiredKeys,
                    missingKeys,
                    null
            );
        } catch (InvalidPathException exception) {
            return new EmotionModelRegistryArtifactJsonCheckItemResponse(
                    name,
                    rawPath,
                    required,
                    true,
                    false,
                    false,
                    false,
                    requiredKeys,
                    requiredKeys,
                    exception.getMessage()
            );
        } catch (Exception exception) {
            return new EmotionModelRegistryArtifactJsonCheckItemResponse(
                    name,
                    rawPath,
                    required,
                    true,
                    true,
                    false,
                    false,
                    requiredKeys,
                    requiredKeys,
                    exception.getMessage()
            );
        }
    }

    private EmotionModelRegistryArtifactHealthItemResponse buildArtifactHealthItem(
            String name,
            String rawPath,
            boolean required,
            boolean directoryExpected
    ) {
        if (rawPath == null || rawPath.isBlank()) {
            return new EmotionModelRegistryArtifactHealthItemResponse(
                    name,
                    rawPath,
                    required,
                    false,
                    false,
                    directoryExpected,
                    false,
                    null
            );
        }

        try {
            Path path = Path.of(rawPath);
            boolean exists = Files.exists(path);
            boolean directory = exists && Files.isDirectory(path);
            return new EmotionModelRegistryArtifactHealthItemResponse(
                    name,
                    rawPath,
                    required,
                    true,
                    exists,
                    directoryExpected,
                    directory,
                    null
            );
        } catch (InvalidPathException exception) {
            return new EmotionModelRegistryArtifactHealthItemResponse(
                    name,
                    rawPath,
                    required,
                    true,
                    false,
                    directoryExpected,
                    false,
                    exception.getMessage()
            );
        }
    }

    private boolean isHealthy(EmotionModelRegistryArtifactHealthItemResponse item) {
        if (!item.configured() || !item.exists()) {
            return false;
        }
        return item.directoryExpected() == item.directory();
    }

    private EmotionModelRegistryPromotionChecklistItemResponse checklistItem(String name, boolean passed, String detail) {
        return new EmotionModelRegistryPromotionChecklistItemResponse(name, passed, detail);
    }

    private boolean getChecklistPassed(List<EmotionModelRegistryPromotionChecklistItemResponse> items, String name) {
        return items.stream()
                .filter(item -> item.name().equals(name))
                .findFirst()
                .map(EmotionModelRegistryPromotionChecklistItemResponse::passed)
                .orElse(false);
    }

    private String formatMetricCompare(String metricName, double candidateValue, double baselineValue) {
        return metricName + " candidate=" + String.format("%.4f", candidateValue)
                + ", baseline=" + String.format("%.4f", baselineValue);
    }

    private ParsedErrorSampleCsvReview evaluateHappyToCalmErrorSampleGate(
            EmotionModelRegistryRecord candidate,
            ParsedMetrics candidateMetrics
    ) {
        if (candidate.isActive()) {
            return new ParsedErrorSampleCsvReview(true, "already active baseline; CSV review gate skipped");
        }
        if (candidateMetrics == null) {
            return new ParsedErrorSampleCsvReview(false, "candidate metrics could not be parsed");
        }
        if (candidateMetrics.happyToCalmCount() < 0) {
            return new ParsedErrorSampleCsvReview(false, "candidate HAPPY->CALM count is unavailable from metrics");
        }
        if (candidateMetrics.happyToCalmCount() == 0) {
            return new ParsedErrorSampleCsvReview(true, "candidate HAPPY->CALM=0 so no review CSV is required");
        }

        Optional<Path> csvPath = resolveHappyToCalmErrorSampleCsvPath(candidate);
        if (csvPath.isEmpty()) {
            return new ParsedErrorSampleCsvReview(
                    false,
                    "candidate HAPPY->CALM=" + candidateMetrics.happyToCalmCount()
                            + " but no matching error sample CSV was found"
            );
        }

        try {
            ParsedErrorSampleCsv parsedCsv = parseHappyToCalmErrorSampleCsv(csvPath.get());
            boolean reviewCompleted = parsedCsv.reviewedRows() == parsedCsv.happyToCalmRows();
            if (parsedCsv.hasBaselinePredictionColumn()) {
                Optional<ParsedHappyToCalmPredictionReview> predictionReview = parseHappyToCalmPredictionReview(candidate);
                if (predictionReview.isEmpty()) {
                    return new ParsedErrorSampleCsvReview(
                            false,
                            "csv=" + csvPath.get().getFileName()
                                    + " uses legacy baseline_pred export but companion prediction CSVs were not found"
                    );
                }
                ParsedHappyToCalmPredictionReview review = predictionReview.get();
                boolean rowCountMatches = parsedCsv.happyToCalmRows() == review.newlyFlippedHappyToCalmCount();
                boolean metricsMatchPredictions = candidateMetrics.happyToCalmCount() == review.candidateHappyToCalmCount();
                return new ParsedErrorSampleCsvReview(
                        rowCountMatches && metricsMatchPredictions && reviewCompleted,
                        "csv=" + csvPath.get().getFileName()
                                + ", exportMode=newly_flipped_only"
                                + ", newlyFlippedRows=" + parsedCsv.happyToCalmRows()
                                + ", reviewedRows=" + parsedCsv.reviewedRows()
                                + ", predictionNewlyFlipped=" + review.newlyFlippedHappyToCalmCount()
                                + ", baselineAlreadyCalm=" + review.baselineAlreadyHappyToCalmCount()
                                + ", metricsHappyToCalm=" + candidateMetrics.happyToCalmCount()
                );
            }
            boolean rowCountMatches = parsedCsv.happyToCalmRows() == candidateMetrics.happyToCalmCount();
            return new ParsedErrorSampleCsvReview(
                    rowCountMatches && reviewCompleted,
                    "csv=" + csvPath.get().getFileName()
                            + ", exportMode=full_happy_to_calm"
                            + ", happyToCalmRows=" + parsedCsv.happyToCalmRows()
                            + ", reviewedRows=" + parsedCsv.reviewedRows()
                            + ", metricsHappyToCalm=" + candidateMetrics.happyToCalmCount()
            );
        } catch (Exception exception) {
            return new ParsedErrorSampleCsvReview(
                    false,
                    "failed to parse error sample CSV " + csvPath.get().getFileName() + ": " + exception.getMessage()
            );
        }
    }

    private ParsedLabelMetadata parseLabelMetadata(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(Path.of(rawPath).toFile());
            if (!node.has("active_labels") || !node.has("num_labels") || !node.get("active_labels").isArray()) {
                return null;
            }
            List<String> activeLabels = OBJECT_MAPPER.convertValue(
                    node.get("active_labels"),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return new ParsedLabelMetadata(activeLabels, node.get("num_labels").asInt());
        } catch (Exception exception) {
            return null;
        }
    }

    private List<String> parseActiveLabelsFromRegistry(EmotionModelRegistryRecord record) {
        ParsedLabelMetadata labelMetadata = parseLabelMetadata(record.labelMetadataPath());
        return labelMetadata == null ? ACTIVE_FIVE_LABELS : labelMetadata.activeLabels();
    }

    private ParsedMetrics parseMetrics(String rawPath, List<String> labelOrderFallback) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Path.of(rawPath).toFile());
            JsonNode report = root.get("classification_report");
            JsonNode confusionMatrix = root.get("confusion_matrix");
            if (report == null || confusionMatrix == null || !confusionMatrix.isArray()) {
                return null;
            }
            List<String> labelOrder = root.has("evaluated_labels") && root.get("evaluated_labels").isArray()
                    ? OBJECT_MAPPER.convertValue(
                    root.get("evaluated_labels"),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)
            )
                    : labelOrderFallback;
            int happyIndex = labelOrder.indexOf("HAPPY");
            int calmIndex = labelOrder.indexOf("CALM");
            int happyToCalmCount = -1;
            if (happyIndex >= 0
                    && happyIndex < confusionMatrix.size()
                    && confusionMatrix.get(happyIndex).isArray()
                    && calmIndex >= 0
                    && calmIndex < confusionMatrix.get(happyIndex).size()) {
                happyToCalmCount = confusionMatrix.get(happyIndex).get(calmIndex).asInt(-1);
            }

            return new ParsedMetrics(
                    report.path("accuracy").asDouble(Double.NaN),
                    report.path("macro avg").path("f1-score").asDouble(Double.NaN),
                    report.path("HAPPY").path("f1-score").asDouble(Double.NaN),
                    report.path("CALM").path("f1-score").asDouble(Double.NaN),
                    happyToCalmCount
            );
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean hasJsonPath(JsonNode root, String dottedPath) {
        JsonNode current = root;
        for (String key : dottedPath.split("\\.")) {
            if (current == null || current.isMissingNode() || !current.has(key)) {
                return false;
            }
            current = current.get(key);
        }
        return current != null && !current.isMissingNode() && !current.isNull();
    }

    private String normalizePathForComparison(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            return Path.of(rawPath)
                    .toAbsolutePath()
                    .normalize()
                    .toString()
                    .toLowerCase();
        } catch (Exception exception) {
            return null;
        }
    }

    // label metadata 핵심 필드를 간단히 다루기 위한 내부 파싱 결과다.
    private record ParsedLabelMetadata(List<String> activeLabels, int numLabels) {
    }

    // promotion gate 비교에 필요한 metrics 핵심 값을 담는 내부 파싱 결과다.
    private record ParsedMetrics(
            double accuracy,
            double macroF1,
            double happyF1,
            double calmF1,
            int happyToCalmCount
    ) {
    }

    // activate guard?좏쁽??promotion checklist 怨꾩궛 寃곌낵瑜?재사용?섍린 ?꾪븳 ?대? ?붿빟??.
    private record PromotionChecklistComputation(
            String recommendation,
            boolean readyForShadow,
            boolean readyForActive,
            int passedCount,
            List<EmotionModelRegistryPromotionChecklistItemResponse> items
    ) {
    }

    private Optional<Path> resolveHappyToCalmErrorSampleCsvPath(EmotionModelRegistryRecord record) {
        if (record.metricsJsonPath() == null || record.metricsJsonPath().isBlank()) {
            return Optional.empty();
        }
        try {
            Path metricsPath = Path.of(record.metricsJsonPath());
            Path evaluationDir = metricsPath.getParent();
            if (evaluationDir == null || !Files.isDirectory(evaluationDir)) {
                return Optional.empty();
            }

            List<String> lookupTokens = buildErrorSampleLookupTokens(record.experimentName());
            try (Stream<Path> paths = Files.list(evaluationDir)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(HAPPY_TO_CALM_ERROR_SAMPLE_PREFIX))
                        .filter(path -> {
                            String lowerName = path.getFileName().toString().toLowerCase();
                            return lookupTokens.stream().anyMatch(lowerName::contains);
                        })
                        .sorted()
                        .findFirst();
            }
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private List<String> buildErrorSampleLookupTokens(String experimentName) {
        if (experimentName == null || experimentName.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String normalized = experimentName.trim().toLowerCase();
        tokens.add(normalized);
        tokens.add(normalized.replace("cpu_compare_medium_", ""));
        tokens.add(normalized.replace("cpu_compare_", ""));
        tokens.add(normalized.replace("medium_", ""));
        return tokens.stream()
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private ParsedErrorSampleCsv parseHappyToCalmErrorSampleCsv(Path csvPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IOException("CSV header is missing");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);

            Integer serviceLabelIndex = headerIndex.get("service_label");
            Integer predictedLabelIndex = headerIndex.containsKey("predicted_label")
                    ? headerIndex.get("predicted_label")
                    : headerIndex.get("v2_pred");
            Integer manualRelabelReasonIndex = headerIndex.get(MANUAL_RELABEL_REASON_COLUMN);
            boolean hasBaselinePredictionColumn = headerIndex.containsKey("baseline_pred");
            if (serviceLabelIndex == null || predictedLabelIndex == null || manualRelabelReasonIndex == null) {
                throw new IOException("required columns are missing");
            }

            int happyToCalmRows = 0;
            int reviewedRows = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                String serviceLabel = getCsvValue(values, serviceLabelIndex);
                String predictedLabel = getCsvValue(values, predictedLabelIndex);
                if (!"HAPPY".equalsIgnoreCase(serviceLabel) || !"CALM".equalsIgnoreCase(predictedLabel)) {
                    continue;
                }
                happyToCalmRows++;
                String manualRelabelReason = getCsvValue(values, manualRelabelReasonIndex);
                if (manualRelabelReason != null && !manualRelabelReason.isBlank()) {
                    reviewedRows++;
                }
            }
            return new ParsedErrorSampleCsv(happyToCalmRows, reviewedRows, hasBaselinePredictionColumn);
        }
    }

    private Optional<ParsedHappyToCalmPredictionReview> parseHappyToCalmPredictionReview(EmotionModelRegistryRecord record) {
        if (record.metricsJsonPath() == null || record.metricsJsonPath().isBlank()) {
            return Optional.empty();
        }
        try {
            Path metricsPath = Path.of(record.metricsJsonPath());
            Path evaluationDir = metricsPath.getParent();
            if (evaluationDir == null || !Files.isDirectory(evaluationDir)) {
                return Optional.empty();
            }

            String datasetToken = deriveExperimentDatasetToken(record.experimentName());
            Path baselinePredictionPath = evaluationDir.resolve("valid_predictions_baseline_on_" + datasetToken + ".csv");
            Path candidatePredictionPath = evaluationDir.resolve("valid_predictions_" + datasetToken + "_on_" + datasetToken + ".csv");
            if (!Files.exists(baselinePredictionPath) || !Files.exists(candidatePredictionPath)) {
                return Optional.empty();
            }

            Map<String, String> baselinePredictions = new HashMap<>();
            try (BufferedReader baselineReader = Files.newBufferedReader(baselinePredictionPath)) {
                String headerLine = baselineReader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    return Optional.empty();
                }
                Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
                Integer sampleIdIndex = headerIndex.get("sample_id");
                Integer predictedLabelIndex = headerIndex.get("predicted_label");
                if (sampleIdIndex == null || predictedLabelIndex == null) {
                    return Optional.empty();
                }

                String line;
                while ((line = baselineReader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    List<String> values = parseCsvLine(line);
                    baselinePredictions.put(
                            getCsvValue(values, sampleIdIndex),
                            getCsvValue(values, predictedLabelIndex)
                    );
                }
            }

            int candidateHappyToCalmCount = 0;
            int newlyFlippedHappyToCalmCount = 0;
            int baselineAlreadyHappyToCalmCount = 0;
            try (BufferedReader candidateReader = Files.newBufferedReader(candidatePredictionPath)) {
                String headerLine = candidateReader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    return Optional.empty();
                }
                Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
                Integer sampleIdIndex = headerIndex.get("sample_id");
                Integer serviceLabelIndex = headerIndex.get("service_label");
                Integer predictedLabelIndex = headerIndex.get("predicted_label");
                if (sampleIdIndex == null || serviceLabelIndex == null || predictedLabelIndex == null) {
                    return Optional.empty();
                }

                String line;
                while ((line = candidateReader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    List<String> values = parseCsvLine(line);
                    String serviceLabel = getCsvValue(values, serviceLabelIndex);
                    String predictedLabel = getCsvValue(values, predictedLabelIndex);
                    if (!"HAPPY".equalsIgnoreCase(serviceLabel) || !"CALM".equalsIgnoreCase(predictedLabel)) {
                        continue;
                    }
                    candidateHappyToCalmCount++;
                    String baselinePrediction = baselinePredictions.get(getCsvValue(values, sampleIdIndex));
                    if ("CALM".equalsIgnoreCase(baselinePrediction)) {
                        baselineAlreadyHappyToCalmCount++;
                    } else {
                        newlyFlippedHappyToCalmCount++;
                    }
                }
            }

            return Optional.of(new ParsedHappyToCalmPredictionReview(
                    candidateHappyToCalmCount,
                    newlyFlippedHappyToCalmCount,
                    baselineAlreadyHappyToCalmCount
            ));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String deriveExperimentDatasetToken(String experimentName) {
        if (experimentName == null || experimentName.isBlank()) {
            return "";
        }
        return experimentName
                .trim()
                .toLowerCase()
                .replace("cpu_compare_medium_", "")
                .replace("cpu_compare_", "");
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            headerIndex.put(normalizeHeaderName(headers.get(index)), index);
        }
        return headerIndex;
    }

    private String normalizeHeaderName(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String getCsvValue(List<String> values, int index) {
        return index < values.size() ? values.get(index).trim() : "";
    }

    private record ParsedErrorSampleCsv(int happyToCalmRows, int reviewedRows, boolean hasBaselinePredictionColumn) {
    }

    private record ParsedErrorSampleCsvReview(boolean passed, String detail) {
    }

    private record ParsedHappyToCalmPredictionReview(
            int candidateHappyToCalmCount,
            int newlyFlippedHappyToCalmCount,
            int baselineAlreadyHappyToCalmCount
    ) {
    }

    private EmotionModelRegistryRecord getRecordById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emotion model registry not found"));
    }

    private EmotionModelRegistryStatus parseStatus(String rawStatus) {
        try {
            return EmotionModelRegistryStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported registry status: " + rawStatus);
        }
    }

    private String buildStatusReason(UpdateEmotionModelRegistryStatusRequest request) {
        if (request.rejectionReason() != null && !request.rejectionReason().isBlank()) {
            return request.rejectionReason();
        }
        if (request.approvalNote() != null && !request.approvalNote().isBlank()) {
            return request.approvalNote();
        }
        return request.servingNotes();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private EmotionModelRegistryResponse toResponse(EmotionModelRegistryRecord record) {
        return new EmotionModelRegistryResponse(
                record.id(),
                record.experimentName(),
                record.modelName(),
                record.baseModelName(),
                record.artifactDir(),
                record.metricsJsonPath(),
                record.labelMetadataPath(),
                record.trainingConfigPath(),
                record.labelMapPath(),
                record.trainingDatasetTag(),
                record.validationDatasetTag(),
                record.fallbackPolicy(),
                record.status().name(),
                record.isActive(),
                record.isShadow(),
                record.accuracy(),
                record.macroF1(),
                record.happyF1(),
                record.calmF1(),
                record.anxiousF1(),
                record.sadF1(),
                record.angryF1(),
                record.servingNotes(),
                record.approvalNote(),
                record.rejectionReason(),
                record.approvedAt(),
                record.rejectedAt(),
                record.activatedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private EmotionModelRegistryStatusHistoryResponse toStatusHistoryResponse(
            EmotionModelRegistryStatusHistoryRecord record
    ) {
        return new EmotionModelRegistryStatusHistoryResponse(
                record.id(),
                record.registryId(),
                record.fromStatus(),
                record.toStatus(),
                record.changeReason(),
                record.changedAt()
        );
    }
}
