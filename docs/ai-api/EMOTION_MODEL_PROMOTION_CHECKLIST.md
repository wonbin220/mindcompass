# Emotion Model Promotion Checklist

> 2026-03-30 update
>
> - `promotion-checklist`에 `errorSampleCsvGate`를 추가했다.
> - 기준 artifact는 evaluation 디렉터리의 `happy_to_calm_errors_<experiment>.csv`다.
> - `HAPPY -> CALM > 0` candidate는
>   CSV 존재, metrics count 일치, `manual_relabel_reason` 전부 작성 완료를 모두 만족해야 active promotion 후보로 통과한다.
> - live check
>   - registry `1`: pass, `already active baseline; CSV review gate skipped`
>   - registry `2`: fail, `csv=happy_to_calm_errors_manual_seed_v2.csv, exportMode=newly_flipped_only, newlyFlippedRows=107, reviewedRows=0, predictionNewlyFlipped=107, baselineAlreadyCalm=17, metricsHappyToCalm=124`
> - interpretation
>   - `124`는 candidate 전체 `HAPPY -> CALM` count다.
>   - `107`은 baseline 대비 새로 `CALM`으로 무너진 row만 export한 legacy CSV count다.
>   - 즉 현재 mismatch는 누락이라기보다 export 기준 차이이고, 실제 남은 blocker는 `manual_relabel_reason` 미작성이다.

> - 2026-03-30 progress update
>   - `happy_to_calm_errors_manual_seed_v2.csv` manual review is now complete: `total=107`, `reviewed=107`, `blank=0`
>   - current remaining work moved from CSV blank cleanup to `manual_seed_v3` candidate narrowing and fixed compare re-check
>   - live `promotion-checklist` API result has not been re-run in this session, so any registry recommendation shown below should still be read as the previous runtime snapshot
>   - reviewed CALM-only pre-check candidate size: `46`
>   - reviewed fixed compare pre-check still leaves a strong HAPPY/CALM tradeoff, so this reviewed set is not yet a direct promotion seed
>   - narrower safety-first subset `narrow15` also still leaves `manual_seed_v2` with severe `HAPPY -> CALM` collapse, so label-side expansion should stay secondary to model-side mitigation

## 0.1 manual_relabel_reason review rule

- 이번 CSV 리뷰의 목적은 `manual_seed_v2`를 바로 승격하는 것이 아니라, `manual_seed_v3` 후보에 넣어도 되는 아주 좁은 boundary-cleanup 근거만 남기는 것이다.
- reason 코드는 문장 전체 정서를 한 줄로 요약하는 용도가 아니라, 왜 이 row를 `CALM` 쪽으로 보수적으로 승인했는지 추적하는 용도다.
- 이번 턴에서 먼저 쓰는 승인 코드는 아래 4개로 고정한다.
  - `RELABEL_TO_CALM_RELIEF_AND_REASSURANCE`
  - `RELABEL_TO_CALM_STABILITY_AND_PEACE_OF_MIND`
  - `RELABEL_TO_CALM_GRATITUDE_WITH_LOW_AROUSAL`
  - `RELABEL_TO_CALM_RECOVERY_AND_SETTLING`
- 2차 리뷰부터 쓰는 `KEEP_HAPPY` 코드는 아래 4개로 고정한다.
  - `KEEP_HAPPY_HIGH_AROUSAL_EXCITEMENT`
  - `KEEP_HAPPY_ACTIVE_CELEBRATION_AND_REWARD`
  - `KEEP_HAPPY_PROUD_RECOGNITION_AND_PRAISE`
  - `KEEP_HAPPY_CONFIDENT_FORWARD_MOMENTUM`
- 적용 우선순위
  1. `안도/다행/마음 편함/여유`가 명시되면 먼저 `RELIEF_AND_REASSURANCE` 또는 `STABILITY_AND_PEACE_OF_MIND`를 검토한다.
  2. `감사`가 중심이지만 축하/흥분보다 `안식처`, `무탈`, `편안함`, `돌봄에 대한 고마움`이 중심이면 `GRATITUDE_WITH_LOW_AROUSAL`을 쓴다.
  3. 건강 회복, 증상 완화, 퇴원 직후 안정처럼 긴장 하강이 핵심이면 `RECOVERY_AND_SETTLING`을 쓴다.
  4. `신나`, `날아갈 것 같다`, 즉시 축하/소비/자랑/행동 충동이 강하면 `KEEP_HAPPY_HIGH_AROUSAL_EXCITEMENT` 또는 `KEEP_HAPPY_ACTIVE_CELEBRATION_AND_REWARD`를 검토한다.
  5. 칭찬, 승진, 성취 인정, 자랑, 발표 자신감처럼 앞으로 더 밀고 나가는 추진감이 강하면 `KEEP_HAPPY_PROUD_RECOGNITION_AND_PRAISE` 또는 `KEEP_HAPPY_CONFIDENT_FORWARD_MOMENTUM`을 검토한다.
