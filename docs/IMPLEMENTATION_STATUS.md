# Mind Compass 구현 상태

이 문서는 새 세션에서도 현재 구현 상태를 빠르게 이어가기 위한 진행 기록 문서다.
문서는 UTF-8 기준으로 유지한다.

## 현재 기준

- 기준 날짜: 2026-03-28
- 현재 단계: MVP 핵심 백엔드와 내부 AI 계층 뼈대가 잡혀 있고, 반응형 웹 기준 아키텍처 문서화와 데이터셋 정리가 함께 진행 중이다.
- 전체 구조:
  - `backend-api` = Spring Boot 공개 API
  - `ai-api` = Spring AI 기반 내부 AI 오케스트레이션 서버
  - `ai-api-fastapi` = FastAPI 기반 감정분류 모델 서빙 계층
- 핵심 원칙:
  - 반응형 웹은 `backend-api`만 호출한다.
  - `backend-api`는 `ai-api`만 내부적으로 호출한다.
  - `ai-api`가 필요 시 `ai-api-fastapi`를 호출한다.
  - AI 실패가 diary/chat 전체 실패로 번지지 않게 fallback을 유지한다.

## 현재 완료 범위

### 1. backend-api

- Auth
  - `POST /api/v1/auth/signup`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- User
  - `GET /api/v1/users/me`
- Diary
  - `POST /api/v1/diaries`
  - `GET /api/v1/diaries/{diaryId}`
  - `PATCH /api/v1/diaries/{diaryId}`
  - `DELETE /api/v1/diaries/{diaryId}`
  - `GET /api/v1/diaries?date=YYYY-MM-DD`
- Calendar
  - `GET /api/v1/calendar/monthly-emotions?year=YYYY&month=MM`
  - `GET /api/v1/calendar/daily-summary?date=YYYY-MM-DD`
- Chat
  - `POST /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions/{sessionId}`
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
- Report
  - `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`

### 2. ai-api

- 현재 내부 비교 엔드포인트
  - `GET /health`
  - `POST /internal/ai/analyze-diary`
  - `POST /internal/ai/risk-score`
  - `POST /internal/ai/generate-reply`
- 구조 특징
  - Controller + DTO + Service + PromptClient 구조 유지
  - Spring AI 프롬프트 호출 실패 시 규칙 기반 fallback 유지
  - health 응답으로 런타임과 서비스 상태 확인 가능

### 3. ai-api-fastapi

- 현재 노출된 엔드포인트
  - `GET /health`
  - `POST /internal/ai/analyze-diary`
  - `POST /internal/ai/risk-score`
  - `POST /internal/ai/generate-reply`
- 목표 역할
  - 감정분류 모델 서빙
  - 모델 버전 / threshold / calibration 관리
  - inference metadata 반환
- 주의 메모
  - 현재는 과거 비교용 엔드포인트 형태가 남아 있지만, 문서 기준 역할 설명은 감정분류 모델 서빙 계층으로 통일한다.

### 4. AI 흐름 상태

- Diary 흐름
  - `backend-api`가 diary 저장 후 `analyze-diary`와 필요 시 `risk-score`를 호출하는 구조 정리
  - ai-api 실패 시 diary 저장 자체는 유지
- Chat 흐름
  - `risk-score`를 먼저 호출해 `NORMAL`, `SUPPORTIVE`, `SAFETY`, `FALLBACK` 분기를 지원
  - 일반 답변 생성은 `generate-reply` 담당
  - ai-api 실패 시에도 assistant fallback 응답으로 대화 흐름을 끊지 않음

## 현재 검증 상태

### backend-api 테스트 상태

- Service 테스트
  - `AuthServiceTest`
  - `UserServiceTest`
  - `DiaryServiceTest`
  - `ChatServiceTest`
  - `ReportServiceTest`
  - `CalendarServiceTest`
- Controller 테스트
  - `AuthControllerTest`
  - `UserControllerTest`
  - `DiaryControllerTest`
  - `ChatControllerTest`
  - `ReportControllerTest`
  - `CalendarControllerTest`
- 보안/권한 테스트
  - `SecurityIntegrationTest`
  - `OwnershipIntegrationTest`
- 공개 API E2E 테스트
  - `PublicApiAuthE2ETest`
  - `PublicApiUserE2ETest`
  - `PublicApiReportMonthlySummaryE2ETest`
  - `PublicApiReportWeeklyEmotionsE2ETest`
  - `PublicApiReportMonthlyRisksE2ETest`
  - `PublicApiChatE2ETest`
  - `PublicApiDiaryFallbackE2ETest`
  - `PublicApiChatFallbackE2ETest`

