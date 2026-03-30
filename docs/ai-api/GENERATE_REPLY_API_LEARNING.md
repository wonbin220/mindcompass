# Generate Reply API 학습 문서

이 문서는 ai-api의 `상담 답변 생성` 내부 API를 학습하기 위한 문서다.

---

# 1. 이 API가 왜 필요한가

사용자가 AI 상담 화면에서 메시지를 보내면,
서비스는 그에 맞는 다음 응답을 생성해야 한다.

하지만 단순 생성만 하면 안 된다.
답변은 다음을 동시에 만족해야 한다.

- 공감적이어야 한다
- 과도하게 단정하지 않아야 한다
- 위험 신호가 있으면 안전 우선 흐름을 반영해야 한다
- 필요하면 RAG 근거를 반영해야 한다
- 이전 대화 맥락을 이어야 한다

그래서 `generate-reply` API가 필요하다.

---

# 2. 엔드포인트

`POST /internal/ai/generate-reply`

---

# 3. 어떤 화면/기능에서 간접적으로 쓰이는가

- AI 상담 채팅방의 AI 답변
- 일기 저장 후 “AI 상담으로 이어가기”
- 공감 기억 저장소 기반 연속 상담
- 근거 기반 상담 UX

즉, 사용자가 AI와 실제로 상호작용한다고 느끼는 핵심이 이 API다.

---

# 4. 요청 예시

```json
{
  "userId": 1,
  "sessionId": 501,
  "message": "오늘 너무 불안해서 아무것도 못 했어요.",
  "conversationHistory": [
    {"role": "user", "content": "요즘 계속 마음이 불안해요."},
    {"role": "assistant", "content": "최근 어떤 상황에서 가장 불안함이 커지는지 이야기해볼까요?"}
  ],
  "riskLevel": "MEDIUM",
  "memorySummary": "사용자는 업무 압박과 잠 부족 상황에서 불안을 자주 호소함",
  "mode": "EMPATHETIC_WITH_EVIDENCE"
}
```

---

# 5. 응답 예시

```json
{
  "reply": "오늘 불안감이 많이 커져서 아무것도 하기 어려우셨겠어요. 지금 그 불안을 가장 크게 키운 장면이 무엇이었는지 하나만 같이 짚어볼까요?",
  "usedEvidence": [
    {
      "title": "CBT grounding guidance",
      "summary": "강한 불안 시 현재 감각에 주의를 돌리는 짧은 grounding 기법이 도움이 될 수 있음"
    }
  ],
  "confidence": 0.78,
  "responseType": "NORMAL"
}
```

---

# 6. 관련 파일 예시

## FastAPI
- `app/routers/chat_router.py`
- `app/schemas/generate_reply.py`
- `app/services/reply_generation_service.py`
- `app/services/prompt_builder_service.py`
- `app/services/memory_summary_service.py`
- `app/rag/retriever.py`
- `app/rag/context_builder.py`
- `app/clients/openai_client.py`

## Spring Boot 측 호출부
- `backend-api/.../client/AiChatClient.java`
- `backend-api/.../service/ChatService.java`

---

# 7. FastAPI 안에서 실행 순서

1. Spring Boot가 `/internal/ai/generate-reply`를 호출한다.
2. `chat_router.py`가 요청을 받는다.
3. body가 `GenerateReplyRequest` 스키마로 변환된다.
4. router가 `reply_generation_service.generate_reply()`를 호출한다.
5. service가 현재 riskLevel을 확인한다.
6. 필요하면 safety 우선 응답 규칙을 적용한다.
7. 대화 히스토리, memorySummary, mode를 기반으로 입력 문맥을 정리한다.
8. 필요하면 `rag/retriever.py`가 관련 근거를 검색한다.
9. `context_builder.py`가 최종 프롬프트용 문맥을 조립한다.
10. `prompt_builder_service.py`가 모델 입력용 프롬프트를 만든다.
11. `openai_client.py`가 외부 모델을 호출한다.
12. 응답을 파싱하고 `GenerateReplyResponse` 스키마로 만든다.
13. ai-api가 Spring Boot에 반환한다.
14. Spring Boot가 응답을 저장하고 앱에 최종 전달한다.

---

# 8. 파일별 역할을 쉽게 설명하면

## chat_router.py
채팅 응답 생성 요청 접수

## generate_reply.py
입력/출력 스키마

## reply_generation_service.py
전체 흐름 지휘자
- safety 확인
- memory 반영
- rag 호출
- prompt 생성
- 모델 호출
- 결과 정리

## prompt_builder_service.py
LLM에게 보낼 시스템 지침/응답 형식 구성

## retriever.py
관련 문서/가이드/기억 검색

## context_builder.py
검색 결과와 대화 맥락을 합쳐 프롬프트 문맥 생성

## openai_client.py
실제 생성 호출

---

# 9. 왜 이 API가 가장 복잡한가

이 API는 단순 생성이 아니라 아래를 모두 다룬다.

- 현재 사용자 메시지
- 이전 대화
- 감정 상태
- 위험 수준
- 기억 요약
- 외부 근거
- 답변 톤/모드

즉, `generate-reply`는 ai-api의 핵심 오케스트레이션 API다.

---

# 10. 예외 상황

- 대화 문맥이 너무 길어 토큰 초과
- RAG 검색 결과 없음
- 외부 모델 응답 형식 파손
- 고위험인데 일반 답변이 생성됨
- 메모리 요약이 오래되어 현재 상황과 어긋남