- 즉 현재 리뷰는 `느긋`, 일부 `감사하는`, 일부 `만족스러운`의 저각성 `CALM` 승인 row와, `신이 난`/`기쁨`/`자신하는`의 고각성 `HAPPY` 유지 row를 분리해서 채운다.

## 1. Goal

- `ai-api-fastapi` KcELECTRA 학습 결과를 나중에 `registry -> serving -> ai-api` 흐름으로 안전하게 연결하기 위한 체크리스트를 정리한다.
- 다음 작업자가 `happy_to_calm_errors_manual_seed_v2.csv` 분석 이후 `manual_seed_v3` 후보를 만들 때도 같은 기준으로 승격 여부를 판단할 수 있게 한다.

## 2. Design decision

- 학습 파이프라인과 서빙 파이프라인은 계속 분리한다.
- 대신 운영 전환에 필요한 메타데이터를 `ai-api` registry에 등록해서 두 계층을 연결한다.
- 현재 active serving baseline은 `cpu_compare_medium_relabel_weighted`이고, `manual_seed_v2` 계열은 `HAPPY -> CALM` 붕괴 때문에 아직 승격하지 않는다.
- serving 정책은 계속 `TIRED fallback-only`를 유지한다.

## 3. Flow summary

1. `ai-api-fastapi/training/emotion_classifier`에서 train/eval 결과를 만든다.
2. 승격 후보의 artifact 경로, metric JSON, label metadata, fallback policy를 `ai-api` registry에 등록한다.
3. `ai-api-fastapi` serving runtime이 registry 기준 active model path를 읽어 실제 모델을 로드하도록 맞춘다.
4. `ai-api`는 `ai-api-fastapi`의 emotion inference 결과를 내부적으로 호출해서 diary/chat flow에 반영한다.

## 4. Training artifact checklist

### Why this step exists

- registry와 serving이 공통으로 참조할 실체를 학습 결과에서 먼저 고정해야 한다.

### Required files

- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\<experiment>\best\model.safetensors`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\<experiment>\best\config.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\<experiment>\best\tokenizer.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\<experiment>\best\label_metadata.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\<metrics>.json`

### Files to verify

- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\scripts\train_emotion_classifier.py`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\scripts\evaluate_emotion_classifier.py`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config_cpu.json`
- `C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\label_map.json`

### Promotion gate

- `label_metadata.json`의 `active_labels`와 `num_labels`가 실제 serving 계약과 맞아야 한다.
- 현재 기준은 active 5-label (`HAPPY`, `CALM`, `ANXIOUS`, `SAD`, `ANGRY`)이다.
- `TIRED`는 서빙 primary label로 승격하지 않고 fallback policy metadata로만 유지한다.
- 다음 실험은 macro F1만 보지 말고 반드시 `HAPPY F1`, `CALM F1`, `HAPPY -> CALM` 오분류를 같이 본다.

## 5. Registry checklist

### Why this step exists

- 어떤 실험이 운영 후보인지, 어떤 모델이 active인지, 왜 reject됐는지를 `ai-api`가 일관되게 관리하기 위해 필요하다.

### Files to verify

- `C:\programing\mindcompass\docs\ai-api\EMOTION_MODEL_REGISTRY_DB_DESIGN.md`
- `C:\programing\mindcompass\ai-api\src\main\resources\db\migration\V1__emotion_model_registry.sql`
- `C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\registry\controller\EmotionModelRegistryAdminController.java`
- `C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\registry\service\EmotionModelRegistryService.java`

### Registry fields that must be filled

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
- `accuracy`
- `macro_f1`
- `happy_f1`
- `calm_f1`
- `anxious_f1`
- `sad_f1`
- `angry_f1`
- `serving_notes`
- `status`

### Current confirmed state

- baseline row is already live-registered as active.
- current active experiment: `cpu_compare_medium_relabel_weighted`
- current fallback policy: `TIRED_FALLBACK_ONLY`

### Decision rule

- `manual_seed_v2` or `manual_seed_v3`가 baseline을 이겨도 `HAPPY -> CALM` 붕괴가 심하면 `APPROVED` 또는 `SHADOW`까지만 가능하고 바로 `ACTIVE`로 올리지 않는다.

## 6. ai-api-fastapi serving checklist

### Why this step exists

- 학습 결과가 실제 inference runtime에 반영되려면 serving이 읽는 model path, label map, fallback rule이 고정되어야 한다.

### Files to verify

- `C:\programing\mindcompass\ai-api-fastapi\app\routers\model_router.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\services\emotion_classifier_service.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\inference\predictor.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\inference\label_mapper.py`

### Current serving contract

- endpoint: `POST /internal/model/emotion-classify`
- predictor model source:
  - env `EMOTION_MODEL_DIR`
  - fallback path `ai-api-fastapi/training/emotion_classifier/artifacts/best`
- label map source:
  - env `EMOTION_LABEL_MAP_PATH`
  - fallback path `ai-api-fastapi/training/emotion_classifier/configs/label_map.json`
- `primaryEmotion == TIRED`이면 `CALM` fallback response로 바꾼다.
- response에는 `fallbackUsed`와 `fallbackReason`이 포함된다.

### Before promotion

- active registry row의 `artifact_dir`와 FastAPI runtime의 `EMOTION_MODEL_DIR`가 같은 위치를 가리키게 맞춰야 한다.
- active registry row의 `label_metadata_path`와 serving label contract가 충돌하지 않는지 확인해야 한다.
- `label_map.json`이 6-label을 유지하더라도, 실제 model head가 5-label이면 serving path에서 index mismatch가 나지 않도록 검증해야 한다.

## 7. ai-api integration checklist

### Why this step exists

- 최종적으로는 `backend-api -> ai-api -> ai-api-fastapi` 흐름에서 감정 분석 결과가 깨지지 않아야 한다.

### Files to verify

- `C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\controller\DiaryAiController.java`
- `C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\service\DiaryAnalysisService.java`
- `C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\AnalyzeDiaryResponse.java`
- `C:\programing\mindcompass\docs\ai-api\ANALYZE_DIARY_API_LEARNING.md`

### Current caution

- current `DiaryAnalysisService` is still Spring-AI prompt/fallback centered.
- it is not yet wired to consume `ai-api-fastapi` emotion-classify output as the main inference source.
- so registry and FastAPI serving are prepared, but final ai-api runtime wiring is still a remaining integration step.

### Integration rule

- `ai-api`가 `ai-api-fastapi`를 붙일 때도 response contract는 최소한 아래 값을 유지해야 한다.
  - `primaryEmotion`
  - `emotionTags`
  - `confidence`
- fallback이 발생해도 diary flow 전체가 깨지지 않아야 한다.

## 8. Practical promotion checklist for next experiment

1. `happy_to_calm_errors_manual_seed_v2.csv`에서 boundary-cleanup 대상만 다시 추린다.
   - 1차는 `manual_relabel_reason`을 채운 저각성 `CALM` 승인 row부터 확정한다.
2. broad relabel 없이 approved `sample_id`만 사용해 `manual_relabel_seed_v3.csv`를 만든다.
3. fixed compare CSV 기준으로 재학습/재평가한다.
4. baseline 대비 아래 항목을 같이 비교한다.
   - accuracy
   - macro F1
   - `HAPPY F1`
   - `CALM F1`
   - `HAPPY -> CALM`
5. 결과가 통과하면 registry에 새 row를 `TRAINED` 또는 `APPROVED`로 등록한다.
6. shadow 검증이 필요하면 `SHADOW` 상태로 유지한다.
7. 운영 승격이 가능하면 active row를 전환하고 FastAPI serving model path를 맞춘다.
8. 마지막으로 `ai-api` diary/chat flow에서 새 serving 결과를 읽는 통합 테스트를 한다.

## 9. Next worker handoff

- immediate next task is not serving promotion.
- immediate next task is `happy_to_calm_errors_manual_seed_v2.csv` 집계 후 `manual_seed_v3` 승인 후보를 좁히는 것이다.
- until that result clears the fixed compare gate, keep:
  - baseline = `cpu_compare_medium_relabel_weighted`
  - serving policy = `TIRED fallback-only`
  - active registry row = baseline

## 10. Registry Checklist Validation API Note

- 2026-03-29 기준으로 문서 체크리스트를 registry admin API에서도 바로 확인할 수 있게 만들었다.
- endpoints
  - `GET /internal/admin/emotion-models/{id}/artifact-json-check`
  - `GET /internal/admin/emotion-models/{id}/promotion-checklist`
- why this update exists
  - 다음 worker가 문서만 보고 수동 판단하는 대신, 현재 registry row 하나를 같은 기준으로 즉시 검증할 수 있게 하기 위함이다.
- current recommendation values
  - `ACTIVE_ALREADY`
  - `ACTIVE_CANDIDATE`
  - `SHADOW_ONLY`
  - `BLOCKED`
- current live result
  - `GET /internal/admin/emotion-models/1/promotion-checklist`
    - `recommendation = ACTIVE_ALREADY`
  - `GET /internal/admin/emotion-models/2/promotion-checklist`
    - `recommendation = SHADOW_ONLY`
    - failed gates:
      - `happyF1Gate`
      - `happyToCalmGate`
- interpretation
  - 문서에서 이미 정리한 것처럼 `manual_seed_v2`는 macro F1만 보면 baseline보다 좋지만,
    `HAPPY F1` 하락과 `HAPPY -> CALM` 붕괴 때문에 바로 `ACTIVE`로 올리지 않는 것이 맞다.
  - 이제 이 판단을 문서뿐 아니라 API 결과로도 같은 방식으로 재현할 수 있다.
## 11. Activate Guard Note

- 2026-03-29 기준으로 `POST /internal/admin/emotion-models/{id}/activate`는 promotion checklist를 자동으로 선행 검증한다.
- activation allowed rule
  - 대상 row는 먼저 `APPROVED` 상태여야 한다.
  - 같은 row의 checklist recommendation이 `ACTIVE_CANDIDATE`일 때만 실제 활성화가 진행된다.
  - 이미 active인 row는 현재 snapshot을 그대로 반환한다.
- blocked rule
  - checklist recommendation이 `SHADOW_ONLY` 또는 `BLOCKED`이면 activate는 `400 Bad Request`로 차단된다.
  - 응답 reason에는 recommendation 값과 실패한 gate 이름 목록이 포함되어 운영자가 바로 원인을 볼 수 있다.
- why this update exists
  - 이전에는 운영자가 `promotion-checklist`를 먼저 보고 `/activate`를 따로 호출해야 해서, 승인 상태만으로 활성화가 가능한 빈틈이 있었다.
  - 이번 가드로 checklist API와 activate 실행 규칙이 같은 기준을 공유하게 됐다.
- current operator guidance
  - `GET /internal/admin/emotion-models/{id}/promotion-checklist`로 사전 확인
  - recommendation이 `ACTIVE_CANDIDATE`일 때만 `/activate` 실행
  - `SHADOW_ONLY`면 shadow/approved 유지, `BLOCKED`면 artifact 또는 schema 문제부터 수정

## 12. narrow15 Reference Subset Note

- 2026-03-30 update
  - `manual_relabel_seed_v3_happy_to_calm_reviewed_narrow15.csv` is now treated as the final reference subset for HAPPY/CALM boundary tracking
  - related frozen compare file:
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare_medium.csv`
- why this update exists
  - the reviewed narrow set is small enough to stay conservative, but still large enough to expose the same directional tradeoff that blocked promotion on the broader fixed compare gate
  - this makes it useful as a stable reference slice for model-side mitigation checks