### 현재까지 확인된 동작

- Diary create
  - AI 분석 실패 시에도 `201 Created` 유지
  - 감정 분석/위험 점수 fallback 동작 확인
- Chat send-message
  - risk-score 선판단 후 `SAFETY`, `SUPPORTIVE`, `NORMAL`, `FALLBACK` 분기 확인
  - AI 실패 시에도 assistant 응답 저장 유지
- Calendar / Report
  - 월간/주간 조회 계약 검증
  - controller + E2E 경로 분리 후 회귀 확인

### Swagger / 실호출 메모

- `ai-api` health 응답 확인 경로 존재
- `backend-api` Swagger/UI 진입 가능 상태 기록 존재
- `ai-api` live runtime 검증 기록 존재

## 현재 리스크 / 주의사항

- 환경 변수 의존성이 큼
  - `JWT_SECRET`
  - `DB_PASSWORD`
  - `AI_API_BASE_URL`
  - `WEB_ALLOWED_ORIGINS`
  - `OPENAI_API_KEY`
- PowerShell 실행 환경, IntelliJ Run Configuration, 실제 배포 환경의 변수 차이를 항상 확인해야 한다.
- `generate-reply` 학습 문서의 일부 예시 필드는 현재 Spring AI DTO 구현과 차이가 있다.
  - 예: `riskLevel`, `usedEvidence`
- ai-api logical ERD는 physical DB 확정안이 아니라 운영/확장 기준의 논리 모델이다.
- 슬라이드 검수용 몽타주 스크립트는 현재 Python 3.8 환경과 타입 힌트 호환성 문제가 있어 개별 PNG 렌더 기준으로 확인했다.

## 최근 핵심 메모

### 2026-03-24 diary create API hardening

- Completed API
  - `POST /api/v1/diaries`
- Verification status
  - `analyze-diary` fallback 확인
  - `risk-score` fallback 확인
  - AI 실패 시에도 diary 저장 유지

### 2026-03-24 cross-domain hardening

- Completed API
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/calendar/monthly-emotions`
  - `GET /api/v1/calendar/daily-summary`
- Verification status
  - chat safety/supportive fallback 확인
  - calendar 조회 계약 확인

### 2026-03-24 controller and report regression expansion

- Completed API
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/reports/monthly-summary`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly`
- Verification status
  - report 조회 controller/E2E 회귀 확인

### 2026-03-24 spring ai live runtime verification

- Completed change
  - `ai-api`를 `java -jar`로 실행
  - `backend-api`와 실제 연동 확인
- Verification status
  - `/health` 응답 확인
  - diary create 실호출 시 AI 필드 반영 확인

### 2026-03-24 spring ai backend integration strategy

- Completed change
  - provider, base-url 분리
  - Spring AI / FastAPI 전환 전략 문서 정리
- Verification status
  - properties 테스트로 provider/base URL 선택 확인

### 2026-03-27 CSV 번역 작업 메모

- Completed work
  - `emotion-dataset` 전체 한국어 번역 완료 상태 재확인
  - `scripts/translate_mental_health_csv.py` 최신 기능 재확인
    - 배치 번역
    - 재시도
    - 출력 CSV 기준 resume
    - 해요체 후처리 강화
    - 배치 실패 시 단건 fallback
  - `amod-mental-health-counseling-conversations-data/train_ko.csv` 재개 실행
- Verification status
  - `train.csv` 총 `3512`행 확인
  - 실질 연속 완료 `3512/3512`
  - 마지막 미완료처럼 보이던 5행 중 4행은 원본 응답 공백, 1행은 손상된 Word/MSO 마크업 조각으로 확인
- Script improvements
  - `scripts/translate_mental_health_csv.py`에 `--resume-mode missing` 옵션 추가
  - 원본 텍스트가 비어 있거나 손상된 마크업이면 정상 완료로 판단하도록 보정

### 2026-03-28 human-and-llm 샘플 복구 업데이트

- Completed work
  - `human-and-llm-mental-health-conversations/dataset_ko_sample.csv` 복구 완료
  - 샘플 전용 입력 `dataset_sample_80_input.csv`를 만들어 `missing` 모드로 누락 5행만 재처리
- Verification status
  - `Context_ko=80`
  - `Response_ko=80`
  - `LLM_ko=80`
  - 연속 완료 `80/80`
- Caution
  - 샘플 출력 길이와 전체 원본 길이가 다를 때는 샘플 전용 입력 파일로 resume 기준을 맞추는 편이 안전

### 2026-03-28 ai-api 문서/아키텍처 산출물 업데이트

- Completed change
  - `docs/ai-api/INTERNAL_API_SPEC_DRAFT.md` 추가
  - `docs/ai-api/AI_API_LOGICAL_ERD.md` 추가
  - `docs/slides/ai-api-system-architecture/build_ai_api_system_architecture.js` 추가
  - `docs/slides/ai-api-system-architecture/ai-api-system-architecture.pptx` 생성
- Verification status
  - `ai-api` 현재 코드 기준 컨트롤러/DTO/서비스 흐름을 읽어 명세 초안 반영
  - logical ERD는 현재 구현 + 가까운 확장 범위를 분리해서 정리
  - PowerPoint COM으로 슬라이드 PNG 6장 렌더 확인

### 2026-03-28 웹 기준 AI 계층 구조 문서/슬라이드 재정리

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js` 재작성
  - `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx` 생성
  - `docs/ai-api/README.md` 구조 설명 재작성
  - `docs/ai-api/AI_API_OVERVIEW_LEARNING.md` 재작성
  - `docs/ai-api/INTERNAL_API_SPEC_DRAFT.md`에 오케스트레이터/모델서빙 경계 반영
