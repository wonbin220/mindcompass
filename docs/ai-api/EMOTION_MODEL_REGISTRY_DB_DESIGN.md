# Emotion Model Registry DB Design

## 1. Goal

- 감정분류 모델 실험/배포 메타데이터를 저장하는 `emotion_model_registry` 설계안을 정리한다.
- 모델 파일 자체나 대용량 CSV를 DB blob으로 넣지 않고, 운영 메타데이터와 artifact 경로만 저장하는 기준을 정한다.
- 현재 문서에 남아 있는 KcELECTRA 실험 흐름과 맞게 `manual_seed_v2`, `fixed_compare`, `TIRED fallback-only` 운영 전제를 반영한다.
- `active serving model`, `approved experiment`, `rejected experiment`, `shadow candidate` 상태를 단순하지만 확장 가능한 방식으로 관리한다.

## 2. 현재 상태 확인 결과

- 현재 저장된 실험 산출물은 `ai-api-fastapi/training/emotion_classifier/artifacts/` 아래 디렉터리 단위로 관리된다.
- 현재 주요 비교 축은 `beomi/KcELECTRA-base` 기반 감정분류 실험이다.
- 현재 문서 기준 주요 실험 상태는 아래와 같다.
  - `valid_metrics_cpu_compare_medium_relabel_weighted.json`
    - accuracy `0.4267`
    - macro F1 `0.3645`
    - 기존 학습 비교 기준선으로 사용
  - `valid_metrics_cpu_compare_medium_manual_seed_v2.json`
    - accuracy `0.4427`
    - macro F1 `0.3888`
    - macro F1 기준으로는 더 좋지만 `HAPPY -> CALM` 붕괴 이슈가 있어 최종 serving 교체는 보류
  - `valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_checkpoint156.json`
    - accuracy `0.3107`
    - macro F1 `0.1856`
    - fixed compare는 checkpoint 기준 중간 결과만 존재
- `manual_relabel_seed_v2.csv`는 승인된 `sample_id` seed만 반영하는 재현 가능한 실험 흐름으로 정리되어 있다.
- `TIRED` 관련 실험 요약 `tired_experiment_summary.json` 기준 recommendation은 `blind-expansion-stop`이며, 현재 운영 정책은 계속 `TIRED fallback-only` 유지다.
- `label_map.json`은 아직 `TIRED`를 포함한 6-label map을 보유하지만, 현재 비교 주력 실험은 active 5-label(`HAPPY`, `CALM`, `ANXIOUS`, `SAD`, `ANGRY`) 중심이다.
- `label_metadata.json`에는 `model_name`, `active_labels`, `num_labels`, train/valid distribution이 저장된다.

### 2-1. Live registration verification

- 2026-03-29 기준 draft-only 상태가 아니라 실제 registry row 등록까지 완료했다.
- live admin API execution
  - `POST /internal/admin/emotion-models`
  - `POST /internal/admin/emotion-models/1/activate`
- current baseline DB row
  - `id = 1`
  - `experiment_name = cpu_compare_medium_relabel_weighted`
  - `model_name = cpu_compare_medium_relabel_weighted_active5`
  - `base_model_name = beomi/KcELECTRA-base`
  - `status = ACTIVE`
  - `is_active = true`
  - `macro_f1 = 0.3645`
  - `accuracy = 0.4267`
  - `fallback_policy = TIRED_FALLBACK_ONLY`
- status history confirmation
  - `null -> APPROVED` with reason `created`
  - `APPROVED -> ACTIVE` with reason `activated`
