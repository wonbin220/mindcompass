# KcELECTRA 감정분류 학습 가이드 문서

이 문서는 Mind Compass에서 `beomi/KcELECTRA-base`를 사용해 감정분류 MVP를 학습하고, FastAPI 내부 추론 API까지 연결하는 과정을 정리한 학습 문서다.

---

## 2026-03-30 HAPPY/CALM boundary-control session memo

- fixed compare 1차 gate는 계속 유지한다.
- baseline 교체는 금지한다: `cpu_compare_medium_relabel_weighted`
- serving policy는 그대로 유지한다: `TIRED fallback-only`
- broad text rule, emotion_major 기반 광범위 relabel, dropout-only 실험, learning-rate-only 실험은 다음 후보에서 제외한다.

### one-sided penalty family stop decision

- one-sided `happy_to_calm_penalty_weight` 축은 이번 세션 기준으로 공식 종료 판단을 내린다.
- 종료 anchor는 두 점으로 고정한다.
  - `0.02`
    - one-sided family 안에서 best local balance reference
    - `macro_f1 = 0.3906`
    - `happy_calm_macro_f1 = 0.5070`
    - `HAPPY -> CALM = 22/150`
    - `CALM -> HAPPY = 105/150`
  - `0.005`
    - overall strongest result지만 promotion 불가
    - `macro_f1 = 0.4916`
    - `happy_calm_macro_f1 = 0.5360`
    - `HAPPY -> CALM = 24/150`
    - `CALM -> HAPPY = 99/150`
- 해석:
  - `0.005`는 overall quality를 크게 끌어올렸지만 baseline의 protected-side 경계를 지키지 못했다.
  - `0.019`, `0.018`, `0.01`은 HAPPY 보호를 밀어붙이는 대신 `CALM -> HAPPY` 역붕괴를 반복했다.
  - 따라서 one-sided penalty 축은 더 내려가도, 더 올라가도 promotion gate를 통과할 가능성이 낮다.

### next single-knob choice

- 다음 boundary-control 아이디어는 `happy_calm_bidirectional_penalty_weight` 1개로 고정한다.
- 이유:
  - one-sided penalty의 핵심 실패는 한 방향만 줄일수록 반대 방향이 무너지는 구조였기 때문이다.
  - single-knob 제약을 유지하면서도 `HAPPY -> CALM`, `CALM -> HAPPY`를 동시에 제어할 수 있다.

### first validation result

- 1차 검증 후보는 가장 보수적으로 `happy_calm_bidirectional_penalty_weight = 0.0025`를 사용했다.
- fixed compare validation 결과:
  - `macro_f1 = 0.2167`
  - `happy_calm_macro_f1 = 0.3349`
  - `HAPPY -> CALM = 142/150`
  - `CALM -> HAPPY = 0/150`
- 판단:
  - `CALM -> HAPPY`는 사라졌지만 `HAPPY -> CALM`가 대규모 붕괴했다.
  - 즉 이 family도 현재 scale에서는 즉시 promotable 후보가 아니다.
  - 같은 family를 이어가더라도 `0.005`, `0.0075`처럼 더 큰 값으로 갈 이유는 약해졌고, 재시도한다면 `0.0025`보다 더 작은 구간만 검토하는 것이 맞다.

## 2026-03-28 serving policy update

- `ai-api-fastapi` 서빙에서는 현재 `TIRED`를 learned primary emotion으로 그대로 내보내지 않습니다.
- 모델이 `TIRED`를 예측하면 `CALM` fallback으로 보수 처리합니다.
- 응답에는 `fallbackUsed=true`, `fallbackReason="TIRED_FALLBACK_ONLY"`가 포함됩니다.
- 이유:
  - 현재까지의 TIRED 관련 비교 실험 모두 `TIRED F1 = 0.0`
  - 즉 TIRED는 아직 학습적으로 신뢰 가능한 클래스가 아닙니다.