- Design decision
  - 감정캠퍼스는 모바일 앱이 아니라 반응형 웹 기준으로 설명
  - `ai-api-fastapi`를 단순 비교 서버가 아니라 감정분류 모델 서빙 계층으로 정리
  - 권장 호출 흐름을 `Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`로 고정
  - `backend-api`가 `ai-api-fastapi`를 직접 호출하지 않는 원칙 명시
- Verification status
  - PowerPoint PNG 렌더로 `rendered-reference-v2/슬라이드1.PNG` 확인

### 2026-03-29 responsive web and model-serving doc alignment

- Completed change
  - `AGENTS.md` 아키텍처 설명을 최신 기준으로 정리
  - `docs/MOBILE_INTEGRATION_HANDOFF.md`를 반응형 웹 연동 기준으로 업데이트
  - `docs/SCREEN_TO_API_MAPPING.md`를 웹 클라이언트 기준으로 업데이트
  - `docs/README.md`에 웹 클라이언트와 내부 AI 계층 원칙 반영
- Design decision
  - 외부 클라이언트는 모바일 앱이 아니라 반응형 웹 기준으로 설명한다.
  - `ai-api-fastapi`는 비교용 서버가 아니라 감정분류 모델 서빙 계층으로 통일한다.
  - 권장 호출 흐름은 `Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`로 유지한다.
- Verification status
  - `AGENTS.md`의 `mobile app`, `comparison AI inference server` 표현 제거 확인
  - 웹 연동 문서의 `모바일 앱` 표현을 반응형 웹 기준으로 교체 확인
  - `docs/README.md`에 웹 클라이언트 호출 원칙 반영 확인

### 2026-03-28 웹 기준 AI 계층 문서 일관성 및 슬라이드 확장

- Completed change
  - `README.md`의 공개 구조 설명을 반응형 웹 기준으로 재정리
  - `docs/IMPLEMENTATION_STATUS.md`의 구조/원칙 표현을 `backend-api -> ai-api -> ai-api-fastapi` 기준으로 정리
  - `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx`와 `rendered-reference-v2/슬라이드1.PNG`를 기준으로 시각 검수
  - PowerPoint 호환성 기준으로 이번 차수의 구조 변경과 추가 슬라이드 확장은 보류
- Design decision
  - `ai-api-fastapi`는 단순 비교 서버가 아니라 감정분류 모델 서빙 계층으로 계속 설명
  - `backend-api`는 `ai-api-fastapi`를 직접 호출하지 않고, AI 내부 진입점은 `ai-api`로 고정
  - 현재 1장 구조도는 흰색 기획서 톤과 정보 밀도 측면에서 유지 가능하다고 판단하고, 추가 확장은 별도 호환성 점검 후 진행
- Verification status
  - 검수용 복사본 렌더 기준으로 `슬라이드1.PNG` 재확인 예정

