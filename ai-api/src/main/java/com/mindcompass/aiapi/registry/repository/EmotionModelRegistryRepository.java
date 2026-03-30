// 감정분류 모델 registry 테이블 접근을 담당하는 JDBC repository이다.
package com.mindcompass.aiapi.registry.repository;

import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryRecord;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatusHistoryRecord;
import com.mindcompass.aiapi.registry.domain.EmotionModelRegistryStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmotionModelRegistryRepository {

    // registry 상태별 집계 결과를 전달하는 내부 전용 요약 레코드다.
    public record RegistryStatusCountRow(String status, long count) {
    }

    private final JdbcClient jdbcClient;

    public EmotionModelRegistryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<EmotionModelRegistryRecord> findAll(
            EmotionModelRegistryStatus status,
            Boolean isActive,
            Boolean isShadow,
            String experimentName,
            Boolean currentShadowOnly
    ) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status.name());
        }
        if (isActive != null) {
            conditions.add("is_active = :isActive");
            params.put("isActive", isActive);
        }
        if (isShadow != null) {
            conditions.add("is_shadow = :isShadow");
            params.put("isShadow", isShadow);
        }
        if (experimentName != null && !experimentName.isBlank()) {
            conditions.add("experiment_name ilike :experimentName");
            params.put("experimentName", "%" + experimentName.trim() + "%");
        }
        if (currentShadowOnly != null) {
            if (currentShadowOnly) {
                conditions.add("status = :currentShadowStatus");
                params.put("currentShadowStatus", EmotionModelRegistryStatus.SHADOW.name());
            } else {
                conditions.add("status <> :currentShadowStatus");
                params.put("currentShadowStatus", EmotionModelRegistryStatus.SHADOW.name());
            }
        }

        String sql = baseSelect();
        if (!conditions.isEmpty()) {
            sql += " where " + String.join(" and ", conditions);
        }
        sql += " order by created_at desc";

        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            statementSpec = statementSpec.param(entry.getKey(), entry.getValue());
        }
        return statementSpec.query(this::mapRow).list();
    }

    public Optional<EmotionModelRegistryRecord> findActive() {
        return jdbcClient.sql(baseSelect() + " where is_active = true")
                .query(this::mapRow)
                .optional();
    }

    public Optional<EmotionModelRegistryRecord> findById(Long id) {
        return jdbcClient.sql(baseSelect() + " where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public List<EmotionModelRegistryStatusHistoryRecord> findStatusHistoryByRegistryId(Long registryId) {
        return jdbcClient.sql("""
                select
                    id,
                    registry_id,
                    from_status,
                    to_status,
                    change_reason,
                    changed_at
                from ai_internal.emotion_model_registry_status_history
                where registry_id = :registryId
                order by changed_at desc, id desc
                """)
                .param("registryId", registryId)
                .query(this::mapStatusHistoryRow)
                .list();
    }

    public List<RegistryStatusCountRow> findStatusCounts() {
        return jdbcClient.sql("""
                select status, count(*) as row_count
                from ai_internal.emotion_model_registry
                group by status
                """)
                .query((rs, rowNum) -> new RegistryStatusCountRow(
                        rs.getString("status"),
                        rs.getLong("row_count")
                ))
                .list();
    }

    public long countAll() {
        return jdbcClient.sql("select count(*) from ai_internal.emotion_model_registry")
                .query(Long.class)
                .single();
    }

    public long countShadowLineage() {
        return jdbcClient.sql("""
                select count(*)
                from ai_internal.emotion_model_registry
                where is_shadow = true
                """)
                .query(Long.class)
                .single();
    }

    public Long insert(EmotionModelRegistryRecord record) {
        return jdbcClient.sql("""
                insert into ai_internal.emotion_model_registry (
                    experiment_name,
                    model_name,
                    base_model_name,
                    artifact_dir,
                    metrics_json_path,
                    label_metadata_path,
                    training_config_path,
                    label_map_path,
                    training_dataset_tag,
                    validation_dataset_tag,
                    fallback_policy,
                    status,
                    is_active,
                    is_shadow,
                    accuracy,
                    macro_f1,
                    happy_f1,
                    calm_f1,
                    anxious_f1,
                    sad_f1,
                    angry_f1,
                    serving_notes,
                    approval_note,
                    rejection_reason,
                    approved_at,
                    rejected_at,
                    activated_at,
                    created_at,
                    updated_at
                ) values (
                    :experimentName,
                    :modelName,
                    :baseModelName,
                    :artifactDir,
                    :metricsJsonPath,
                    :labelMetadataPath,
                    :trainingConfigPath,
                    :labelMapPath,
                    :trainingDatasetTag,
                    :validationDatasetTag,
                    :fallbackPolicy,
                    :status,
                    :isActive,
                    :isShadow,
                    :accuracy,
                    :macroF1,
                    :happyF1,
                    :calmF1,
                    :anxiousF1,
                    :sadF1,
                    :angryF1,
                    :servingNotes,
                    :approvalNote,
                    :rejectionReason,
                    :approvedAt,
                    :rejectedAt,
                    :activatedAt,
                    :createdAt,
                    :updatedAt
                )
                returning id
                """)
                .param("experimentName", record.experimentName())
                .param("modelName", record.modelName())
                .param("baseModelName", record.baseModelName())
                .param("artifactDir", record.artifactDir())
                .param("metricsJsonPath", record.metricsJsonPath())
                .param("labelMetadataPath", record.labelMetadataPath())
                .param("trainingConfigPath", record.trainingConfigPath())
                .param("labelMapPath", record.labelMapPath())
                .param("trainingDatasetTag", record.trainingDatasetTag())
                .param("validationDatasetTag", record.validationDatasetTag())
                .param("fallbackPolicy", record.fallbackPolicy())
                .param("status", record.status().name())
                .param("isActive", record.isActive())
                .param("isShadow", record.isShadow())
                .param("accuracy", record.accuracy())
                .param("macroF1", record.macroF1())
                .param("happyF1", record.happyF1())
                .param("calmF1", record.calmF1())
                .param("anxiousF1", record.anxiousF1())
                .param("sadF1", record.sadF1())
                .param("angryF1", record.angryF1())
                .param("servingNotes", record.servingNotes())
                .param("approvalNote", record.approvalNote())
                .param("rejectionReason", record.rejectionReason())
                .param("approvedAt", toTimestamp(record.approvedAt()))
                .param("rejectedAt", toTimestamp(record.rejectedAt()))
                .param("activatedAt", toTimestamp(record.activatedAt()))
                .param("createdAt", toTimestamp(record.createdAt()))
                .param("updatedAt", toTimestamp(record.updatedAt()))
                .query(Long.class)
                .single();
    }

    public void updateStatus(
            Long id,
            EmotionModelRegistryStatus status,
            Boolean isShadow,
            String approvalNote,
            String rejectionReason,
            String servingNotes,
            LocalDateTime approvedAt,
            LocalDateTime rejectedAt,
            LocalDateTime updatedAt
    ) {
        jdbcClient.sql("""
                update ai_internal.emotion_model_registry
                set status = :status,
                    is_shadow = :isShadow,
                    approval_note = :approvalNote,
                    rejection_reason = :rejectionReason,
                    serving_notes = :servingNotes,
                    approved_at = :approvedAt,
                    rejected_at = :rejectedAt,
                    updated_at = :updatedAt
                where id = :id
                """)
                .param("id", id)
                .param("status", status.name())
                .param("isShadow", isShadow)
                .param("approvalNote", approvalNote)
                .param("rejectionReason", rejectionReason)
                .param("servingNotes", servingNotes)
                .param("approvedAt", toTimestamp(approvedAt))
                .param("rejectedAt", toTimestamp(rejectedAt))
                .param("updatedAt", toTimestamp(updatedAt))
                .update();
    }

    public void deactivateCurrentActive(LocalDateTime updatedAt) {
        jdbcClient.sql("""
                update ai_internal.emotion_model_registry
                set is_active = false,
                    status = 'APPROVED',
                    updated_at = :updatedAt
                where is_active = true
                """)
                .param("updatedAt", toTimestamp(updatedAt))
                .update();
    }

    public void activate(Long id, LocalDateTime activatedAt, LocalDateTime updatedAt) {
        jdbcClient.sql("""
                update ai_internal.emotion_model_registry
                set is_active = true,
                    status = 'ACTIVE',
                    activated_at = :activatedAt,
                    updated_at = :updatedAt
                where id = :id
                """)
                .param("id", id)
                .param("activatedAt", toTimestamp(activatedAt))
                .param("updatedAt", toTimestamp(updatedAt))
                .update();
    }

    public void insertStatusHistory(Long registryId, String fromStatus, String toStatus, String changeReason) {
        jdbcClient.sql("""
                insert into ai_internal.emotion_model_registry_status_history (
                    registry_id,
                    from_status,
                    to_status,
                    change_reason
                ) values (
                    :registryId,
                    :fromStatus,
                    :toStatus,
                    :changeReason
                )
                """)
                .param("registryId", registryId)
                .param("fromStatus", fromStatus)
                .param("toStatus", toStatus)
                .param("changeReason", changeReason)
                .update();
    }

    private String baseSelect() {
        return """
                select
                    id,
                    experiment_name,
                    model_name,
                    base_model_name,
                    artifact_dir,
                    metrics_json_path,
                    label_metadata_path,
                    training_config_path,
                    label_map_path,
                    training_dataset_tag,
                    validation_dataset_tag,
                    fallback_policy,
                    status,
                    is_active,
                    is_shadow,
                    accuracy,
                    macro_f1,
                    happy_f1,
                    calm_f1,
                    anxious_f1,
                    sad_f1,
                    angry_f1,
                    serving_notes,
                    approval_note,
                    rejection_reason,
                    approved_at,
                    rejected_at,
                    activated_at,
                    created_at,
                    updated_at
                from ai_internal.emotion_model_registry
                """;
    }

    private EmotionModelRegistryRecord mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new EmotionModelRegistryRecord(
                rs.getLong("id"),
                rs.getString("experiment_name"),
                rs.getString("model_name"),
                rs.getString("base_model_name"),
                rs.getString("artifact_dir"),
                rs.getString("metrics_json_path"),
                rs.getString("label_metadata_path"),
                rs.getString("training_config_path"),
                rs.getString("label_map_path"),
                rs.getString("training_dataset_tag"),
                rs.getString("validation_dataset_tag"),
                rs.getString("fallback_policy"),
                EmotionModelRegistryStatus.valueOf(rs.getString("status")),
                rs.getBoolean("is_active"),
                rs.getBoolean("is_shadow"),
                rs.getObject("accuracy", BigDecimal.class),
                rs.getObject("macro_f1", BigDecimal.class),
                rs.getObject("happy_f1", BigDecimal.class),
                rs.getObject("calm_f1", BigDecimal.class),
                rs.getObject("anxious_f1", BigDecimal.class),
                rs.getObject("sad_f1", BigDecimal.class),
                rs.getObject("angry_f1", BigDecimal.class),
                rs.getString("serving_notes"),
                rs.getString("approval_note"),
                rs.getString("rejection_reason"),
                toLocalDateTime(rs.getTimestamp("approved_at")),
                toLocalDateTime(rs.getTimestamp("rejected_at")),
                toLocalDateTime(rs.getTimestamp("activated_at")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private EmotionModelRegistryStatusHistoryRecord mapStatusHistoryRow(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        return new EmotionModelRegistryStatusHistoryRecord(
                rs.getLong("id"),
                rs.getLong("registry_id"),
                rs.getObject("from_status", String.class),
                rs.getString("to_status"),
                rs.getString("change_reason"),
                toLocalDateTime(rs.getTimestamp("changed_at"))
        );
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
