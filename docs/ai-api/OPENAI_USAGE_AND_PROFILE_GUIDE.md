# ai-api OpenAI 비용 유발 API와 프로필 운영 가이드

이 문서는 `ai-api`에서 어떤 내부 API가 실제 OpenAI 비용을 유발하는지, 로컬 개발에서는 왜 기본 비활성화로 두는지, 그리고 어떤 프로필에서만 실제 OpenAI 호출을 허용하는지 빠르게 이어받기 위한 운영 메모다.

## 왜 이 정리가 필요한가

`ai-api-fastapi`의 KcELECTRA 감정분류 학습은 로컬 CPU 학습이라 OpenAI 비용을 거의 만들지 않는다.

반면 `ai-api`는 Spring AI `ChatClient`를 통해 실제 OpenAI를 호출할 수 있으므로, 로컬에서 일기 분석이나 채팅 검증을 반복하면 API 요금이 계속 누적될 수 있다.

이번 정리의 목표는 아래 두 가지다.

1. 어떤 API가 비용을 만드는지 명확히 보이게 한다.
2. 로컬 개발에서는 기본적으로 OpenAI를 끄고, `manual` / `prod`에서만 명시적으로 켜도록 한다.

---

## 비용을 유발하는 내부 API

현재 OpenAI 비용을 만들 수 있는 `ai-api` 내부 API는 아래 3개다.

### 1. `POST /internal/ai/analyze-diary`

- 왜 존재하나
  - 일기 본문의 대표 감정, 강도, 태그, 요약을 만들어 `backend-api`의 diary 흐름을 보조한다.
- 어떤 화면/기능이 쓰나
  - 일기 작성 및 조회 후 감정 분석 반영 흐름
- 주요 파일
  - `ai-api/src/main/java/com/mindcompass/aiapi/controller/DiaryAiController.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/DiaryAnalysisService.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringDiaryAnalysisPromptClient.java`
  - `backend-api/src/main/java/com/mindcompass/api/diary/client/AiDiaryAnalysisClient.java`
- 실행 순서
  - `backend-api`가 `ai-api`의 `/internal/ai/analyze-diary` 호출
  - Controller가 요청 수신
  - Service가 OpenAI prompt 호출 시도
  - 실패하거나 비활성화면 rule-based fallback 반환
- DB 영향
  - 직접 DB 쓰기 없음, 분석 결과는 상위 서비스가 저장할 수 있음
- 비용 포인트
  - `SpringDiaryAnalysisPromptClient.complete()`가 실제 OpenAI 호출 지점

### 2. `POST /internal/ai/risk-score`

- 왜 존재하나
  - 위험 신호를 분류해 `NORMAL_RESPONSE`, `SUPPORTIVE_RESPONSE`, `SAFETY_RESPONSE` 같은 안전 분기를 결정한다.
- 어떤 화면/기능이 쓰나
  - 채팅 메시지 전송 전 안전 점수 판단
  - 일기 저장 후 위험 신호 판단
- 주요 파일
  - `ai-api/src/main/java/com/mindcompass/aiapi/controller/SafetyAiController.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/RiskScoreService.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringRiskScorePromptClient.java`
  - `backend-api/src/main/java/com/mindcompass/api/chat/client/AiSafetyClient.java`
- 실행 순서
  - `backend-api`가 `ai-api`의 `/internal/ai/risk-score` 호출
  - Controller가 요청 수신
  - Service가 OpenAI prompt 호출 시도
  - 실패하거나 비활성화면 규칙 기반 fallback 반환
- DB 영향
  - 직접 DB 쓰기 없음, 상위 채팅/일기 흐름의 분기 결정에 사용
- 비용 포인트
  - `SpringRiskScorePromptClient.complete()`가 실제 OpenAI 호출 지점

### 3. `POST /internal/ai/generate-reply`

- 왜 존재하나
  - 사용자의 최근 메시지와 대화 문맥을 바탕으로 공감형 답변 초안을 생성한다.
- 어떤 화면/기능이 쓰나
  - 채팅 세션 메시지 전송 후 assistant 답변 생성
- 주요 파일
  - `ai-api/src/main/java/com/mindcompass/aiapi/controller/ChatAiController.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/ReplyGenerationService.java`
  - `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringReplyGenerationPromptClient.java`
  - `backend-api/src/main/java/com/mindcompass/api/chat/client/AiChatClient.java`
- 실행 순서
  - `backend-api`가 `ai-api`의 `/internal/ai/generate-reply` 호출
  - Controller가 요청 수신
  - Service가 OpenAI prompt 호출 시도
  - 실패하거나 비활성화면 fallback 답변 반환
- DB 영향
  - 직접 DB 쓰기 없음, 상위 채팅 서비스가 결과 메시지를 저장
- 비용 포인트
  - `SpringReplyGenerationPromptClient.complete()`가 실제 OpenAI 호출 지점

---

## 비용이 거의 없는 작업

아래 작업은 현재 구조상 OpenAI API 비용과 직접 연결되지 않는다.