### 2026-03-28 슬라이드 아이콘 실아이콘 교체 보강

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js`에서 점선 placeholder를 실제 아이콘 이미지 로딩 방식으로 교체
  - `docs/slides/ai-api-system-architecture/assets/icons/`에 브라우저, Spring, Spring AI, PyTorch/FastAPI, PostgreSQL(pgvector 대체), OpenAI 아이콘 자산 추가
  - 잠금 중인 원본 PPTX 대신 검수용 산출물 `ai-api-system-architecture-reference-style-v2-icons-review.pptx` 생성
- Design decision
  - `pgvector`는 독립 아이콘보다 PostgreSQL 생태계 인지가 더 쉬워 `postgresql.svg`를 대체 아이콘으로 사용
  - `Spring AI`, `PyTorch/FastAPI`는 단일 브랜드 아이콘보다 조합 의미가 중요해 로컬 조합 SVG로 구성
  - 원본 `ai-api-system-architecture-reference-style-v2.pptx`가 PowerPoint에서 열려 있어 덮어쓰기는 보류하고, 스크립트 기준을 먼저 갱신
- Verification status
  - PowerPoint COM 렌더로 `docs/slides/ai-api-system-architecture/rendered-reference-v2-icons-review/슬라이드1.PNG` 확인

### 2026-03-28 KcELECTRA active 5-label head 실험

- Completed change
  - `ai-api-fastapi/training/emotion_classifier/scripts/train_emotion_classifier.py`
    - train/valid CSV의 실제 `service_label` 집합으로 활성 라벨을 계산하도록 수정
    - 활성 라벨 수만큼 classification head를 생성하도록 수정
    - 학습 산출물에 `best/label_metadata.json` 저장 추가
  - `ai-api-fastapi/training/emotion_classifier/scripts/evaluate_emotion_classifier.py`
    - 저장된 모델 `config.id2label` 순서를 기준으로 평가 라벨 순서를 고정
    - 평가 JSON에 `evaluated_labels` 추가
- Verification status
  - 문법 확인: `python -m py_compile ...train_emotion_classifier.py ...evaluate_emotion_classifier.py`
  - 비교 학습 런:
    - 입력 train CSV: `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_cpu_compare_medium.csv`
    - 입력 valid CSV: `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_cpu_compare_medium.csv`
    - 출력 아티팩트: `ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5`
    - 평가 JSON: `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted_active5.json`
  - 확인된 활성 라벨:
    - `HAPPY`, `CALM`, `ANXIOUS`, `SAD`, `ANGRY`
    - `num_labels = 5`
- Result summary
  - 기존 medium baseline
    - accuracy `0.4267`
    - macro F1 `0.3645`
    - `SAD F1 = 0.0000`
    - `ANGRY F1 = 0.4682`
  - active5 medium 비교 런
    - accuracy `0.3987`
    - macro F1 `0.3483`
    - `SAD F1 = 0.4376`
    - `ANGRY F1 = 0.0000`
- Error sample artifact
  - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_error_samples_medium_active5_sad_angry.csv`
  - 주요 오분류 집계
    - `ANGRY -> SAD`: `89`
    - `ANGRY -> ANXIOUS`: `57`
    - `SAD -> ANXIOUS`: `47`
- Interpretation
  - `TIRED` 빈 클래스 때문에 6-label head를 유지하던 구조는 제거했다.
  - 하지만 active 5-label head만으로 최고 baseline이 갱신되지는 않았다.
  - 이번 결과는 `SAD` 붕괴를 줄이는 대신 `ANGRY`가 무너져, 현재 병목이 단순 헤드 차원 수 문제가 아니라 라벨 경계와 데이터 혼선에도 있음을 보여준다.

### 2026-03-28 SAD-ANGRY consistency relabel v1 실험