- artifact mapping used for the live row
  - evaluation source:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics_cpu_compare_medium_relabel_weighted.json`
  - active artifact dir:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\cpu_compare_medium_relabel_weighted_active5`
  - label metadata path:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\cpu_compare_medium_relabel_weighted_active5\best\label_metadata.json`

## 3. Design decision

### 추천안: 단일 테이블 `emotion_model_registry`

- MVP 기준 추천안은 단일 테이블이다.
- 이유:
  - 지금 당장 필요한 것은 "어떤 실험이 있었고", "어떤 artifact를 봐야 하고", "어떤 실험이 승인/거절/서빙 중인지"를 한 곳에서 관리하는 기능이다.
  - 현재 운영 판단 축도 대부분 1행 단위 실험 메타데이터로 설명 가능하다.
  - 승인 이력, 상태 변경 이력, 세부 라벨 지표 이력까지 처음부터 분리하면 오히려 현재 MVP 범위를 넘기 쉽다.

### 단일 테이블에 저장할 것

- 운영에서 직접 필터/정렬/상태판단에 쓰는 값
  - `experiment_name`
  - `model_name`
  - `base_model_name`
  - `artifact_dir`
  - `metrics_json_path`
  - `label_metadata_path`
  - `training_config_path`
  - `label_map_path`
  - `training_dataset_tag`
  - `validation_dataset_tag`
  - `fallback_policy`
  - `status`
  - `is_active`
  - `is_shadow`
  - `macro_f1`
  - `accuracy`
  - `happy_f1`
  - `calm_f1`
  - `anxious_f1`
  - `sad_f1`
  - `angry_f1`
  - `serving_notes`
  - `approval_note`
  - `rejection_reason`
  - `approved_at`
  - `rejected_at`
  - `activated_at`
  - `created_at`
  - `updated_at`

### DB에 넣지 않고 파일/artifact 경로로만 둘 것

- 모델 본체
  - `model.safetensors`
  - tokenizer files
  - checkpoint binary
  - `training_args.bin`
- 대용량 입력/출력 데이터
  - train/valid CSV 본문
  - manual relabel candidate CSV
  - error sample CSV
  - prediction dump CSV
- 구조가 크고 조회 빈도가 낮은 평가 상세
  - confusion matrix 전체
  - full classification report 원본
  - unattended runner logs

### 왜 일부 metric은 컬럼으로, 일부는 파일로 두는가

- `macro_f1`, `happy_f1`, `calm_f1` 같은 값은 대시보드/승인 판단/정렬에 바로 쓰이므로 컬럼화하는 것이 낫다.
- 반대로 confusion matrix 전체와 full report는 운영 조회 빈도보다 보존 목적이 더 커서 JSON 파일 경로만 저장하는 편이 단순하다.

### 상태 모델 제안

- `status` enum 초안
  - `TRAINED`
  - `APPROVED`
  - `REJECTED`
  - `SHADOW`
  - `ACTIVE`
  - `ARCHIVED`
- 상태 의미
  - `TRAINED`: 학습/평가 결과는 나왔지만 승인 판단 전
  - `APPROVED`: 운영 후보로 승인됐지만 실제 serving 교체 전
  - `REJECTED`: 비교 결과나 운영 검토에서 탈락
  - `SHADOW`: shadow candidate로 비교 관찰 대상
  - `ACTIVE`: 현재 실제 serving 기준 모델
  - `ARCHIVED`: 과거 이력 보존용
- `is_active`는 빠른 조회와 유니크 인덱스를 위한 운영 플래그다.
  - `status='ACTIVE'`와 의미가 겹치지만, "현재 active row는 하나만"이라는 DB 제약을 단순하게 걸 수 있다.

### 현재 실험 흐름을 반영한 필드 제안

- `experiment_name`
  - 예: `cpu_compare_medium_relabel_weighted`
  - 예: `cpu_compare_medium_manual_seed_v2`
  - 예: `cpu_compare_medium_manual_seed_v2_fixed_compare`
- `base_model_name`
  - 현재 확인값: `beomi/KcELECTRA-base`
- `fallback_policy`
  - 현재 전제값: `TIRED_FALLBACK_ONLY`
- `training_dataset_tag`
  - 예: `emotion_mvp_manual_seed_v2`
  - 예: `emotion_mvp_relabel_weighted`
- `validation_dataset_tag`
  - 예: `emotion_mvp_manual_seed_v2_fixed_compare_medium`
  - 예: `emotion_mvp_cpu_compare_medium`
- `serving_notes`
  - 예: `manual_seed_v2 is best macro F1 but not active due to HAPPY->CALM collapse`
- `approval_note`
  - 예: `approved as shadow candidate only`
- `rejection_reason`
  - 예: `TIRED f1 remained 0.0, keep fallback-only`

### serving 정책 반영

- 현재 serving 정책은 계속 `TIRED fallback-only`다.
- 그래서 registry가 관리해야 하는 것은 "TIRED 전용 활성 모델"이 아니라:
  - active 5-label serving model
  - fallback policy metadata
  - TIRED 실험의 승인/거절 상태
- 즉 `TIRED`는 현재 별도 serving slot이 아니라 `fallback_policy`와 `serving_notes`로 관리하는 것이 MVP에 맞다.

## 4. Related files

- `C:\programing\mindcompass\docs\ai-api\EMOTION_MODEL_REGISTRY_DB_DESIGN.md`
- `C:\programing\mindcompass\docs\sql\emotion_model_registry_draft.sql`
- `C:\programing\mindcompass\docs\IMPLEMENTATION_STATUS.md`
- `C:\programing\mindcompass\docs\DB_TABLE_SPECIFICATION.md`
- `C:\programing\mindcompass\docs\ai-api\AI_API_LOGICAL_ERD.md`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics_cpu_compare_medium_relabel_weighted.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics_cpu_compare_medium_manual_seed_v2.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_checkpoint156.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\tired_experiment_summary.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\cpu_compare_medium_manual_seed_v2\best\label_metadata.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config_cpu.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\label_map.json`

## 5. SQL draft

아래 초안은 `docs/sql/emotion_model_registry_draft.sql`에 같이 정리한다.