- 현재 우선순위:
  - blind TIRED expansion 반복보다
  - 안정적인 서빙과 다른 감정 클래스 품질 개선이 먼저입니다.

# 1. Goal

- 감정분류 모델 학습 방향을 고정한다.
- 필요한 패키지와 실행 순서를 정리한다.
- AI Hub 감성대화 원본으로 `train/valid CSV`를 만드는 방법을 남긴다.
- 학습된 모델을 `ai-api-fastapi`에서 내부 API로 서빙하는 흐름을 연결한다.
- 사용자가 자리를 비워도 돌릴 수 있도록 무인 실행 스크립트와 로그 구조를 남긴다.

---

# 2. Why this model

`beomi/KcELECTRA-base`를 선택한 이유:

- 한국어 일상 문장 분류에 강하다.
- diary/chat처럼 짧고 감정이 섞인 문장에 잘 맞는다.
- Hugging Face 생태계에 바로 연결돼 학습과 서빙을 단순하게 가져갈 수 있다.

---

# 3. Related files

- `ai-api-fastapi/training/emotion_classifier/README.md`
- `ai-api-fastapi/training/emotion_classifier/configs/label_map.json`
- `ai-api-fastapi/training/emotion_classifier/configs/training_config.json`
- `ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json`
- `ai-api-fastapi/training/emotion_classifier/scripts/prepare_emotion_dataset.py`
- `ai-api-fastapi/training/emotion_classifier/scripts/train_emotion_classifier.py`
- `ai-api-fastapi/training/emotion_classifier/scripts/evaluate_emotion_classifier.py`
- `ai-api-fastapi/training/emotion_classifier/scripts/infer_emotion_classifier.py`
- `ai-api-fastapi/training/emotion_classifier/scripts/run_kcelectra_training.ps1`
- `ai-api-fastapi/app/routers/model_router.py`
- `ai-api-fastapi/app/services/emotion_classifier_service.py`
- `ai-api-fastapi/app/inference/predictor.py`
- `ai-api-fastapi/app/inference/label_mapper.py`

---

# 4. 설치가 필요한가

필요하다.

이유:

- 데이터 가공에는 `pandas`가 필요하다.
- 실제 학습에는 `torch`, `transformers`, `scikit-learn`, `numpy`, `safetensors`, `accelerate`가 필요하다.
- 학습된 모델을 FastAPI에서 로드할 때도 `torch`와 `transformers`가 필요하다.

현재 설치 기준:

- `torch 2.1.2+cpu`
- `transformers 4.38.2`
- `pandas 2.0.3`
- `scikit-learn 1.3.2`
- `accelerate 0.28.0`

설치 명령:

```powershell
.\ai-api-fastapi\.venv\Scripts\python.exe -m pip install `
  pandas==2.0.3 `
  scikit-learn==1.3.2 `
  numpy==1.24.4 `
  transformers==4.38.2 `
  torch==2.1.2 `
  safetensors==0.4.2 `
  accelerate==0.28.0
```

Hugging Face 캐시 권장 설정:

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
New-Item -ItemType Directory -Force -Path $env:HF_HOME
```

---

# 5. 실제 데이터 경로 기준 CSV 생성

이 단계는 아래 3개 진행사항 중 `1) AI Hub 감성대화 원본으로 train/valid CSV 생성`에 해당한다.

## 5-1. train CSV 생성

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\prepare_emotion_dataset.py `
  --xlsx "C:\Users\wonbin\OneDrive\바탕 화면\mindcompass project\018.감성대화\Training_221115_add\원천데이터\감성대화말뭉치(최종데이터)_Training.xlsx" `
  --json "C:\Users\wonbin\OneDrive\바탕 화면\mindcompass project\018.감성대화\Training_221115_add\라벨링데이터\감성대화말뭉치(최종데이터)_Training.json" `
  --split train `
  --output "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\train_emotion_mvp.csv"