- Completed change
  - `ai-api-fastapi/training/emotion_classifier/scripts/create_emotion_consistency_relabel.py` 추가
  - 비교용 재라벨링 규칙
    - `ANGRY + emotion_major in {상처, 슬픔} -> SAD`
    - `ANGRY + emotion_major == 당황 -> ANXIOUS`
    - `SAD + emotion_major == 분노 -> ANGRY`
    - `SAD + emotion_major == 당황 -> ANXIOUS`
  - 생성 산출물
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_consistency_relabel_v1.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_consistency_relabel_v1.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_consistency_relabel_v1_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_consistency_relabel_v1_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_consistency_relabel_v1.json`
- Distribution change
  - train
    - before: `SAD 19886`, `ANXIOUS 14524`, `ANGRY 11094`, `HAPPY 4281`, `CALM 1845`
    - after: `SAD 18268`, `ANXIOUS 18949`, `ANGRY 8287`, `HAPPY 4281`, `CALM 1845`
  - valid
    - before: `SAD 2190`, `ANXIOUS 1798`, `ANGRY 1440`, `HAPPY 752`, `CALM 461`
    - after: `SAD 2010`, `ANXIOUS 2290`, `ANGRY 1128`, `HAPPY 752`, `CALM 461`
- Verification status
  - 변경 건수
    - train `8064`
    - valid `944`
  - medium compare result
    - accuracy `0.3640`
    - macro F1 `0.2174`
    - `ANGRY F1 = 0.4606`
    - `HAPPY F1 = 0.6135`
    - `CALM F1 = 0.0132`
    - `ANXIOUS F1 = 0.0000`
    - `SAD F1 = 0.0000`
- Interpretation
  - `emotion_major` 기준 재라벨링을 너무 강하게 적용해 `ANXIOUS`로 대량 이동시키면서 클래스 경계가 과하게 무너졌다.
  - 이 실험은 `ANGRY` 쪽만 일부 회복시키고 전체 품질은 크게 악화되어 채택 대상이 아니다.

### 2026-03-28 SAD-ANGRY consistency relabel v2 실험

- Completed change
  - `ai-api-fastapi/training/emotion_classifier/scripts/create_emotion_consistency_relabel_v2.py` 추가
  - 보수적 재라벨링 규칙
    - `SAD + emotion_minor in {충격 받은, 당황} -> ANXIOUS`
    - `ANGRY + emotion_minor == 한심한 -> SAD`
    - `ANGRY + emotion_minor == 억울한` 이고 본문에 슬픔 힌트가 있으면 `SAD`
    - `ANGRY + health hint + anxious hint` 일부만 `ANXIOUS`
  - 생성 산출물
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_consistency_relabel_v2.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_consistency_relabel_v2.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_consistency_relabel_v2_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_consistency_relabel_v2_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_consistency_relabel_v2.json`
- Distribution change
  - train
    - before: `SAD 19886`, `ANXIOUS 14524`, `ANGRY 11094`, `HAPPY 4281`, `CALM 1845`
    - after: `SAD 20031`, `ANXIOUS 15628`, `ANGRY 9845`, `HAPPY 4281`, `CALM 1845`
  - valid
    - before: `SAD 2190`, `ANXIOUS 1798`, `ANGRY 1440`, `HAPPY 752`, `CALM 461`
    - after: `SAD 2202`, `ANXIOUS 1945`, `ANGRY 1281`, `HAPPY 752`, `CALM 461`
- Verification status
  - 변경 건수
    - train `2159`
    - valid `290`
  - reason counts
    - train
      - `ANGRY_TO_SAD_MINOR_V2 = 918`
      - `SAD_TO_ANXIOUS_MINOR_V2 = 910`
      - `ANGRY_TO_SAD_TEXT_V2 = 137`
      - `ANGRY_TO_ANXIOUS_TEXT_V2 = 92`
      - `ANGRY_TO_ANXIOUS_HEALTH_V2 = 102`
    - valid
      - `ANGRY_TO_SAD_MINOR_V2 = 126`
      - `SAD_TO_ANXIOUS_MINOR_V2 = 131`
      - `ANGRY_TO_SAD_TEXT_V2 = 17`
      - `ANGRY_TO_ANXIOUS_TEXT_V2 = 7`
      - `ANGRY_TO_ANXIOUS_HEALTH_V2 = 9`
  - medium compare result
    - accuracy `0.3747`
    - macro F1 `0.2575`
    - `HAPPY F1 = 0.5720`
    - `CALM F1 = 0.1250`
    - `ANXIOUS F1 = 0.1124`
    - `SAD F1 = 0.0000`
    - `ANGRY F1 = 0.4783`
- Interpretation
  - `v1`보다 보수적으로 줄였지만 여전히 baseline보다 크게 낮다.
  - `ANGRY`는 일부 회복했지만 `SAD`가 다시 0으로 무너져, 현재 규칙 기반 재라벨링만으로는 두 클래스를 동시에 살리지 못했다.
  - 현재 채택 기준은 여전히 기존 best baseline `valid_metrics_cpu_compare_medium_relabel_weighted.json`이다.

## 지금 바로 다음 할 일