### 5-1. 추천 단일 테이블안

```sql
create table emotion_model_registry (
    id bigserial primary key,
    experiment_name varchar(120) not null unique,
    model_name varchar(120) not null,
    base_model_name varchar(255) not null,
    artifact_dir text not null,
    metrics_json_path text not null,
    label_metadata_path text,
    training_config_path text,
    label_map_path text,
    training_dataset_tag varchar(120) not null,
    validation_dataset_tag varchar(120) not null,
    fallback_policy varchar(40) not null,
    status varchar(20) not null,
    is_active boolean not null default false,
    is_shadow boolean not null default false,
    accuracy numeric(6,4),
    macro_f1 numeric(6,4),
    happy_f1 numeric(6,4),
    calm_f1 numeric(6,4),
    anxious_f1 numeric(6,4),
    sad_f1 numeric(6,4),
    angry_f1 numeric(6,4),
    serving_notes text,
    approval_note text,
    rejection_reason text,
    approved_at timestamp,
    rejected_at timestamp,
    activated_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint chk_emotion_model_registry_status
        check (status in ('TRAINED', 'APPROVED', 'REJECTED', 'SHADOW', 'ACTIVE', 'ARCHIVED')),
    constraint chk_emotion_model_registry_fallback_policy
        check (fallback_policy in ('TIRED_FALLBACK_ONLY')),
    constraint chk_emotion_model_registry_active_status
        check (
            (is_active = true and status = 'ACTIVE')
            or (is_active = false and status in ('TRAINED', 'APPROVED', 'REJECTED', 'SHADOW', 'ARCHIVED'))
        )
);

create unique index uq_emotion_model_registry_active_true
    on emotion_model_registry (is_active)
    where is_active = true;

create index idx_emotion_model_registry_status
    on emotion_model_registry (status, created_at desc);

create index idx_emotion_model_registry_shadow
    on emotion_model_registry (is_shadow, created_at desc);

create index idx_emotion_model_registry_macro_f1
    on emotion_model_registry (macro_f1 desc nulls last);
```

### 5-2. 확장용 보조 테이블안

보조 테이블이 필요해지면 `emotion_model_registry_status_history`를 추가하는 방향이 가장 실용적이다.

```sql
create table emotion_model_registry_status_history (
    id bigserial primary key,
    registry_id bigint not null references emotion_model_registry(id),
    from_status varchar(20),
    to_status varchar(20) not null,
    change_reason text,
    changed_at timestamp not null default current_timestamp
);

create index idx_emotion_model_registry_status_history_registry_id
    on emotion_model_registry_status_history (registry_id, changed_at desc);
```

## 6. 결과

### 단일 테이블안 장점

- 지금 필요한 운영 질문을 거의 모두 1테이블로 해결할 수 있다.
  - 현재 active serving model이 무엇인가
  - approved candidate가 무엇인가
  - rejected 실험이 왜 탈락했는가
  - shadow candidate가 무엇인가
  - 현재 최고 macro F1 실험이 무엇인가
- artifact 경로 기반 운영과 잘 맞는다.
- Flyway 초안으로 옮기기 쉽다.

### 단일 테이블안 한계

- 상태 전이 이력이 누적되면 "누가 언제 승인/거절/활성화했는가" 추적이 약하다.
- 이후 다중 serving slot이나 A/B rollout이 필요하면 `is_active` 하나로는 부족해질 수 있다.

### 보조 테이블안 장점

- 승인/거절/활성화 이력이 남는다.
- 운영 변경 감사(audit) 요구가 생겨도 확장하기 쉽다.

### 보조 테이블안 한계

- 현재 MVP에서는 읽고 쓰는 코드가 늘어난다.
- 지금 문서 기준으로는 actor/audit 체계가 아직 준비되지 않아 과설계가 될 수 있다.

### 최종 추천

- 지금은 `emotion_model_registry` 단일 테이블을 먼저 도입하는 것이 맞다.
- 단, 컬럼 이름과 상태값은 이후 `status_history`를 붙일 수 있게 보수적으로 잡는다.
- 특히 현재 운영 전제인 `TIRED fallback-only`는 별도 active TIRED model 개념으로 풀지 말고 `fallback_policy='TIRED_FALLBACK_ONLY'`로 고정 관리하는 것이 좋다.

## 7. 다음 단계

1. `docs/sql/emotion_model_registry_draft.sql` 기준으로 Flyway 초안 파일명을 정한다.
2. 실제 DB 반영 주체를 결정한다.
   - 후보 1: `ai-api` 전용 PostgreSQL schema
   - 후보 2: `backend-api` DB 내 내부 운영 테이블로 관리
3. Java/Python 어느 계층이 registry를 읽고 갱신할지 정한다.
   - 학습 완료 후 등록
   - 승인/거절 상태 변경
   - active model 전환
