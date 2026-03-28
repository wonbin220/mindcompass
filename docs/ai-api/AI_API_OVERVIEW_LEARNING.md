# ai-api 전체 개요 학습 문서

이 문서는 Mind Compass의 AI 계층 구조를 주니어 백엔드 개발자도 따라가기 쉽게 설명하기 위한 개요 문서다.

---

## 1. Goal

이 문서의 목적은 세 가지다.

1. 왜 AI 서버를 2계층으로 나누는지 이해한다.
2. `backend-api`, `ai-api`, `ai-api-fastapi`가 각각 어디까지 책임지는지 구분한다.
3. 실제 요청 흐름이 왜 `backend-api -> ai-api -> ai-api-fastapi`로 흘러야 하는지 이해한다.

---

## 2. Design Decision

Mind Compass는 모바일 앱 전용 구조가 아니라 반응형 웹을 포함한 웹 서비스 구조를 기준으로 설명한다.

현재 권장 구조:

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

여기서 중요한 점은 AI 서버를 "병렬 서버 2개"로 두는 것이 아니라, 역할이 다른 2계층으로 나누는 것이다.

- `ai-api` = AI 오케스트레이터
- `ai-api-fastapi` = 감정분류 모델 서빙 엔진

---

## 3. Related Files

- [README.md](C:\programing\mindcompass\docs\ai-api\README.md)
- [INTERNAL_API_SPEC_DRAFT.md](C:\programing\mindcompass\docs\ai-api\INTERNAL_API_SPEC_DRAFT.md)
- [ANALYZE_DIARY_API_LEARNING.md](C:\programing\mindcompass\docs\ai-api\ANALYZE_DIARY_API_LEARNING.md)
- [RISK_SCORE_API_LEARNING.md](C:\programing\mindcompass\docs\ai-api\RISK_SCORE_API_LEARNING.md)
- [GENERATE_REPLY_API_LEARNING.md](C:\programing\mindcompass\docs\ai-api\GENERATE_REPLY_API_LEARNING.md)
- [RAG_CONTEXT_API_LEARNING.md](C:\programing\mindcompass\docs\ai-api\RAG_CONTEXT_API_LEARNING.md)

---

## 4. 서버별 역할

### 4-1. backend-api

왜 존재하는가:

- 웹 서비스의 공개 API 진입점이 필요하기 때문이다.
- 인증, 저장, 조회, 권한 검증, 세션 관리 같은 비즈니스 책임은 AI 서버보다 `backend-api`에 두는 편이 안전하기 때문이다.

주요 역할:

- Auth / JWT / User
- Diary CRUD
- Calendar / Report 조회
- Chat session / message 저장
- AI 결과 저장 / 조회

하지 않는 것:

- 프롬프트 템플릿 관리
- RAG 문맥 조립
- 감정분류 모델 직접 로딩
- LLM 직접 호출

### 4-2. ai-api

왜 존재하는가:

- AI 관련 정책과 흐름 제어를 한곳에서 관리하려면 오케스트레이터가 필요하기 때문이다.

주요 역할:

- 프롬프트 템플릿 관리
- 메모리 조립
- RAG 검색 및 context building
- 어떤 AI 기능을 호출할지 결정
- `ai-api-fastapi` 호출
- LLM Provider 호출
- 최종 응답 조합
- safety / fallback / retry

하지 않는 것:

- 회원 / 권한 / 일반 CRUD
- 웹 클라이언트 공개 API 제공
- PyTorch 모델 직접 로딩

### 4-3. ai-api-fastapi

왜 존재하는가:

- 감정분류 모델 추론은 Python / PyTorch / tokenizer / 모델 버전 관리에 더 잘 맞기 때문이다.

주요 역할:

- 감정분류 추론
- 모델 버전 관리
- batch / 실험 라우팅
- threshold / calibration
- inference metadata 반환

하지 않는 것:

- 사용자 메모리 해석
- 상담 멘트 생성
- RAG
- 최종 AI 응답 생성

---

## 5. 실행 흐름

### 5-1. 일반 원칙

`backend-api`가 `ai-api-fastapi`를 직접 여기저기 호출하지 않게 하는 것이 중요하다.

이유:

- AI 정책이 `ai-api`에 모여야 구조가 덜 꼬인다.
- 모델 엔진을 바꿔도 `backend-api`는 덜 흔들린다.
- 메모리, RAG, safety 로직이 중복되지 않는다.

### 5-2. 요청이 들어왔을 때

1. Client request
2. `backend-api` Controller / Service
3. DB 저장 / 조회 / 권한 검증
4. Internal AI client가 `ai-api` 호출
5. `ai-api`가 메모리, RAG, safety 정책을 조립
6. 필요하면 `ai-api-fastapi`에 감정분류 요청
7. 필요하면 LLM Provider 호출
8. `ai-api`가 구조화된 최종 응답 반환
9. `backend-api`가 저장 / 가공 후 웹에 응답

---

## 6. 예시: 일기 분석 흐름

1. 사용자가 웹에서 일기를 저장한다.
2. `backend-api`가 일기 레코드를 저장한다.
3. `backend-api`가 `ai-api`에 일기 분석을 요청한다.
4. `ai-api`가 문맥 구성과 safety 판단을 준비한다.
5. 필요하면 `ai-api-fastapi`로 감정분류 추론을 요청한다.
6. 필요하면 RAG / LLM을 호출한다.
7. `ai-api`가 분석 결과를 조합한다.
8. `backend-api`가 결과를 저장하고 화면용 DTO로 반환한다.

---

## 7. 예시: 채팅 흐름

1. 사용자가 웹 채팅 화면에서 메시지를 보낸다.
2. `backend-api`가 세션과 메시지를 저장한다.
3. `backend-api`가 `ai-api`에 답변 생성을 요청한다.
4. `ai-api`가 메모리와 안전 분기를 확인한다.
5. 감정분류가 필요하면 `ai-api-fastapi`를 호출한다.
6. 근거 문맥이 필요하면 RAG store를 조회한다.
7. LLM Provider를 호출해 응답을 생성한다.
8. `ai-api`가 최종 답변과 메타데이터를 반환한다.
9. `backend-api`가 assistant 메시지를 저장하고 응답한다.

---

## 8. 조심해야 할 점

1. 메모리와 비즈니스 규칙은 FastAPI 쪽으로 보내지 않는다.
2. 실시간 서비스 경로와 무거운 실험 경로를 섞지 않는다.
3. `ai-api-fastapi` 응답은 감정분류 결과 중심으로 좁게 유지한다.
4. 최종 판단권은 `ai-api`에 두고, `ai-api-fastapi`는 모델 엔진 역할에 집중시킨다.

---

## 9. Next Step

- `backend-api` 내부 AI client 계약과 이 구조 설명을 다시 대조한다.
- `ai-api-fastapi` 응답 스키마를 감정분류 중심 계약으로 더 명확히 정리한다.
- 슬라이드와 API 명세에서 같은 용어를 계속 쓰도록 문서 표현을 맞춘다.