1. `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_error_samples_medium_active5_sad_angry.csv`를 기준으로 `emotion_minor`별 20건 내외 수동 seed set을 따로 뽑아, 규칙이 아니라 소규모 gold relabel CSV를 만들기
2. 다음 실험은 `한심한`, `충격 받은`처럼 현재 효과가 확인된 소분류 몇 개만 수동 승인 후 반영하고, broad text rule은 더 넣지 않기
3. serving 기본 방향은 계속 `TIRED fallback-only`로 유지하고, 학습 비교 기준선은 `valid_metrics_cpu_compare_medium_relabel_weighted.json`으로 유지하기
### 2026-03-28 슬라이드 원본 PPTX 재생성 확인

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js` 기준으로 원본 `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx` 재생성
  - 원본 렌더 검수 경로 `docs/slides/ai-api-system-architecture/rendered-reference-v2-original/슬라이드1.PNG` 생성
- Design decision
  - 검수 기준을 임시 복사본이 아니라 원본 PPTX 재생성 결과로 다시 맞춤
  - `ai-api-fastapi`는 계속 감정분류 모델 서빙 계층으로 설명하고, 원본 슬라이드도 같은 용어 기준을 유지
- Verification status
  - `node build_ai_api_reference_style.js` 실행으로 원본 PPTX 덮어쓰기 성공
  - PowerPoint COM 렌더로 원본 기준 `슬라이드1.PNG` 생성 및 시각 확인 완료
  - 브라우저, Spring, Spring AI, PyTorch/FastAPI, PostgreSQL, OpenAI 아이콘이 placeholder 대신 실제 아이콘으로 반영된 상태 확인
- Caution
  - `slides_test.py` 기반 추가 overflow 검사는 현재 로컬 Python 3.8 환경에 `numpy`가 없어 실행하지 못함
  - 생성 스크립트 자체에서는 기존 레이아웃 overlap 경고가 남아 있으므로, 다음 차수에서 필요 시 경고 원인만 별도로 정리 가능
### 2026-03-28 슬라이드 우측 Phase 설명형 정리

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js`에서 우측 설명 영역을 카드형 bullet에서 `Phase 1~4` 설명형 레이아웃으로 재구성
  - 원본 `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx` 재생성
  - 원본 렌더 결과 `docs/slides/ai-api-system-architecture/rendered-reference-v2-original/슬라이드1.PNG` 갱신
- Design decision
  - 좌측 시스템 다이어그램은 기존 PowerPoint 호환성이 확인된 구조를 유지하고, 우측 설명만 예시 이미지처럼 문단형으로 정리
  - `backend-api -> ai-api -> ai-api-fastapi` 경계와 반응형 웹 기준 설명은 그대로 유지
- Verification status
  - `node build_ai_api_reference_style.js` 실행 성공
  - PowerPoint COM 렌더로 원본 PNG 재생성 및 시각 확인 완료
- Caution
  - 우측 설명 영역은 더 줄이면 더 미니멀하게 만들 수 있지만, 현재는 가독성과 정보량 균형을 우선한 버전