4. 필요하면 다음 단계에서만 `emotion_model_registry_status_history`를 추가한다.

## 8. Live Registration Note

- 2026-03-29에 live `ai-api` admin API로 baseline row 등록과 active 전환을 완료했다.
- API execution
  - `POST /internal/admin/emotion-models`
  - `POST /internal/admin/emotion-models/1/activate`
- Direct DB verification
  - `ai_internal.emotion_model_registry`
    - `id = 1`
    - `experiment_name = cpu_compare_medium_relabel_weighted`
    - `model_name = cpu_compare_medium_relabel_weighted_active5`
    - `status = ACTIVE`
    - `is_active = true`
    - `macro_f1 = 0.3645`
    - `accuracy = 0.4267`
    - `fallback_policy = TIRED_FALLBACK_ONLY`
  - `ai_internal.emotion_model_registry_status_history`
    - `null -> APPROVED` with reason `created`
    - `APPROVED -> ACTIVE` with reason `activated`
- Artifact mapping for the active baseline row
  - evaluation source:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics_cpu_compare_medium_relabel_weighted.json`
  - active artifact dir:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\cpu_compare_medium_relabel_weighted_active5`
  - label metadata:
    - `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\cpu_compare_medium_relabel_weighted_active5\best\label_metadata.json`
- Promotion rule remains unchanged
  - `manual_seed_v2` and `manual_seed_v2_fixed_compare` do not replace the baseline because the fixed compare gate did not beat this row.

## 9. Status History API Note

- Registry audit visibility를 위해 read-only history endpoint를 추가했다.
- endpoint
  - `GET /internal/admin/emotion-models/{id}/history`
- why this API exists
  - 운영자가 특정 실험 row가 어떤 순서로 `APPROVED`, `ACTIVE`, `REJECTED` 등으로 바뀌었는지 바로 확인하기 위해 필요하다.
  - baseline 교체 판단이나 shadow candidate 검토 시 DB를 직접 조회하지 않아도 된다.
- request flow
  - controller가 `{id}/history` 요청을 받는다.
  - service가 먼저 registry row 존재 여부를 확인한다.
  - repository가 `ai_internal.emotion_model_registry_status_history`를 `changed_at desc, id desc`로 조회한다.
  - DTO가 최신 이력부터 반환한다.
- live verification
  - `GET /internal/admin/emotion-models/1/history`
  - response order
    - `APPROVED -> ACTIVE` reason `activated`
    - `null -> APPROVED` reason `created`

## 10. Single Lookup API Note

- Current snapshot 확인을 위해 단건 조회 endpoint를 추가했다.
- endpoint
  - `GET /internal/admin/emotion-models/{id}`
- why this API exists
  - 운영자가 목록 전체를 다시 보지 않고도 특정 registry row의 현재 상태를 바로 확인할 수 있어야 한다.
  - active row, approved candidate, rejected row를 개별 링크나 id 기준으로 빠르게 확인하기 쉽다.
- request flow
  - controller가 `{id}` 요청을 받는다.
  - service가 registry row 존재 여부를 확인한다.
  - repository가 `ai_internal.emotion_model_registry`에서 한 건을 읽는다.
  - DTO가 현재 snapshot을 반환한다.
- live verification
  - `GET /internal/admin/emotion-models/1`
  - response summary
    - `experiment_name = cpu_compare_medium_relabel_weighted`
    - `status = ACTIVE`
    - `is_active = true`
    - `macro_f1 = 0.3645`

## 11. List Filter API Note

- Registry list query utility瑜?媛뺥솕?섍린 ?꾪빐 optional filter瑜?異붽??덈떎.
- endpoint
  - `GET /internal/admin/emotion-models`
- supported query params
  - `status`
  - `isActive`
  - `isShadow`
  - `experimentName`
- why this API update exists
  - ?댁쁺?먭? active row留??ㅻⅨ 寃껋씠 ?꾨땲??approved/rejected/shadow row濡?遺꾨쪟??遺꾨Ⅴ 寃?됱븷 ???덉뼱???쒕떎.
  - baseline row媛 ?섎굹 ?덈뜑?쇰룄 future experiment媛 ?묒씠湲곗떆?먮뒗 list level filter媛 癒쇱? ?꾩슂?섎떎.
- request flow
  - controller媛 `status`, `isActive`, `isShadow`, `experimentName`瑜?optional query param?쇰줈 諛쏅뒗??
  - service媛 `status`媛 ?덈뒗 寃쎌슦 registry enum?쇰줈 寃利앺븳??
  - repository媛 provided filter留??곕씪 ?숈쟻 `where` 議곌굔???꽦?덈맂??
  - `experimentName`??`ILIKE '%keyword%'`濡???ъ냼臾몄옄瑜?臾댁떆?쒕뒗 partial match濡?議고쉶?쒕떎.
  - matching row??`created_at desc` ?쒖꽌濡?諛섑솚?쒕떎.
