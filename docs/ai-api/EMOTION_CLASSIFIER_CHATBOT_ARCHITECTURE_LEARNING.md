# 감정분류 모델과 챗봇 아키텍처 학습 문서

이 문서는 Mind Compass 기준으로 `모델 파인튜닝 + 내부 추론 API + Spring AI 챗봇 + RAG`를 한 흐름으로 설명하는 문서다.

---

# 1. Goal

- 감정분류 모델이 어디서 학습되고 어디서 서빙되는지 정한다.
- `backend-api`, `ai-api`, `ai-api-fastapi`가 각각 어떤 역할을 맡는지 정한다.
- 챗봇 프롬프트, 메모리, RAG를 어떤 순서로 붙일지 정한다.
- AI 실패 시 어떤 fallback으로 서비스 흐름을 지킬지 정한다.

---

# 2. Design Decision

권장 최종 구조는 아래와 같다.

```text
모바일 앱
  -> backend-api (Spring Boot public API)
      -> ai-api (Spring AI orchestration server)
          -> ai-api-fastapi (PyTorch emotion classifier serving, comparison target)
          -> LLM provider
          -> PostgreSQL + pgvector (RAG retrieval)
```

핵심 이유:

- 모바일 앱은 오직 `backend-api`만 호출한다.
- `backend-api`는 저장, 인증, 권한, 공개 API 계약을 담당한다.
- `ai-api`는 챗봇 orchestration, 프롬프트, 메모리, RAG, fallback을 담당한다.
- `ai-api-fastapi`는 Python 모델 서빙과 비교 실험을 담당한다.

즉 Python 감정분류 모델은 `ai-api-fastapi`에 두고,
챗봇의 중심 orchestration은 `ai-api`가 맡는 구조를 추천한다.

---

# 3. 이 구조가 왜 필요한가

이 프로젝트에는 AI 기능이 2종류 있다.

## 3-1. 분류형 AI

예:

- 대표 감정 분류
- 감정 강도 추정
- 위험 신호 보조 점수

이런 작업은:

- 입력/출력이 구조적이고
- PyTorch 모델 서빙에 잘 맞고
- FastAPI가 가볍게 운영하기 좋다.

## 3-2. 생성형 AI

예:

- 공감 답변 생성
- memory 반영 답변
- RAG 기반 설명

이런 작업은:

- 프롬프트 관리가 중요하고
- LLM 호출 실패 처리와 fallback이 중요하고
- Spring AI로 운영하면 Java 서비스와 붙이기 쉽다.

그래서:

- 분류형 = `ai-api-fastapi`
- 생성형 orchestration = `ai-api`

로 분리하는 것이 가장 깔끔하다.

---

# 4. 실제 저장소 기준 관련 파일

## 4-1. 공개 API 진입점

- `backend-api/src/main/java/com/mindcompass/api/diary/controller/DiaryController.java`
- `backend-api/src/main/java/com/mindcompass/api/diary/service/DiaryService.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/controller/ChatController.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/service/ChatService.java`

## 4-2. backend-api 내부 AI 호출부

- `backend-api/src/main/java/com/mindcompass/api/diary/client/AiDiaryAnalysisClient.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/client/AiSafetyClient.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/client/AiChatClient.java`

## 4-3. Spring AI 내부 서버

- `ai-api/src/main/java/com/mindcompass/aiapi/controller/DiaryAiController.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/controller/SafetyAiController.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/controller/ChatAiController.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/DiaryAnalysisService.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/RiskScoreService.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/service/ReplyGenerationService.java`

## 4-4. FastAPI 비교/모델 서빙 서버

- `ai-api-fastapi/app/main.py`
- `ai-api-fastapi/app/routers/diary_router.py`
- `ai-api-fastapi/app/routers/safety_router.py`
- `ai-api-fastapi/app/routers/chat_router.py`
- `ai-api-fastapi/app/schemas/analyze_diary.py`
- `ai-api-fastapi/app/services/emotion_analysis_service.py`