### 2026-03-28 두 번째 레퍼런스 스타일 단계별 슬라이드 조정

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js`를 기준으로 4단계 순차 수정 진행
  - 1단계: 우측 설명 텍스트 상세화
  - 2단계: `FRONTEND`, `AI PLATFORM`, `DATABASE` 태그 박스 추가
  - 3단계: 가운데를 `backend-api -> ai-api -> ai-api-fastapi` 선형 다이어그램으로 단순화
  - 4단계: 우측을 `PostgreSQL`, `pgvector`, `LLM Provider` 분기 노드로 재구성
- Verification status
  - 단계별 PowerPoint COM 렌더 확인
  - `rendered-step1-detail/슬라이드1.PNG`
  - `rendered-step2-tags/슬라이드1.PNG`
  - `rendered-step3-linear/슬라이드1.PNG`
  - `rendered-step4-branches/슬라이드1.PNG`
- Design decision
  - 두 번째 레퍼런스처럼 박스/연결선 중심의 시스템 다이어그램 톤으로 이동하되, 현재 슬라이드 생성 파이프라인에서 깨지지 않는 범위 안에서 점진적으로 정리
- Caution
  - 현재 최종본은 방향성은 두 번째 레퍼런스에 가까워졌지만, 연결선 밀도와 우측 설명 텍스트 밀도는 추가 미세조정 여지가 남아 있음

### 2026-03-28 두 번째 레퍼런스 스타일 미세 조정 2차

- Completed change
  - `docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js`에서 `DATABASE` 태그와 `PostgreSQL / pgvector / LLM Provider` 노드를 소폭 상향 조정
  - 우측 연결선을 대각선 3개에서 세로 후 가로로 이어지는 얇은 분기선 형태로 보수적으로 변경
  - 최신 원본 `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx` 재생성
- Verification status
  - `node docs/slides/ai-api-system-architecture/build_ai_api_reference_style.js` 실행 성공
  - PowerPoint COM 렌더 성공
  - 최신 확인 PNG: `docs/slides/ai-api-system-architecture/rendered-step7-orthogonal-lines/슬라이드1.PNG`
- Design decision
  - PowerPoint 호환성을 지키기 위해 복잡한 connector 대신 일반 line shape를 여러 개 조합하는 방식 사용
  - 중심 흐름 `Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`는 유지하고, 우측 저장소 분기만 더 읽기 쉽게 정리
  - `backend-api`가 `ai-api-fastapi`를 직접 호출하지 않는 구조 설명은 유지
- Caution
  - PptxGenJS overlap 경고는 일부 남아 있지만, 실제 PowerPoint 열기와 COM PNG 렌더는 정상 확인
  - 다음 차수도 큰 폭 라우팅 변경 대신 선 라벨 위치, 우측 여백, 설명 텍스트 밀도처럼 한 축씩만 조정하는 것이 안전
### 2026-03-28 manual relabel seed v1 experiment

- Completed change
  - Added manual relabel helper scripts
    - `ai-api-fastapi/training/emotion_classifier/scripts/export_manual_relabel_candidates.py`
    - `ai-api-fastapi/training/emotion_classifier/scripts/apply_manual_relabel_seed.py`
  - Exported candidate CSVs
    - `ai-api-fastapi/training/emotion_classifier/processed/manual_relabel_candidates/train_manual_relabel_candidates_v1.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/manual_relabel_candidates/valid_manual_relabel_candidates_v1.csv`
  - Created approved seed CSV
    - `ai-api-fastapi/training/emotion_classifier/processed/manual_relabel_candidates/manual_relabel_seed_v1.csv`
  - Applied approved seeds and created medium compare CSVs
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_manual_seed_v1.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_manual_seed_v1.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_manual_seed_v1_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_manual_seed_v1_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v1.json`
- Distribution change
  - train
    - before: `SAD 19886`, `ANXIOUS 14524`, `ANGRY 11094`, `HAPPY 4281`, `CALM 1845`
    - after: `SAD 19881`, `ANXIOUS 14551`, `ANGRY 11072`, `HAPPY 4281`, `CALM 1845`
  - valid
    - before: `SAD 2190`, `ANXIOUS 1798`, `ANGRY 1440`, `HAPPY 752`, `CALM 461`
    - after: `SAD 2194`, `ANXIOUS 1818`, `ANGRY 1416`, `HAPPY 752`, `CALM 461`
- Verification status
  - candidate export count
    - train `9014`
    - valid `1063`
  - manual seed applied count
    - train `36`
    - valid `36`
  - manual reason breakdown
    - train
      - `manual_seed_sad_to_anxious_v1 = 14`
      - `manual_seed_angry_to_anxious_v1 = 13`
      - `manual_seed_angry_to_sad_v1 = 9`
    - valid
      - `manual_seed_angry_to_sad_v1 = 16`
      - `manual_seed_sad_to_anxious_v1 = 12`
      - `manual_seed_angry_to_anxious_v1 = 8`
  - medium compare result
    - accuracy `0.3573`
    - macro F1 `0.3059`
    - `HAPPY F1 = 0.4444`
    - `CALM F1 = 0.4557`
    - `ANXIOUS F1 = 0.0000`
    - `SAD F1 = 0.4043`
    - `ANGRY F1 = 0.2252`
- Interpretation
  - This is more stable than broad relabel experiments because it uses approved `sample_id` seeds only.
  - It still does not beat the current best baseline `valid_metrics_cpu_compare_medium_relabel_weighted.json` with accuracy `0.4267` and macro F1 `0.3645`.
  - Keep this as a reproducible seed workflow and expand the approved seed set before trying another compare run.

### 2026-03-28 architecture slide v3 minimal icon pass

- Completed change
  - Updated `docs/slides/ai-api-system-architecture/build_ai_api_reference_style_v3.js`
  - Reduced card interior text to icon + service-name style
  - Simplified `FRONTEND` card to `Next.js` 중심 표현
  - Replaced `Spring Boot / Orchestrator / Model Serving` copy with `backend-api / ai-api / ai-api-fastapi`
  - Kept right phase text shorter and presentation-oriented