- filter semantics
  - `status=ACTIVE` -> exact status match
  - `isActive=true` -> exact active flag match
  - `isShadow=true` -> exact shadow candidate flag match, even after the row later moves to `APPROVED` or `REJECTED`
  - `currentShadowOnly=true` -> current `status = SHADOW` rows only
  - `currentShadowOnly=false` -> rows whose current status is not `SHADOW`
  - `experimentName=cpu_compare` -> case-insensitive partial experiment name match
  - multiple params -> `AND` combination
- verification note
  - service test added for:
    - valid filter forwarding to repository
    - unsupported `status` rejection

## 12. Shadow Flag Semantics Note

- `is_shadow` is fixed as a source flag meaning "this row was registered as a shadow candidate".
- this flag is intentionally independent from the current `status`.
- so these combinations are valid in the current design:
  - `status = SHADOW`, `is_shadow = true`
  - `status = APPROVED`, `is_shadow = true`
  - `status = REJECTED`, `is_shadow = true`
- why this decision is kept
  - live DB already contains approved/rejected rows that still need to preserve shadow-candidate lineage.
  - auto-clearing `is_shadow` on status change would blur whether a row went through shadow observation.
  - it also avoids rewriting existing live rows and keeps backward compatibility for the current list filter.
- operator guidance
  - if the operator wants "all rows that were ever managed as shadow candidates", use `isShadow=true`.
- if the operator wants "rows that are currently in shadow observation", use `currentShadowOnly=true`.
- if stricter narrowing is needed, combine both:
  - `isShadow=true&currentShadowOnly=true`

## 13. Admin List Swagger Note

- Registry admin list endpoint Swagger/OpenAPI description was strengthened on 2026-03-29.
- endpoint
  - `GET /internal/admin/emotion-models`
- why this update exists
  - operator 입장에서 `isShadow`와 `currentShadowOnly`가 모두 shadow 관련 이름이라 의미 차이를 바로 구분하기 어려웠다.
  - 그래서 endpoint description과 query parameter description에 "이력성 shadow lineage flag"와 "현재 SHADOW 상태 필터"를 명시해서 혼동을 줄였다.
- current Swagger wording intent
  - `isShadow=true`
    - "shadow candidate lineage/source로 등록된 적이 있는 row"
  - `currentShadowOnly=true`
    - "현재 `status=SHADOW` 인 row"
  - `currentShadowOnly=false`
    - "현재 `status!=SHADOW` 인 row"
  - list response examples
    - `isShadow=true` -> historical shadow lineage rows 예시
    - `currentShadowOnly=true` -> current shadow queue rows 예시
    - `isShadow=true&currentShadowOnly=false` -> shadow lineage but not current shadow rows 예시
- live verification
  - runtime: `http://localhost:8004`
  - `GET /v3/api-docs` = `200 OK`
  - confirmed in generated OpenAPI:
    - endpoint summary = `감정 모델 registry 목록 조회`
    - endpoint description includes the contrast between `isShadow` and `currentShadowOnly`
    - endpoint description includes three ready-to-use query combination examples:
      - `/internal/admin/emotion-models?isShadow=true`
      - `/internal/admin/emotion-models?currentShadowOnly=true`
      - `/internal/admin/emotion-models?isShadow=true&currentShadowOnly=false`
    - query params include per-field description and example for:
      - `status`
      - `isActive`
      - `isShadow`
      - `experimentName`
      - `currentShadowOnly`
- implementation note
  - `ai-api` added `springdoc-openapi-starter-webmvc-ui:2.8.6`
  - to avoid runtime mismatch with Spring AI transitive dependencies, `io.swagger.core.v3:swagger-annotations:2.2.29` was pinned explicitly

## 14. Admin Endpoint Swagger Usability Note

- 2026-03-29에 registry admin Swagger를 한 단계 더 다듬어 operator가 Swagger UI만 보고도 작업 순서를 더 빨리 이해할 수 있게 보강했다.
- strengthened endpoints
  - `GET /internal/admin/emotion-models/active`
  - `GET /internal/admin/emotion-models/{id}`
  - `GET /internal/admin/emotion-models/{id}/history`
  - `POST /internal/admin/emotion-models`
  - `PATCH /internal/admin/emotion-models/{id}/status`
  - `POST /internal/admin/emotion-models/{id}/activate`
- why this update exists
  - list endpoint의 filter 의미는 이미 명확해졌지만, operator가 다음 액션을 하려면 각 endpoint가 어느 운영 단계에서 쓰이는지까지 바로 보여야 했다.
  - 특히 create, status change, activate는 순서와 제약이 중요한데, Swagger 기본 표시만으로는 `ACTIVE 직접 생성 불가`, `ACTIVE 전환은 activate 전용`, `기존 active row 자동 강등` 같은 규칙이 바로 드러나지 않았다.
