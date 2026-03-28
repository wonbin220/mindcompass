# ai-api Internal API Spec Draft

이 문서는 `backend-api`와 `ai-api` 사이의 내부 API 계약 초안을 정리한 문서다.

이 문서에서 중요한 전제는 다음과 같다.

- 감정캠퍼스는 모바일 앱 전용이 아니라 반응형 웹 서비스 기준으로 설명한다.
- 공개 API 진입점은 `backend-api` 하나다.
- `ai-api`는 AI 오케스트레이터다.
- `ai-api-fastapi`는 감정분류 모델 서빙 계층이다.
- AI 관련 내부 진입점은 되도록 `ai-api` 하나로 고정한다.

---

## 1. Goal

1. `backend-api -> ai-api` 내부 계약을 먼저 고정한다.
2. `ai-api`와 `ai-api-fastapi`의 책임 경계를 문서로 분명히 한다.
3. 향후 RAG, 메모리, safety, 모델 실험이 늘어나도 구조가 흔들리지 않도록 기준점을 만든다.

---

## 2. Design Decision

### 전체 구조

```text
[ Responsive Web ]
        |
        v
[ backend-api ]
        |
        v
[ ai-api ]
   |          \
   |           \
   v            v
[ ai-api-fastapi ]   [ LLM Provider ]
        |
        v
[ pgvector / RAG store ]
```

### 책임 분리

#### `backend-api`

- 웹용 공개 REST API
- 인증 / 인가 / CRUD / 세션 / 저장
- AI 결과 저장 및 화면용 DTO 반환

#### `ai-api`

- 프롬프트, 메모리, RAG, safety, fallback, 최종 응답 조합
- 필요하면 `ai-api-fastapi`와 LLM Provider를 호출

#### `ai-api-fastapi`

- 감정분류 모델 추론
- 모델 버전 / threshold / calibration / inference metadata

### 왜 이렇게 두는가

- `backend-api`가 모델 서버를 직접 호출하기 시작하면 AI 정책이 여기저기 흩어진다.
- 메모리, safety, 프롬프트 로직은 `ai-api`에 모아야 유지보수가 쉽다.
- 감정분류 모델은 Python / PyTorch 스택과 더 잘 맞아서 별도 계층이 자연스럽다.

---

## 3. Related Files

- [DiaryAiController.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\controller\DiaryAiController.java)
- [SafetyAiController.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\controller\SafetyAiController.java)
- [ChatAiController.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\controller\ChatAiController.java)
- [HealthController.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\controller\HealthController.java)
- [AnalyzeDiaryRequest.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\AnalyzeDiaryRequest.java)
- [AnalyzeDiaryResponse.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\AnalyzeDiaryResponse.java)
- [RiskScoreRequest.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\RiskScoreRequest.java)
- [RiskScoreResponse.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\RiskScoreResponse.java)
- [GenerateReplyRequest.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\GenerateReplyRequest.java)
- [GenerateReplyResponse.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\dto\GenerateReplyResponse.java)
- [DiaryAnalysisService.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\service\DiaryAnalysisService.java)
- [RiskScoreService.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\service\RiskScoreService.java)
- [ReplyGenerationService.java](C:\programing\mindcompass\ai-api\src\main\java\com\mindcompass\aiapi\service\ReplyGenerationService.java)

---

## 4. Internal API Summary

| Method | Path | Why it exists | Main caller | Main role |
| --- | --- | --- | --- | --- |
| `GET` | `/health` | AI 오케스트레이터 상태 확인 | `backend-api`, ops | 헬스체크 |
| `POST` | `/internal/ai/analyze-diary` | 일기 감정 분석 | diary flow | 분석 오케스트레이션 |
| `POST` | `/internal/ai/risk-score` | 위험도 / safety 분기 | diary, chat flow | safety 오케스트레이션 |
| `POST` | `/internal/ai/generate-reply` | 상담 답변 생성 | chat flow | 메모리 + RAG + 답변 조합 |

주의:

- 위 표는 `ai-api`의 공개 범위를 설명하는 것이 아니라 `backend-api`가 내부적으로 호출하는 계약을 설명한다.
- `ai-api-fastapi`의 모델 추론 API는 `backend-api`의 직접 호출 대상이 아니다.

---

## 5. Execution Flow by Layer

### 5-1. Client request

- 사용자는 반응형 웹에서 기능을 호출한다.
- 웹은 `backend-api`만 호출한다.

### 5-2. backend-api

- Controller가 요청을 받는다.
- Service가 권한, 저장, 세션, 비즈니스 검증을 수행한다.
- AI가 필요하면 내부 AI client가 `ai-api`를 호출한다.

### 5-3. ai-api

- Controller가 요청을 받는다.
- DTO가 payload를 파싱한다.
- Service가 메모리, RAG, safety, prompt 조합을 수행한다.
- 필요하면 `ai-api-fastapi`와 LLM Provider를 호출한다.
- 구조화된 응답을 `backend-api`로 돌려준다.