- Verification status
  - `node docs/slides/ai-api-system-architecture/build_ai_api_reference_style_v3.js` success
  - PowerPoint COM render success
  - Confirmed render: `docs/slides/ai-api-system-architecture/rendered-v3-minimal-v2/슬라이드1.PNG`
  - Output deck: `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v3.pptx`
- Current state
  - This version is closer to the reference direction than the previous text-heavy variants
  - The slide is still editable and opens in PowerPoint
  - Remaining optional polish is limited to spacing and label density, not structural rewrite

### 2026-03-28 architecture slide v3 techlead-density pass

- Completed change
  - Expanded the usable left diagram area and shifted the right explanation column slightly to the right
  - Reduced pastel card feel so the service nodes read closer to icon-centered system diagram boxes
  - Rebuilt the right explanation column with smaller text and denser technical phrasing for presentation to a tech lead
  - Kept `v3` as the experimental path without overwriting the stable `v2` source
- Verification status
  - `node docs/slides/ai-api-system-architecture/build_ai_api_reference_style_v3.js` success
  - PowerPoint COM render success
  - Latest render: `docs/slides/ai-api-system-architecture/rendered-v3-techlead-v4/슬라이드1.PNG`
  - Output deck: `docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v3.pptx`
- Current state
  - Left-side diagram now occupies more visual weight than earlier `v3` passes
  - Right-side notes are denser and better aligned with architecture review discussion
  - Remaining polish is mostly visual cleanup around the lower-left legacy text masking and line spacing near the DB stack

## Immediate next work
1. Expand `manual_relabel_seed_v1.csv` into `manual_relabel_seed_v2.csv` by approving 20-40 more samples per bucket from the exported candidate CSVs.
2. Keep using approved `sample_id` seed application only, and avoid broad `emotion_major` or text-wide relabel rules.
3. Keep serving policy as `TIRED fallback-only`, and keep `valid_metrics_cpu_compare_medium_relabel_weighted.json` as the main learning baseline until a manual-seed run exceeds it.
### 2026-03-28 manual relabel seed v2 experiment

- Completed change
  - Expanded manual seed file
    - `ai-api-fastapi/training/emotion_classifier/processed/manual_relabel_candidates/manual_relabel_seed_v2.csv`
  - Applied v2 seeds and created compare inputs
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_manual_seed_v2.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_manual_seed_v2.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/train_emotion_mvp_manual_seed_v2_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/processed/valid_emotion_mvp_manual_seed_v2_cpu_compare_medium.csv`
    - `ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json`
- Distribution change
  - train
    - before: `SAD 19886`, `ANXIOUS 14524`, `ANGRY 11094`, `HAPPY 4281`, `CALM 1845`
    - after: `SAD 19878`, `ANXIOUS 14558`, `ANGRY 11068`, `HAPPY 4281`, `CALM 1845`
  - valid
    - before: `SAD 2190`, `ANXIOUS 1798`, `ANGRY 1440`, `HAPPY 752`, `CALM 461`
    - after: `SAD 2192`, `ANXIOUS 1826`, `ANGRY 1410`, `HAPPY 752`, `CALM 461`
- Verification status
  - seed rows
    - v1 `72`
    - v2 `91`
  - manual seed applied count
    - train `45`
    - valid `46`
  - newly added review notes
    - `manual_seed_sad_to_anxious_v2 = 9`
    - `manual_seed_angry_to_anxious_v2 = 6`
    - `manual_seed_angry_to_sad_v2 = 4`
  - medium compare result
    - accuracy `0.4427`
    - macro F1 `0.3888`
    - `HAPPY F1 = 0.2000`
    - `CALM F1 = 0.6318`
    - `ANXIOUS F1 = 0.2957`
    - `SAD F1 = 0.5000`
    - `ANGRY F1 = 0.3167`
- Interpretation
  - Manual seed expansion is the first experiment that exceeded the previous best baseline `valid_metrics_cpu_compare_medium_relabel_weighted.json` with accuracy `0.4267` and macro F1 `0.3645`.
  - However the confusion matrix shows strong `CALM` and `SAD` bias, while `HAPPY` fell sharply from `0.6146` to `0.2000`.
  - Treat this as the current best macro-F1 experiment, but not a final adopted serving model yet. The next check should focus on the `HAPPY -> CALM` collapse before replacing the learning baseline.