## 4-5. 앞으로 추가 추천 경로

```text
ai-api-fastapi/
├─ app/
│  ├─ routers/
│  │  ├─ diary_router.py
│  │  └─ model_router.py
│  ├─ schemas/
│  │  ├─ analyze_diary.py
│  │  └─ emotion_classify.py
│  ├─ services/
│  │  ├─ emotion_analysis_service.py
│  │  └─ emotion_classifier_service.py
│  ├─ models/
│  │  └─ emotion_classifier.py
│  └─ inference/
│     ├─ predictor.py
│     └─ label_mapper.py
└─ training/
   └─ emotion_classifier/
      ├─ raw/
      ├─ processed/
      ├─ scripts/
      └─ artifacts/
```

---

# 5. 전체 아키텍처 초안

```text
[모바일 앱]
   |
   v
[backend-api]
   |- Auth/JWT
   |- Diary CRUD
   |- Chat Session/Message 저장
   |- Report/Calendar 조회
   |
   +--> [ai-api]
          |- analyze-diary orchestration
          |- risk-score orchestration
          |- generate-reply orchestration
          |- prompt assembly
          |- memory summary
          |- RAG retrieval
          |
          +--> [ai-api-fastapi]
          |      |- KcELECTRA 감정분류 추론
          |      |- 필요 시 보조 안전 분류 실험
          |
          +--> [LLM Provider via Spring AI]
          |
          +--> [PostgreSQL + pgvector]
                 |- diary memory
                 |- counseling knowledge
                 |- retrieved context
```

---

# 6. Diary API에서의 실행 흐름

이 흐름은 주니어 개발자가 가장 먼저 이해해야 할 흐름이다.

## 6-1. Client request

모바일 앱이 `POST /api/v1/diaries`를 `backend-api`로 보낸다.

## 6-2. Security / JWT filter

Spring Security가 JWT를 검사해서 현재 사용자 ID를 확인한다.

## 6-3. Controller

`DiaryController`가 요청을 받는다.

## 6-4. Service

`DiaryService`가:

- diary 저장
- 사용자 입력 감정 태그 저장
- 내부 AI 분석 호출

을 조정한다.

## 6-5. Repository / DB

- `DiaryRepository`
- `DiaryEmotionRepository`
- `DiaryAiAnalysisRepository`

가 diary와 분석 결과를 저장한다.

## 6-6. Internal AI client

`AiDiaryAnalysisClient`가 내부적으로 `ai-api`를 호출한다.

## 6-7. ai-api 내부 흐름

1. `DiaryAiController`가 `/internal/ai/analyze-diary` 요청 수신
2. `AnalyzeDiaryRequest` DTO 파싱
3. `DiaryAnalysisService` 실행
4. 서비스가 필요하면 `ai-api-fastapi`의 감정분류 endpoint 호출
5. 감정 라벨, 강도, 태그를 조합
6. 필요 시 LLM 요약 문장 생성
7. `AnalyzeDiaryResponse` 반환

## 6-8. Response DTO

`backend-api`가 결과를 받아:

- `primaryEmotion`
- `emotionIntensity`
- `emotionTags`
- `summary`
- `confidence`

를 diary 응답 DTO에 담아 앱으로 보낸다.

---

# 7. Chat API에서의 실행 흐름

## 7-1. Client request

모바일 앱이 `POST /api/v1/chat/sessions/{sessionId}/messages`를 보낸다.

## 7-2. Controller

`ChatController`가 메시지 요청을 받는다.

## 7-3. Service

`ChatService`가 아래 순서로 조정한다.

1. 사용자 메시지 저장
2. `AiSafetyClient`로 risk-score 확인
3. 위험하면 safety/supportive 응답 우선
4. 위험하지 않으면 `AiChatClient`로 generate-reply 호출
5. assistant 메시지 저장
6. 응답 DTO 반환

## 7-4. ai-api 내부 흐름