```

## 5-2. valid CSV 생성

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\prepare_emotion_dataset.py `
  --xlsx "C:\Users\wonbin\OneDrive\바탕 화면\mindcompass project\018.감성대화\Validation_221115_add\원천데이터\감성대화말뭉치(최종데이터)_Validation.xlsx" `
  --json "C:\Users\wonbin\OneDrive\바탕 화면\mindcompass project\018.감성대화\Validation_221115_add\라벨링데이터\감성대화말뭉치(최종데이터)_Validation.json" `
  --split valid `
  --output "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\valid_emotion_mvp.csv"
```

현재 생성 결과:

- `train_emotion_mvp.csv`: `51,630`행
- `valid_emotion_mvp.csv`: `6,641`행

---

# 6. 1차 KcELECTRA 학습 실행

이 단계는 `2) 1차 KcELECTRA 학습 실행`에 해당한다.

기본 설정으로 직접 실행:

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\train_emotion_classifier.py `
  --train-csv "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\train_emotion_mvp.csv" `
  --valid-csv "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\valid_emotion_mvp.csv" `
  --config "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config.json" `
  --label-map "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\label_map.json" `
  --output-dir "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts"
```

학습 후 확인 포인트:

- epoch 단위 로그가 기록되는지
- `eval_macro_f1`가 저장되는지
- `artifacts/best` 아래에 모델과 tokenizer가 저장되는지

---

# 7. CPU 기준 현실적인 학습 설정

현재 PC처럼 CPU 환경에서 `training_config.json`을 그대로 사용하면 학습 시간이 너무 길다.
실제로 현재 런도 CPU 기준으로는 매우 오래 걸릴 가능성이 높다.

그래서 다음 실행부터는 별도 CPU 설정 파일을 쓰는 편이 현실적이다.

파일:

- `ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json`

추천 값:

- `max_length = 96`
- `num_train_epochs = 2`
- `train_batch_size = 4`
- `eval_batch_size = 4`
- `gradient_accumulation_steps = 4`
- `warmup_ratio = 0.05`
- `logging_steps = 100`
- `save_total_limit = 1`
- `dataloader_num_workers = 0`
- `early_stopping_patience = 1`

이 설정을 추천하는 이유:

- `max_length`를 줄이면 토큰 계산량이 줄어 CPU 시간이 많이 줄어든다.
- `batch_size`를 낮춰 메모리 부담과 CPU 병목을 줄인다.
- `gradient_accumulation_steps`로 작은 배치의 불안정성을 조금 보완한다.
- `early_stopping`을 넣어 성능이 더 좋아지지 않는 epoch를 빨리 끊는다.

CPU 설정으로 실행:

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\train_emotion_classifier.py `
  --train-csv "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\train_emotion_mvp.csv" `
  --valid-csv "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\valid_emotion_mvp.csv" `
  --config "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config_cpu.json" `
  --label-map "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\label_map.json" `
  --output-dir "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts"
```

---

# 8. 평가와 단건 추론

## 8-1. 평가

```powershell
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\evaluate_emotion_classifier.py `
  --model-dir "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\best" `
  --input-csv "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\processed\valid_emotion_mvp.csv" `
  --output-json "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation\valid_metrics.json"
```

## 8-2. 단건 추론

```powershell
.\ai-api-fastapi\.venv\Scripts\python.exe `
  ai-api-fastapi\training\emotion_classifier\scripts\infer_emotion_classifier.py `
  --model-dir "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\best" `
  --text "요즘 회사 일 때문에 너무 지치고 불안해요."
```

---

# 9. FastAPI 실호출 검증

이 단계는 `3) /internal/model/emotion-classify와 /internal/ai/analyze-diary 실호출 검증`에 해당한다.

먼저 서버 실행:

```powershell
$env:HF_HOME="C:\programing\mindcompass\ai-api-fastapi\.cache\huggingface"
.\ai-api-fastapi\.venv\Scripts\uvicorn.exe app.main:app --host 127.0.0.1 --port 8002
```