- interpretation rule
  - `narrow15` is a reference subset, not the next relabel expansion seed
  - passing `narrow15` alone is not enough for promotion, but failing `narrow15` is a strong early warning that the branch is still directionally unstable
- latest check
  - baseline `cpu_compare_medium_relabel_weighted`
    - accuracy `0.4013`
    - macro F1 `0.3513`
    - `HAPPY -> CALM = 29/135`
    - `CALM -> HAPPY = 108/165`
  - `manual_seed_v2`
    - accuracy `0.4267`
    - macro F1 `0.3612`
    - `HAPPY -> CALM = 109/135`
    - `CALM -> HAPPY = 12/165`
  - `hidden_dropout25` checkpoint pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout25_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.3893`
    - macro F1 `0.2911`
    - `HAPPY -> CALM = 41/135`
    - `CALM -> HAPPY = 84/165`
  - `attention_dropout20` checkpoint pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_attention_dropout20_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.3760`
    - macro F1 `0.2213`
    - `HAPPY -> CALM = 128/135`
    - `CALM -> HAPPY = 0/165`
  - `hidden_dropout20 + happy_to_calm_penalty_weight 0.1` pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty10_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.3627`
    - macro F1 `0.2469`
    - `HAPPY -> CALM = 0/135`
    - `CALM -> HAPPY = 164/165`
  - `hidden_dropout20 + happy_to_calm_penalty_weight 0.02` pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty02_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.4293`
    - macro F1 `0.3913`
    - `HAPPY -> CALM = 16/135`
    - `CALM -> HAPPY = 114/165`
  - `hidden_dropout20 + happy_to_calm_penalty_weight 0.015` pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty015_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.3880`
    - macro F1 `0.3083`
    - `HAPPY -> CALM = 9/135`
    - `CALM -> HAPPY = 153/165`
  - `hidden_dropout20 + happy_to_calm_penalty_weight 0.018` pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty018_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.4133`
    - macro F1 `0.3585`
    - `HAPPY -> CALM = 2/135`
    - `CALM -> HAPPY = 160/165`
  - `hidden_dropout20 + happy_to_calm_penalty_weight 0.019` pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty019_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.4040`
    - macro F1 `0.3065`
    - `HAPPY -> CALM = 1/135`
    - `CALM -> HAPPY = 164/165`
- working rule for the next session
  - keep `narrow15` frozen
  - prefer one single-knob model-side experiment at a time
  - do not reopen broad relabel expansion unless a model-side run first improves both overall metrics and directional counts on this reference slice
  - do not spend another session on dropout-only tuning before changing the objective or loss design
  - directional penalties must now be treated like dropout knobs:
    - if one side goes to near-zero while the reverse side explodes, stop at the narrow reference gate and do not continue to broad fixed compare
  - next session priority:
    - keep `0.02` as the best narrow15 reference in the one-sided penalty family
    - do not run the broad fixed compare gate unless a new narrow15 result is clearly more balanced than both baseline and `0.02`
    - treat the sub-`0.02` one-sided penalty descent as exhausted for now
    - move the next continuation to a small balanced two-sided loss-side adjustment instead of another smaller one-sided penalty
    - if local time is already close to the stop window, do not start a fresh training run; leave the session with `0.02` as the current best narrow15 anchor

## 13. Runtime Alignment Note

- 2026-03-29 기준으로 active registry row와 FastAPI serving runtime의 실제 model dir 정합성을 확인하는 read API를 추가했다.
- endpoints
  - FastAPI: `GET /internal/model/runtime-info`
  - ai-api: `GET /internal/admin/emotion-models/active/runtime-alignment`
- why this update exists
  - registry의 active row가 바뀌어도 FastAPI runtime이 여전히 이전 `EMOTION_MODEL_DIR`를 보고 있으면 실제 serving 상태와 DB 상태가 어긋날 수 있다.
  - activate 이후 운영자가 바로 "DB active artifact"와 "FastAPI runtime model dir"를 같은 기준으로 확인할 수 있어야 한다.
- current check fields
  - active registry `artifactDir`
  - FastAPI `modelDirConfigured`
  - FastAPI `modelDirResolved`
  - FastAPI `modelDirExists`
  - FastAPI `modelLoadSource`
  - FastAPI `labelMapPathResolved`
- current alignment rule
  - `artifactDirAligned`
    - active registry `artifactDir` 절대 경로와 FastAPI `modelDirResolved` 절대 경로가 같아야 한다.
  - `overallAligned`
    - `artifactDirAligned = true`
    - `modelDirExists = true`
- operator guidance
  - activate 직후 `GET /internal/admin/emotion-models/active/runtime-alignment` 호출
  - `overallAligned = false`면 FastAPI runtime env 또는 기동 경로부터 먼저 수정
## 14. 2026-03-30 balanced two-sided loss update

- latest check
  - `hidden_dropout20 + happy_calm_bidirectional_penalty_weight 0.0025` broad fixed compare
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_bidirectional_penalty0025_v1.json`
    - accuracy `0.3667`
    - macro F1 `0.2167`
    - `happy_calm_macro_f1 = 0.3349`
    - `HAPPY -> CALM = 142/150`
    - `CALM -> HAPPY = 0/150`
  - `hidden_dropout20 + happy_calm_bidirectional_penalty_weight 0.0025` narrow15 pre-check
    - metrics file:
      - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_bidirectional_penalty0025_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json`
    - accuracy `0.3867`
    - macro F1 `0.2250`
    - `happy_calm_macro_f1 = 0.3568`
    - `HAPPY -> CALM = 127/135`
    - `CALM -> HAPPY = 0/165`
- interpretation
  - the first balanced two-sided loss-side candidate failed clearly
  - symmetric pressure did not improve balance; it over-protected `CALM` so strongly that `HAPPY` recall collapsed
  - this candidate is worse than both baseline and the current one-sided `0.02` anchor on the metrics that matter for promotion
- working rule update
  - keep broad fixed compare gate closed
  - keep `hidden_dropout20 + happy_to_calm_penalty_weight 0.02` as the best current narrow15 anchor
  - if disk space is recovered, the next balanced checks may continue conservatively to:
    - `happy_calm_bidirectional_penalty_weight = 0.005`
    - `happy_calm_bidirectional_penalty_weight = 0.0075`
  - if those also show one-direction collapse, stop this bidirectional family and redesign the loss or sampling strategy instead of widening promotion discussion