- `ai-api-fastapi` KcELECTRA 감정분류 학습 및 평가
- `ai-api-fastapi` 모델 compare CSV 평가
- `backend-api`의 일반 CRUD, 조회, JWT, 캘린더/리포트 API
- `ai-api` fallback 로직 자체 검증

즉, 최근 감정분류 학습 세션은 OpenAI 요금의 주원인이 아니다.

---

## 이번 설정 변경

### 기본 정책

- 기본 실행: OpenAI 비활성화
- `dev`: OpenAI 비활성화
- `manual`: OpenAI 활성화
- `prod`: OpenAI 활성화

### 관련 설정 파일

- `ai-api/src/main/resources/application.yaml`
  - `spring.profiles.default=dev`
  - `ai.openai.enabled=false`
- `ai-api/src/main/resources/application-dev.yaml`
  - `ai.openai.enabled=false`
- `ai-api/src/main/resources/application-manual.yaml`
  - `ai.openai.enabled=true`
- `ai-api/src/main/resources/application-prod.yaml`
  - `ai.openai.enabled=true`

### 관련 코드 파일

- `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringDiaryAnalysisPromptClient.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringRiskScorePromptClient.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/SpringReplyGenerationPromptClient.java`
  - `ai.openai.enabled=true`일 때만 실제 OpenAI 호출 bean 등록
- `ai-api/src/main/java/com/mindcompass/aiapi/service/NoOpDiaryAnalysisPromptClient.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/NoOpRiskScorePromptClient.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/NoOpReplyGenerationPromptClient.java`
  - `ai.openai.enabled=false`일 때는 `null`을 반환해서 Service가 기존 fallback으로 내려가게 함

---

## 요청이 들어왔을 때 실행 흐름

### dev

- `backend-api`가 `ai-api` 내부 API 호출
- Controller가 요청 수신
- Service가 PromptClient 호출
- 등록된 bean은 `NoOp*PromptClient`
- `null` 반환
- Service가 기존 fallback 응답 반환

### manual / prod

- `backend-api`가 `ai-api` 내부 API 호출
- Controller가 요청 수신
- Service가 PromptClient 호출
- 등록된 bean은 `Spring*PromptClient`
- Spring AI `ChatClient`가 OpenAI 호출
- 성공 시 모델 응답 파싱, 실패 시 fallback 반환

---

## 예외 / fallback 동작

- `ai.openai.enabled=false`
  - 의도적으로 OpenAI를 부르지 않음
  - 비용 없음
  - 서비스는 fallback 응답 유지
- `ai.openai.enabled=true`인데 키가 없거나 잘못됨
  - prompt 호출 실패
  - 서비스는 fallback 응답 유지
- OpenAI 응답 JSON 파싱 실패
  - 서비스는 fallback 응답 유지

즉, 이번 변경은 OpenAI를 꺼도 diary/chat 흐름이 죽지 않게 하는 데 초점을 둔다.

---

## 실행 방법

### 로컬 기본 실행

아무 프로필도 주지 않으면 `dev`가 기본값이라 OpenAI가 꺼진다.

### 수동 OpenAI 검증 실행

기존 스크립트:

- `ai-api/scripts/start-ai-api-with-openai.ps1`

이 스크립트는 이제 기본적으로 `manual` 프로필로 실행한다.

예시:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-ai-api-with-openai.ps1 -OpenAiApiKey "sk-..."
```

필요하면 프로필도 명시할 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-ai-api-with-openai.ps1 -OpenAiApiKey "sk-..." -Profile "manual"
```

운영은 `SPRING_PROFILES_ACTIVE=prod`로 켜는 것을 전제로 한다.

---

## 개발 시 장단점

### 장점

- 실수 과금을 크게 줄인다.
- 로컬 개발과 E2E에서 fallback 흐름을 안전하게 검증할 수 있다.
- 키가 없어도 개발이 막히지 않는다.

### 단점

- dev에서는 실제 LLM 품질을 바로 볼 수 없다.
- 프롬프트 튜닝, 응답 문체, 실제 지연 시간 검증은 `manual` 또는 `prod` 계열에서 따로 확인해야 한다.

그래도 현재 MVP 단계에서는 비용 제어와 fallback 안정성이 더 중요하므로, 기본 비활성화가 더 안전한 선택이다.

---

## 다음 시작점

다음 세션에서 이어갈 때는 아래 순서로 보면 된다.

1. `application.yaml`, `application-dev.yaml`, `application-manual.yaml`, `application-prod.yaml`
2. `Spring*PromptClient` 와 `NoOp*PromptClient`
3. `ai-api/scripts/start-ai-api-with-openai.ps1`
4. 이 문서와 `docs/IMPLEMENTATION_STATUS.md`

---

## manual 품질 검증 체크리스트

- 실제 OpenAI 응답 품질 검증은 별도 체크리스트 기준으로만 진행한다.
- 체크리스트 문서:
  - `docs/ai-api/MANUAL_OPENAI_QUALITY_CHECKLIST.md`
- 원칙:
  - local dev에서는 실행하지 않는다.
  - `manual` 프로필에서만 수행한다.
  - pass/fail는 감상평이 아니라 safety, 구조 안정성, fallback 동작 기준으로 남긴다.