## 9-1. emotion-classify 호출

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8002/internal/model/emotion-classify" `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"text":"요즘 회사 일 때문에 너무 지치고 불안해요.","returnTopK":3}'
```

## 9-2. analyze-diary 호출

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8002/internal/ai/analyze-diary" `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"userId":1,"diaryId":101,"content":"오늘은 일이 많아서 너무 지치고 불안했어.","writtenAt":"2026-03-27T22:00:00"}'
```

확인 포인트:

- `primaryEmotion`이 서비스 라벨 체계로 내려오는지
- `confidence`가 채워지는지
- 모델 로드나 추론 실패 시 fallback 응답으로 내려가는지

---

# 10. 무인 실행 구조

무인 실행 스크립트:

- `ai-api-fastapi/training/emotion_classifier/scripts/run_kcelectra_training.ps1`

자동 실행 순서:

1. `prepareTrain`
2. `prepareValid`
3. `train`
4. `evaluate`
5. `verifyApi`

로그 구조:

- `logs/<timestamp>/prepare-train.out.log`
- `logs/<timestamp>/prepare-train.err.log`
- `logs/<timestamp>/prepare-valid.out.log`
- `logs/<timestamp>/prepare-valid.err.log`
- `logs/<timestamp>/train.out.log`
- `logs/<timestamp>/train.err.log`
- `logs/<timestamp>/evaluate.out.log`
- `logs/<timestamp>/evaluate.err.log`
- `logs/<timestamp>/status.json`
- `logs/<timestamp>/summary.txt`
- `logs/<timestamp>/verify-api.json`

현재 기본 동작:

- `run_kcelectra_training.ps1`는 별도 옵션이 없으면 `training_config_cpu.json`을 사용한다.
- 기존 무거운 설정을 쓰고 싶으면 `-ConfigPath`로 `training_config.json`을 넘기면 된다.

예시:

```powershell
.\ai-api-fastapi\training\emotion_classifier\scripts\run_kcelectra_training.ps1
```

기존 설정 강제:

```powershell
.\ai-api-fastapi\training\emotion_classifier\scripts\run_kcelectra_training.ps1 `
  -ConfigPath "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config.json"
```

---

# 11. 지금 진행사항과의 연결

현재 작업은 아래 3개 진행사항과 직접 연결된다.

1. `AI Hub 감성대화 원본으로 train/valid CSV 생성`
2. `1차 KcELECTRA 학습 실행`
3. `/internal/model/emotion-classify`와 `/internal/ai/analyze-diary` 실호출 검증

즉 데이터 준비, 학습, 내부 서빙 검증까지 한 흐름으로 이어지는 문서다.

---

# 12. 라벨 매핑과 분포 보정 메모

2026-03-27 추가 보정 기준:

- `prepare_emotion_dataset.py`에서 exact match만 보던 방식 때문에 실제 원본의 `emotion_minor` 표현이 많이 누락됐다.
- 특히 `상처`, `당황` 대분류가 기본값 `CALM`으로 떨어져 분포가 왜곡됐다.
- 그래서 다음 보정을 넣었다.

1. exact match 라벨 추가
2. keyword 기반 fallback 추가
3. major fallback을 `상처 -> SAD`, `당황 -> ANXIOUS`로 수정

보정 후 확인 포인트:

- `CALM` 쏠림이 줄어드는지
- `SAD`, `ANXIOUS`, `ANGRY`가 실제 감정 의미에 더 맞게 재분배되는지
- smoke 학습의 macro F1이 이전보다 조금이라도 개선되는지

현재 남은 한계:

- 원본 처리 결과에는 아직 `TIRED` 샘플이 없다.
- 즉 `label_map.json`은 6라벨 구조를 유지하지만, 실제 학습 데이터는 사실상 5라벨에 가깝다.
- 이 점은 full run 전 반드시 알고 있어야 한다.