- current Swagger wording intent
  - `GET /active`
    - 현재 serving baseline row를 바로 확인하는 조회
  - `GET /{id}`
    - 특정 registry row snapshot 확인
  - `GET /{id}/history`
    - 상태 전이와 change reason을 최신순으로 확인
  - `POST /`
    - 새 experiment/artifact 메타데이터 등록
    - 기본 status는 `TRAINED`
    - `ACTIVE` 직접 생성 금지
  - `PATCH /{id}/status`
    - `APPROVED`, `REJECTED`, `SHADOW`, `ARCHIVED` 같은 비활성 상태 전이 전용
    - `ACTIVE` 전환은 이 endpoint가 아니라 activate endpoint 전용
  - `POST /{id}/activate`
    - 대상 row를 current active serving row로 승격
    - 기존 active row는 `APPROVED`로 자동 전환
- DTO/schema usability additions
  - request/response DTO field descriptions and examples were added so the payload shape is visible in Swagger schema without opening code.
  - create endpoint now includes a realistic shadow candidate registration example.
  - status update endpoint now includes both APPROVED and REJECTED request examples.
  - response examples were added for:
    - `GET /internal/admin/emotion-models/active`
    - `GET /internal/admin/emotion-models/{id}`
    - `GET /internal/admin/emotion-models/{id}/history`
    - `GET /internal/admin/emotion-models` shadow filter combinations

## 15. State Transition Guard Note

- 2026-03-29 기준으로 registry status transition guard를 service code에 고정했다.
- why this update exists
  - 기존 구현은 `ACTIVE`만 별도 endpoint로 막고 나머지 status 전이는 거의 모두 열려 있어서
    운영자가 `REJECTED -> APPROVED` 같은 재승인 흐름을 실수로 호출해도 코드상으로는 막히지 않았다.
  - 이번 작업에서는 "기존 row를 되살리기보다 새 row를 다시 등록/검토한다"는 운영 원칙을 반영해서
    보수적인 전이 규칙을 명시했다.
- current allowed transition matrix
  - `TRAINED -> APPROVED | REJECTED | SHADOW | ARCHIVED`
  - `APPROVED -> REJECTED | SHADOW | ARCHIVED`
  - `SHADOW -> APPROVED | REJECTED | ARCHIVED`
  - `REJECTED -> ARCHIVED`
  - `ARCHIVED -> none`
  - `ACTIVE -> status update API blocked`
- request validation rules
  - `APPROVED`로 이동할 때는 `approvalNote`가 필수다.
  - `REJECTED`로 이동할 때는 `rejectionReason`이 필수다.
  - 현재 active row는 `PATCH /status`로 직접 변경할 수 없고 activate flow로만 교체된다.
- activation rule
  - `POST /internal/admin/emotion-models/{id}/activate`는 이제 `APPROVED` row에만 허용된다.
  - 이미 active인 같은 row에 다시 activate를 호출하면 history를 추가하지 않고 현재 snapshot을 그대로 반환한다.
- shadow lineage rule
  - non-shadow row가 `SHADOW` 상태로 이동하면 `is_shadow`를 자동으로 `true`로 올린다.
  - 이유는 shadow queue에 한 번 들어간 row는 이후 `APPROVED`/`REJECTED`가 되더라도 lineage를 남겨야 하기 때문이다.

## 16. Admin Ops Convenience API Note

- 운영 편의를 위해 조회 API 2개를 추가했다.
- endpoints
  - `GET /internal/admin/emotion-models/summary`
  - `GET /internal/admin/emotion-models/{id}/transitions`
- why these APIs exist
  - `summary`
    - 목록 필터를 여러 번 조합하기 전에 현재 운영 상태를 한눈에 파악하기 위한 API다.
    - 상태별 row 개수, shadow lineage 개수, 현재 active row id/experiment 이름을 반환한다.
  - `transitions`
    - 특정 row에서 다음에 가능한 status 변경과 activate 가능 여부를 바로 확인하기 위한 API다.
    - admin UI 버튼 제어, 운영 스크립트 사전 확인, 수동 검토 체크리스트에 바로 쓸 수 있다.
- request flow
  - `GET /summary`
    - controller -> service -> repository status group-by query + active row lookup -> summary DTO
  - `GET /{id}/transitions`
    - controller -> service -> current row lookup -> transition matrix evaluation -> transition DTO
- live verification
  - runtime: `http://localhost:8004`
  - `GET /internal/admin/emotion-models/summary`
    - result summary:
      - `totalCount = 3`
      - `activeCount = 1`
      - `approvedCount = 1`
      - `rejectedCount = 1`
      - `shadowCount = 0`
      - `shadowLineageCount = 2`
      - `activeRegistryId = 1`
  - `GET /internal/admin/emotion-models/2/transitions`
    - result summary:
      - `currentStatus = APPROVED`
      - `allowedStatusUpdates = [REJECTED, SHADOW, ARCHIVED]`
      - `canActivate = true`