### 5-4. ai-api-fastapi

- 감정분류 모델 입력을 받는다.
- 모델 버전 / threshold 기준으로 추론한다.
- label, score, sub-label, model_version 같은 메타데이터를 반환한다.

---

## 6. ai-api와 ai-api-fastapi의 경계

### `ai-api`가 해야 하는 것

- 프롬프트 템플릿 관리
- 메모리 조립
- RAG retrieval / context building
- safety fallback
- 최종 응답 조합

### `ai-api-fastapi`가 해야 하는 것

- 감정분류 추론
- 모델 버전 관리
- 실험 / 비교 / calibration
- inference metadata 반환

### `ai-api-fastapi`가 하지 말아야 하는 것

- 상담 멘트 생성
- 사용자 상태의 최종 판단
- 장기 메모리 조립
- RAG 근거 선택

---

## 7. Example Contract for ai-api-fastapi

아래는 `ai-api`가 내부적으로 기대하는 감정분류 응답 예시다.

```json
{
  "modelVersion": "emotion-ko-v3",
  "label": "sadness",
  "score": 0.91,
  "subLabels": [
    {"label": "loneliness", "score": 0.72},
    {"label": "fatigue", "score": 0.41}
  ]
}
```

이 응답은 감정분류 결과에 집중해야 하며, 상담 멘트나 메모리 해석 결과를 섞지 않는 것이 좋다.

---

## 8. API Details

### 8-1. `GET /health`

왜 필요한가:

- `backend-api`가 `ai-api`의 생존 여부를 먼저 확인할 수 있어야 하기 때문이다.

응답 예시:

```json
{
  "status": "ok",
  "service": "ai-api",
  "runtime": "spring-ai"
}
```

예외 / fallback:

- 별도 fallback은 없고, 연결 실패 자체가 장애 신호다.

### 8-2. `POST /internal/ai/analyze-diary`

왜 필요한가:

- 일기 텍스트를 감정, 강도, 요약, 태그로 구조화해 diary 상세, report, chat 진입에 활용하기 위해 필요하다.

실행 순서:

1. `backend-api` diary service가 내부 AI client로 `ai-api`를 호출한다.
2. `DiaryAiController`가 요청을 받는다.
3. `AnalyzeDiaryRequest`가 입력을 검증한다.
4. `DiaryAnalysisService`가 prompt, memory, retrieval 필요 여부를 판단한다.
5. 필요하면 `ai-api-fastapi`의 감정분류 결과를 참고한다.
6. 필요하면 LLM 결과와 결합해 최종 분석 응답을 만든다.
7. 실패 시 규칙 기반 fallback을 반환한다.

### 8-3. `POST /internal/ai/risk-score`

왜 필요한가:

- 일반 응답, supportive 응답, safety 응답 중 어떤 경로로 갈지 먼저 판단해야 하기 때문이다.

실행 순서:

1. `backend-api` chat 또는 diary flow가 `risk-score`를 요청한다.
2. `SafetyAiController`가 요청을 받는다.
3. `RiskScoreService`가 safety policy와 모델 결과를 조합한다.
4. 필요하면 `ai-api-fastapi` 감정분류 결과를 참고한다.
5. `LOW`, `MEDIUM`, `HIGH`와 추천 action을 반환한다.
6. 실패 시 규칙 기반 safety fallback을 적용한다.

### 8-4. `POST /internal/ai/generate-reply`

왜 필요한가:

- 대화 메모리와 맥락을 바탕으로 최종 상담 응답을 생성하는 오케스트레이션이 필요하기 때문이다.

실행 순서:

1. `backend-api` chat service가 `generate-reply`를 호출한다.
2. `ChatAiController`가 요청을 받는다.
3. `ReplyGenerationService`가 메모리, 최근 대화, safety 상태를 조립한다.
4. 필요하면 RAG store를 조회한다.
5. 필요하면 `ai-api-fastapi`의 감정분류 결과를 참고한다.
6. LLM Provider를 호출한다.
7. 최종 응답 DTO를 `backend-api`에 반환한다.
8. 실패 시 공감형 fallback 메시지를 반환한다.

---

## 9. Fallback Rule

AI 관련 실패가 전체 서비스 실패로 번지지 않는 것이 중요하다.

원칙:

- `backend-api`는 AI 결과가 없어도 저장 / 조회 흐름을 유지한다.
- `ai-api`는 모델 호출 실패 시 규칙 기반 fallback을 가진다.
- `ai-api-fastapi` 실패 시 `ai-api`가 대체 경로를 선택한다.

---

## 10. Next Step

- `backend-api` 내부 AI client가 이 계층 구조를 제대로 따르는지 재점검한다.
- `ai-api-fastapi` 추론 응답 스키마를 감정분류 중심 계약으로 고정한다.
- 향후 OpenAPI 문서 버전에서는 `backend-api -> ai-api`와 `ai-api -> ai-api-fastapi` 계약을 분리해 적는다.