`ChatAiController` -> `ReplyGenerationService`

이때 `ReplyGenerationService`는:

- 현재 사용자 메시지
- 최근 대화 이력
- memory summary
- diary 감정분석 결과
- RAG 검색 결과

를 묶어서 LLM 프롬프트를 만든다.

---

# 8. Spring AI 챗봇 구조는 어떻게 붙일까

권장 구조:

## 8-1. Prompt

시스템 프롬프트는 최소 아래 정보를 포함한다.

- 역할: 공감형 멘탈 헬스 동반자
- 금지사항: 진단 단정, 공격적 표현, 위험 상황 축소
- 응답 원칙: 짧고 따뜻하게, 행동 1개 제안
- safety override: 위험하면 일반 답변보다 안전 안내 우선

## 8-2. Memory

메모리는 2단계로 나눈다.

### 단기 메모리

- 현재 세션 최근 10~20개 메시지
- DB: `chat_messages`

### 장기 메모리

- 최근 diary 감정 요약
- 반복되는 고민 주제
- 사용자가 선호하는 응답 톤
- DB: PostgreSQL 테이블 + 필요 시 summary 컬럼

## 8-3. RAG

RAG는 아래 2가지 소스를 먼저 붙인다.

1. 내부 상담/가이드 문서
2. 사용자의 과거 diary 요약 및 감정 흐름

벡터 저장은 `PostgreSQL + pgvector`를 사용한다.

---

# 9. 감정분류 모델은 챗봇에서 어떻게 쓰이는가

감정분류 모델은 챗봇을 대체하는 것이 아니라,
챗봇이 더 정확하게 반응하도록 돕는 입력값이다.

예:

- `ANXIOUS`면 답변 tone을 더 안정적으로
- `SAD`면 위로와 지지 중심으로
- `ANGRY`면 공감 후 감정 정리 질문 중심으로
- `TIRED`면 부담 적은 휴식/정리 제안 중심으로

즉 분류 모델 출력은 프롬프트의 condition으로 들어간다.

예시:

```text
primaryEmotion=ANXIOUS
emotionTags=[OVERWHELMED]
riskLevel=LOW
memorySummary=최근 업무와 인간관계 스트레스가 반복됨
retrievedContext=불안이 높을 때 호흡/수면/생각 기록 관련 가이드
```

이 입력을 바탕으로 LLM이 최종 답변을 만든다.

---

# 10. 왜 ai-api가 중간 orchestration을 맡아야 하나

Python 감정분류 모델을 바로 `backend-api`에서 부를 수도 있다.

하지만 권장 구조는:

`backend-api -> ai-api -> ai-api-fastapi`

다.

이유:

- 챗봇, 프롬프트, 메모리, RAG는 ai-api에 모으는 것이 더 자연스럽다.
- 감정분류 모델이 바뀌어도 `backend-api` 계약 변경이 적다.
- 추후 FastAPI 대신 다른 Python serving으로 바뀌어도 ai-api 안에서만 조정하면 된다.

예외:

MVP 초기에 diary 감정분류만 급히 붙일 때는
`backend-api -> ai-api-fastapi`
직접 호출도 가능하다.

하지만 장기 구조는 `ai-api` 중간 orchestration을 추천한다.

---

# 11. 내부 API 스켈레톤 제안

## 11-1. ai-api-fastapi emotion classify

```text
POST /internal/model/emotion-classify
```

요청 예시:

```json
{
  "text": "요즘 회사 일 때문에 너무 지치고 불안해요.",
  "returnTopK": 3
}
```

응답 예시:

```json
{
  "primaryEmotion": "ANXIOUS",
  "confidence": 0.81,
  "scores": [
    { "label": "ANXIOUS", "score": 0.81 },
    { "label": "TIRED", "score": 0.12 },
    { "label": "SAD", "score": 0.04 }
  ],
  "emotionTags": ["OVERWHELMED"]
}
```

## 11-2. ai-api analyze-diary orchestration

