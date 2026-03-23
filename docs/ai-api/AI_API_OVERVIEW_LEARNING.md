# ai-api 전체 학습 문서

이 문서는 `ai-api` 전체 구조를 이해하기 위한 개요 문서다.

---

# 1. ai-api는 왜 따로 분리하는가

이 프로젝트는 `Spring Boot + FastAPI 2서버 구조`를 사용한다.

핵심 원칙:
- 앱은 Spring Boot만 호출한다.
- Spring Boot가 내부적으로 FastAPI를 호출한다.
- Spring Boot는 저장/인증/업무 로직 중심이다.
- FastAPI는 AI 추론/분석/문맥 조립 중심이다.

즉, ai-api는 외부 앱이 직접 호출하는 공개 API 서버가 아니라,
**Spring Boot가 내부적으로 호출하는 AI 전용 서버**다.

---

# 2. ai-api가 맡는 역할

ai-api는 아래 같은 기능을 맡는다.

- 일기 감정 분석
- 위험 신호 스코어링
- 상담 답변 초안 생성
- RAG 문맥 조립
- 임베딩 생성
- 유사 문서/유사 일기 검색 질의 처리
- 추후 GraphRAG, 멀티모달, 실험용 추론 확장

이 중 MVP에서 가장 먼저 필요한 것은:
1. analyze-diary
2. risk-score
3. generate-reply

---

# 3. ai-api에서 자주 보는 폴더 역할

예시 구조:

```text
ai-api/
├─ app/
│  ├─ main.py
│  ├─ core/
│  │  ├─ config.py
│  │  └─ logging.py
│  ├─ routers/
│  │  ├─ diary_router.py
│  │  ├─ safety_router.py
│  │  └─ chat_router.py
│  ├─ schemas/
│  │  ├─ analyze_diary.py
│  │  ├─ risk_score.py
│  │  └─ generate_reply.py
│  ├─ services/
│  │  ├─ emotion_analysis_service.py
│  │  ├─ risk_scoring_service.py
│  │  ├─ reply_generation_service.py
│  │  └─ prompt_builder_service.py
│  ├─ clients/
│  │  ├─ openai_client.py
│  │  └─ vector_store_client.py
│  ├─ rag/
│  │  ├─ retriever.py
│  │  ├─ context_builder.py
│  │  └─ citation_formatter.py
│  └─ utils/
│     ├─ text_cleaner.py
│     └─ time_utils.py
└─ tests/
```

---

# 4. ai-api 안에서의 계층을 쉽게 설명하면

## 4-1. Router
FastAPI에서 HTTP 요청을 직접 받는 입구다.
Spring Boot의 Controller와 비슷하다.

예:
- `/internal/ai/analyze-diary`
- `/internal/ai/risk-score`
- `/internal/ai/generate-reply`

쉽게 말하면:
- Router = 요청 접수창구

## 4-2. Schema
요청과 응답 형식을 정의하는 Pydantic 모델이다.
Spring Boot의 Request DTO / Response DTO와 비슷하다.

쉽게 말하면:
- Schema = 입출력 포장 규격

## 4-3. Service
AI 로직의 핵심이 들어간다.
감정 분석, 위험 점수 계산, 답변 생성, 문맥 조립 등이 여기에 들어간다.

쉽게 말하면:
- Service = 실제 판단과 처리의 중심

## 4-4. Client
외부 LLM API, 벡터DB, 임베딩 서비스 등과 연결한다.

쉽게 말하면:
- Client = 외부 AI 도구와 대화하는 창구

## 4-5. RAG
검색/문맥 조립/출처 정리 같은 로직을 분리한 영역이다.

쉽게 말하면:
- RAG = 답변에 필요한 근거를 모으고 정리하는 곳

---

# 5. 외부 요청이 ai-api까지 오는 전체 흐름

ai-api는 앱이 직접 호출하지 않는다.

기본 흐름:
1. 앱이 Spring Boot로 요청한다.
2. Spring Boot Controller가 요청을 받는다.
3. Spring Boot Service가 저장/조회/권한 검증을 수행한다.
4. 필요하면 Spring Boot 내부 AI Client가 ai-api를 호출한다.
5. ai-api Router가 요청을 받는다.
6. ai-api Schema가 요청 body를 파싱한다.
7. ai-api Service가 핵심 AI 로직을 실행한다.
8. 필요하면 Client/RAG 계층을 호출한다.
9. ai-api Response Schema를 만든다.
10. Spring Boot가 결과를 받아 저장/가공한다.
11. 최종 응답이 앱으로 간다.

즉:
앱 → Spring Boot → ai-api → Spring Boot → 앱

---

# 6. ai-api를 이해할 때 꼭 기억할 문장

- ai-api는 공개 API 서버가 아니라 내부 AI 서버다.
- ai-api는 저장보다 분석/추론/문맥 조립이 중심이다.
- Router는 얇게, Service는 명확하게 유지하는 것이 좋다.
- 위험 신호 감지와 안전 분기는 일반 생성보다 우선이다.
- ai-api 실패가 전체 서비스 실패로 번지지 않게 Spring Boot가 중심을 잡아야 한다.