## 17. Artifact Health Check API Note

- registry row에 저장된 artifact 경로가 실제 파일시스템에 존재하는지 확인하는 운영용 read API를 추가했다.
- endpoint
  - `GET /internal/admin/emotion-models/{id}/artifact-health`
- why this API exists
  - status가 `APPROVED` 또는 `ACTIVE`라고 해도 실제 artifact 디렉터리나 metric JSON 경로가 깨져 있으면 운영자가 activate 전후에 바로 문제를 파악하기 어렵다.
  - 그래서 DB 메타데이터 조회와 실제 경로 존재 여부 점검을 분리해서, 운영자가 파일 경로 문제를 먼저 확인할 수 있게 했다.
- checked items
  - required
    - `artifactDir`
    - `metricsJsonPath`
  - optional-if-configured
    - `labelMetadataPath`
    - `trainingConfigPath`
    - `labelMapPath`
- response intent
  - `requiredArtifactsHealthy`
    - 필수 경로 기준으로 activate 전 최소 안전 상태를 확인하기 위한 값
  - `overallHealthy`
    - 설정된 선택 경로까지 포함한 전체 운영 상태를 확인하기 위한 값
  - `missingRequiredItems`
    - 즉시 조치가 필요한 누락 목록
  - `missingOptionalItems`
    - 메타데이터 보강이나 문서 정리가 필요한 누락 목록
  - `items`
    - 각 경로별 `configured`, `exists`, `directoryExpected`, `directory`, `errorMessage`를 상세 반환
- request flow
  - controller가 `{id}/artifact-health` 요청을 받는다.
  - service가 registry row 존재 여부를 먼저 확인한다.
  - service가 각 경로를 `Path`로 해석하고 파일/디렉터리 존재 여부를 점검한다.
  - 점검 결과를 요약 필드와 item 리스트로 묶어서 반환한다.
- live verification
  - runtime: `http://localhost:8004`
  - `GET /internal/admin/emotion-models/1/artifact-health`
    - result summary:
      - `requiredArtifactsHealthy = true`
      - `overallHealthy = true`
      - baseline active row의 5개 점검 경로가 모두 존재함
  - `GET /internal/admin/emotion-models/2/artifact-health`
    - result summary:
      - `requiredArtifactsHealthy = true`
      - `overallHealthy = true`
      - approved shadow-lineage row의 점검 경로도 모두 존재함
- current scope note
  - 이번 API는 존재 여부와 디렉터리/파일 타입만 점검한다.
  - JSON 파싱 가능 여부, artifact 내부 필드 유효성, 모델 파일 integrity는 다음 단계의 별도 check로 분리하는 것이 MVP 범위에 맞다.

## 18. Artifact JSON Parse Check API Note

- 파일 존재 여부 다음 단계로, registry row에 저장된 JSON artifact를 실제로 읽어서 parse/schema check를 수행하는 read API를 추가했다.
- endpoint
  - `GET /internal/admin/emotion-models/{id}/artifact-json-check`
- checked JSON files
  - required
    - `metricsJsonPath`
  - optional-if-configured
    - `labelMetadataPath`
    - `trainingConfigPath`
    - `labelMapPath`
- minimum schema rules
  - `metricsJsonPath`
    - `classification_report`
    - `confusion_matrix`
    - `classification_report.accuracy`
    - `classification_report.macro avg`
  - `labelMetadataPath`
    - `model_name`
    - `active_labels`
    - `num_labels`
    - `train_distribution`
    - `valid_distribution`
  - `trainingConfigPath`
    - `model_name`
    - `max_length`
    - `num_labels`
    - `num_train_epochs`
    - `learning_rate`
  - `labelMapPath`
    - `id_to_label`
    - `label_to_id`
    - `label_to_tags`
- response intent
  - `parseHealthy`
    - 설정된 JSON이 모두 정상 파싱되는지 확인
  - `requiredSchemaHealthy`
    - 필수 JSON이 최소 스키마를 만족하는지 확인
  - `overallSchemaHealthy`
    - 설정된 JSON 전체가 최소 스키마를 만족하는지 확인
- live verification
  - `GET /internal/admin/emotion-models/1/artifact-json-check`
    - `parseHealthy = true`
    - `requiredSchemaHealthy = true`
    - `overallSchemaHealthy = true`
  - `GET /internal/admin/emotion-models/2/artifact-json-check`
    - `parseHealthy = true`
    - `requiredSchemaHealthy = true`
    - `overallSchemaHealthy = true`

## 19. Promotion Checklist Validation API Note

- 운영 승격 판단을 빠르게 내리기 위한 checklist validation read API를 추가했다.
- endpoint
  - `GET /internal/admin/emotion-models/{id}/promotion-checklist`