```text
POST /internal/ai/analyze-diary
```

내부 처리:

1. diary 텍스트 수신
2. emotion-classify 호출
3. 필요 시 summary 생성
4. 결과 구조화

---

# 12. Controller / Service / Repository / DTO / Internal AI Client 역할 정리

## backend-api

### Controller

- 공개 HTTP 요청을 받는다.
- 인증된 사용자 기준으로 요청을 위임한다.

### Service

- 저장, 권한, 업무 흐름을 조정한다.
- 내부 AI 실패 시 fallback 결정도 여기서 한다.

### Repository

- diary, chat session, chat message, report 집계를 담당한다.

### DTO

- 앱과 주고받는 public API 계약이다.

### Internal AI Client

- `AiDiaryAnalysisClient`
- `AiSafetyClient`
- `AiChatClient`

역할:

- `ai-api` 또는 `ai-api-fastapi`를 내부 호출한다.

## ai-api

### Controller

- Spring Boot 내부 요청을 받는다.

### Service

- 프롬프트, 메모리, RAG, 모델 호출을 조정한다.

### DTO

- internal request/response 계약을 정의한다.

### Internal Model Client

- `ai-api-fastapi` emotion model endpoint 호출
- pgvector retrieval 호출
- LLM provider 호출

---

# 13. DB impact

이번 설계에서 추가 또는 활용되는 저장 포인트는 아래와 같다.

## backend-api / PostgreSQL

- `diaries`
- `diary_ai_analyses`
- `chat_sessions`
- `chat_messages`

추가 추천:

- `chat_memory_summaries`
- `rag_documents`
- `rag_document_chunks`

## pgvector 활용

- 상담 가이드 문서 임베딩
- diary summary 임베딩
- 사용자 장기 메모리 임베딩

---

# 14. Failure / fallback behavior

이 프로젝트에서 가장 중요한 부분이다.

## 14-1. 감정분류 모델 실패

상황:

- `ai-api-fastapi` 다운
- 모델 로딩 실패
- timeout

대응:

- `ai-api`가 규칙 기반 fallback 감정으로 응답
- `confidence`를 낮게 설정
- diary 저장은 계속 진행

## 14-2. RAG 실패

상황:

- pgvector 검색 실패
- 관련 문서 없음

대응:

- retrieved context 없이 일반 공감 답변 생성
- 검색 실패 때문에 채팅 전체 실패로 보내지 않는다.

## 14-3. LLM 실패

상황:

- API key 문제
- provider timeout
- 응답 파싱 실패

대응:

- `ReplyGenerationService`가 canned fallback 응답 반환
- `ChatService`는 assistant 메시지를 fallback 문구로 저장

## 14-4. 고위험 메시지

상황:

- 자해/자살 암시
- 고위험 추천 액션

대응:

- 일반 생성형 답변보다 safety 응답 우선
- 필요 시 챗봇 생성 자체를 건너뛴다.

---

# 15. 단계별 실행 계획

## 1단계
감성대화 데이터 `xlsx -> processed csv` 변환

## 2단계
6개 service label 매핑표 확정

## 3단계
`beomi/KcELECTRA-base` 파인튜닝 스크립트 작성

## 4단계
`ai-api-fastapi`에 emotion-classify endpoint 추가

## 5단계
`ai-api`에서 emotion-classify 호출하는 client/service 추가

## 6단계
`backend-api` diary/chat 흐름과 연결

## 7단계
pgvector 기반 RAG 문서 적재

## 8단계
Spring AI 프롬프트 + 메모리 + RAG 결합

---

# 16. 다음 단계

다음 구현 세션에서 바로 이어서 하기 좋은 작업은 아래 3개다.

1. `ai-api-fastapi/training/emotion_classifier/processed` 기준 CSV 생성 스크립트 만들기
2. `emotion-classify` FastAPI schema/service/router 스켈레톤 만들기
3. `ai-api`에서 해당 모델 서버를 호출하는 내부 client 설계하기