대응 전략 예시:
- 최근 N개 메시지만 사용
- RAG 미존재 시 근거 없이 공감 모드 fallback
- safety 우선 템플릿 강제
- 출력 파서 검증 강화

---

# 11. 학습 포인트

- 답변 생성은 “텍스트 한 줄 뽑기”가 아니라 문맥 조립 문제다.
- 생성 품질 못지않게 safety와 consistency가 중요하다.
- service가 지휘자 역할을 하고, client/rag는 부품 역할을 한다.

---

# 12. 실패 시 운영 가이드

## 실패 징후
- Chat 메시지 전송 응답의 `responseType`이 `FALLBACK`으로 내려온다.
- assistant 메시지가 일반 reply가 아니라 fallback 문구로 저장된다.
- 서버 로그에 `Chat AI orchestration failed` 또는 `Chat AI reply generation failed` 경고가 남는다.

## backend-api 영향
- 사용자 메시지는 먼저 저장된다.
- assistant 메시지도 저장되지만 fallback 문구일 수 있다.
- 세션 상세 조회에서는 대화 흐름이 끊기지 않는다.

## 1차 확인 순서
1. ai-api가 실행 중인지 `GET http://localhost:8001/health`로 확인
2. `POST /internal/ai/generate-reply` 직접 호출로 `NORMAL` 응답 확인
3. `POST /internal/ai/risk-score`가 정상 동작하는지 함께 확인
4. backend-api 로그에서 ai-api 호출 실패 경고 확인

## 운영 메모
- generate-reply 실패는 Chat 저장 실패로 번지지 않도록 설계돼 있다.
- fallback 응답은 장애 은닉이 아니라 대화 기록 보존용 안전 장치다.

---

# 13. Spring AI 비교 서버 기준 현재 구조

현재 `ai-api`의 `generate-reply`는 단순 기본 문장 반환만 하지 않고,
아래 순서로 동작하도록 정리했다.

1. Spring Boot가 `POST /internal/ai/generate-reply`를 호출한다.
2. `ChatAiController`가 요청을 받는다.
3. `ReplyGenerationService`가 먼저 Spring AI 프롬프트 생성을 시도한다.
4. 모델 응답이 계약에 맞는 JSON이면 그 값을 그대로 사용한다.
5. 모델 응답이 비어 있거나 JSON 파싱에 실패하면 안전한 fallback 답변으로 내려간다.
6. 최종적으로 `GenerateReplyResponse`를 Spring Boot에 반환한다.

즉 현재 구조는:

- 1차: Spring AI 프롬프트 시도
- 2차: 구조화 JSON 파싱
- 실패 시: 안전한 fallback reply

## 왜 이렇게 바꿨는가

비교 목적상 `generate-reply`도 실제 모델 시도를 해봐야
나중에 `ai-api-fastapi`와 응답 품질, 유지보수성, fallback 안정성을 비교할 수 있다.

하지만 멘탈헬스 서비스에서는 생성 실패 때문에 대화 흐름이 끊기면 안 되므로,
프롬프트 실패 시에도 assistant 메시지가 비지 않게 fallback을 유지한다.

## 현재 코드 기준 역할

- `ChatAiController`
  - 내부 `/generate-reply` 엔드포인트 진입점
- `ReplyGenerationService`
  - 프롬프트 시도
  - JSON 파싱
  - fallback 답변 생성
- `ReplyGenerationPromptClient`
  - 프롬프트 호출 인터페이스
- `SpringReplyGenerationPromptClient`
  - Spring AI `ChatClient` 기반 실제 호출 구현

## 현재 fallback 원칙

- 입력 메시지가 비어 있으면 짧은 입력 유도 문구 + `FALLBACK`
- 프롬프트 응답이 비어 있거나 계약에 맞지 않으면 공감형 기본 답변 + `FALLBACK`
- backend-api는 이 응답을 받아도 대화 저장 흐름을 계속 유지한다

## 현재 테스트 포인트

- 프롬프트가 정상 JSON을 반환하면 그 값을 그대로 반영하는지
- 프롬프트 응답이 깨졌을 때 fallback으로 내려가는지
- 프롬프트 클라이언트가 없을 때도 fallback이 유지되는지
- 빈 입력에서 fallback 유도 문구를 주는지

# 16. 2026-03-25 프롬프트 품질 조정 메모

현재 `ai-api` 프롬프트는 비용과 품질을 함께 고려해 아래 방향으로 다듬었다.

## generate-reply
- 2~3문장 구조를 강하게 유도
- 1문장: 공감
- 2문장: 사용자 상황 반영
- 마지막 문장: 부담 적은 후속 질문 1개
- 기계적이거나 훈계조인 표현 금지

## analyze-diary
- summary는 감정 라벨만 반복하지 않도록 유도
- 감정 + 맥락/지속성/일상 영향 중 최소 하나를 포함하도록 유도
- 상담자가 빠르게 읽어도 이해되는 한 문장 요약을 목표로 조정

## risk-score
- `riskLevel`과 `riskScore` 범위를 더 일관되게 맞추도록 유도
- LOW / MEDIUM / HIGH의 수치 범위를 프롬프트에 직접 명시
- 과도한 고위험 분류를 피하되 안전 우선 원칙은 유지