- why this API exists
  - 기존에는 문서 체크리스트가 있었지만, 운영자가 registry row 하나를 보고 바로 "지금 active 후보인지, shadow까지만 가능한지"를 코드 기준으로 판단할 수 있는 API가 없었다.
  - 이번 API는 artifact health, JSON check, label contract, fallback policy, baseline metric 비교를 한 번에 묶어 recommendation까지 돌려준다.
- recommendation values
  - `BLOCKED`
    - core prerequisite를 통과하지 못해서 shadow 후보로도 권장하지 않음
  - `SHADOW_ONLY`
    - core prerequisite는 통과했지만 baseline active 승격 gate는 통과하지 못함
  - `ACTIVE_CANDIDATE`
    - active 승격 gate까지 모두 통과
  - `ACTIVE_ALREADY`
    - 현재 active baseline row 자체
- current gate rules
  - artifact health required pass
  - JSON required schema pass
  - `fallback_policy = TIRED_FALLBACK_ONLY`
  - `label_metadata.active_labels = [HAPPY, CALM, ANXIOUS, SAD, ANGRY]`
  - `label_metadata.num_labels = 5`
  - status gate
    - shadow/open 검토 가능 상태
    - active 승격 가능 상태는 `APPROVED` 또는 `ACTIVE`
  - baseline compare gate
    - `macroF1 >= baseline`
    - `happyF1 >= baseline`
    - `calmF1 >= baseline`
    - `HAPPY -> CALM <= baseline`
- live verification
  - `GET /internal/admin/emotion-models/1/promotion-checklist`
    - result summary:
      - `recommendation = ACTIVE_ALREADY`
      - all checklist items passed
  - `GET /internal/admin/emotion-models/2/promotion-checklist`
    - result summary:
      - `recommendation = SHADOW_ONLY`
      - passed:
        - artifact health
        - JSON schema
        - fallback policy
        - active label contract
        - status gates
        - `macroF1`
        - `calmF1`
      - failed:
        - `happyF1`
        - `HAPPY -> CALM`
- interpretation note
  - 현재 live row `cpu_compare_medium_manual_seed_v2`는 macro F1은 baseline보다 좋지만, `HAPPY F1`과 `HAPPY -> CALM` gate를 통과하지 못하므로 active 승격 대신 `SHADOW_ONLY`가 맞다는 문서 판단을 API 결과로도 재현했다.
## 20. Activate Guard Note

- 2026-03-29 기준으로 registry admin activation은 DB status만 보고 끝내지 않고 promotion checklist 결과까지 함께 확인한다.
- activation rule
  - 대상 row status는 `APPROVED`여야 한다.
  - checklist recommendation은 `ACTIVE_CANDIDATE`여야 한다.
  - 현재 active row는 예외적으로 재활성화 없이 그대로 반환된다.
- why this matters
  - registry row에 `APPROVED`가 찍혀 있어도 artifact/JSON/metric gate가 다시 깨진 상태라면 serving 승격을 막아야 한다.
  - 그래서 activation은 단순 status 전환이 아니라 "현재 artifact 상태 + 현재 baseline 비교"를 다시 확인하는 운영 명령으로 본다.
- operator implication
  - `/promotion-checklist`와 `/activate`가 같은 기준을 공유한다.
  - future admin UI나 script도 이 규칙을 그대로 따라야 한다.

## 21. Runtime Alignment Note

- 2026-03-29 기준으로 registry DB snapshot과 FastAPI serving runtime snapshot을 나란히 비교하는 read API를 추가했다.
- why this matters
  - `emotion_model_registry.is_active = true`만으로는 실제 FastAPI runtime이 같은 artifact를 로드 중인지 보장되지 않는다.
  - 특히 `EMOTION_MODEL_DIR`가 env로 따로 주입되면 DB active row와 runtime이 쉽게 어긋날 수 있다.
- current design
  - FastAPI는 `/internal/model/runtime-info`에서 다음 값을 반환한다.
    - `modelDirConfigured`
    - `modelDirResolved`
    - `modelDirExists`
    - `modelLoadSource`
    - `labelMapPathResolved`
  - ai-api는 active registry row의 `artifactDir`와 위 값을 비교해 `/internal/admin/emotion-models/active/runtime-alignment`로 반환한다.
- current decision rule
  - `artifactDirAligned = true`
    - active registry `artifactDir` 절대 경로 == FastAPI `modelDirResolved` 절대 경로
  - `overallAligned = true`
    - `artifactDirAligned = true`
    - `modelDirExists = true`
- operator implication
  - activation 이후 DB row만 확인하지 말고 runtime alignment도 같이 확인해야 한다.
  - future serving promotion checklist에 이 항목을 gate로 포함할지 여부는 다음 단계에서 결정하면 된다.
