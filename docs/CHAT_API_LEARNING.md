# Chat API 학습 문서

이 문서는 `Chat` 도메인을 학습하기 위한 문서다.  
Chat 도메인은 이 프로젝트에서 가장 흐름이 복잡한 도메인 중 하나다.  
왜냐하면 단순 CRUD를 넘어서 **세션 관리 + 메시지 저장 + 내부 AI 호출 + 안전 분기**가 함께 일어날 수 있기 때문이다.

---

# 1. Chat 도메인이 왜 중요한가

이 서비스는 단순 일기 앱이 아니라,  
일기 기록 후 사용자가 AI와 대화를 이어갈 수 있는 상담형 앱을 목표로 한다.

즉 Chat 도메인은:
- 사용자의 질문을 저장하고
- 대화 맥락을 유지하고
- AI 답변을 생성하고
- 안전한 응답을 분기하는
핵심 역할을 담당한다.

---

# 2. Chat에서 다루는 핵심 개념

## 2-1. Chat Session
하나의 상담 대화방 같은 개념이다.

예:
- “오늘 감정 상담”
- “불안 관련 대화”
- “잠들기 전 기록 대화”

## 2-2. Chat Message
세션 안에서 오가는 실제 메시지다.

- 사용자 메시지
- AI 메시지
- 시스템 메시지(선택)

## 2-3. AI Response Log
AI가 어떤 입력을 받고 어떤 응답을 냈는지 로그를 남기는 구조다.  
운영/디버깅/품질 개선에 도움이 된다.

## 2-4. Safety Net
위험 키워드나 위험 맥락이 감지되면 일반 공감 응답 대신  
안전 안내를 우선해야 한다.

---

# 3. Chat 패키지 예시 구조

```text
chat/
├─ controller/
│  └─ ChatController.java
├─ service/
│  └─ ChatService.java
├─ repository/
│  ├─ ChatSessionRepository.java
│  ├─ ChatMessageRepository.java
│  └─ AiResponseLogRepository.java
├─ domain/
│  ├─ ChatSession.java
│  ├─ ChatMessage.java
│  └─ AiResponseLog.java
├─ client/
│  └─ AiChatClient.java
└─ dto/
   ├─ request/
   │  ├─ CreateChatSessionRequest.java
   │  └─ SendChatMessageRequest.java
   └─ response/
      ├─ ChatSessionResponse.java
      ├─ ChatSessionListResponse.java
      ├─ ChatDetailResponse.java
      └─ SendChatMessageResponse.java
```

FastAPI 예시 구조:

```text
ai-api/app/
├─ routers/
│  └─ chat_router.py
├─ services/
│  ├─ reply_generation_service.py
│  └─ risk_scoring_service.py
└─ schemas/
   ├─ generate_reply_request.py
   └─ generate_reply_response.py
```

---

# 4. Chat 도메인을 이해할 때 가장 중요한 관점

Chat은 다른 도메인과 달리 “단일 저장”이 아니라 **흐름 설계**가 중요하다.

질문:
- 세션이 없으면 먼저 세션을 만들까?
- 사용자 메시지는 AI 호출 전에 저장할까, 후에 저장할까?
- AI가 실패하면 사용자 메시지는 남길까?
- 위험 신호가 보이면 AI 답변 대신 safety 응답을 줄까?
- 이전 대화 맥락은 어디까지 보낼까?

이런 판단을 주로 `ChatService`가 담당한다.

즉:
- Chat에서는 Controller보다 Service 이해가 특히 중요하다.

---

# 5. API 1 - 상담 세션 생성

## 5-1. 엔드포인트
`POST /api/v1/chat/sessions`

## 5-2. 왜 필요한가
사용자와 AI의 대화를 하나의 단위로 묶기 위해 필요하다.  
세션이 있어야 메시지들이 어떤 대화에 속하는지 구분할 수 있다.

## 5-3. 어느 화면에서 쓰는가
- AI 상담 시작 버튼
- 일기 저장 후 “AI 상담으로 이어가기” 버튼

## 5-4. 요청 예시
```json
{
  "title": "오늘 감정 상담",
  "sourceDiaryId": 101
}
```

## 5-5. 응답 예시
```json
{
  "sessionId": 501,
  "title": "오늘 감정 상담",
  "createdAt": "2026-03-18T22:00:00"
}
```

## 5-6. 관련 파일
- `ChatController.java`
- `ChatService.java`
- `ChatSessionRepository.java`
- `ChatSession.java`
- `CreateChatSessionRequest.java`
- `ChatSessionResponse.java`

## 5-7. 실행 순서
1. 앱이 새 상담 세션 생성 요청을 보낸다.
2. JWT 필터가 인증을 확인한다.
3. `ChatController.createSession()`이 요청을 받는다.
4. `CreateChatSessionRequest`로 body가 변환된다.
5. `ChatService.createSession(userId, request)`를 호출한다.
6. Service가 제목, 연결 diaryId 등을 검증한다.
7. `ChatSession` 엔티티를 생성한다.
8. `ChatSessionRepository.save()`로 저장한다.
9. 응답 DTO를 만든다.
10. Controller가 응답을 반환한다.

## 5-8. 학습 포인트
Chat의 시작점은 메시지가 아니라 **세션**일 수 있다.  
세션이 있어야 이후 메시지 흐름을 모을 수 있다.

---

# 6. API 2 - 상담 세션 목록 조회

## 6-1. 엔드포인트
`GET /api/v1/chat/sessions`

## 6-2. 왜 필요한가
사용자가 이전에 어떤 상담을 했는지 목록으로 보여주기 위해 필요하다.

## 6-3. 어느 화면에서 쓰는가
- 상담 히스토리 화면
- 최근 대화 목록

## 6-4. 관련 파일
- `ChatController.java`
- `ChatService.java`
- `ChatSessionRepository.java`
- `ChatSessionListResponse.java`

## 6-5. 실행 순서
1. 앱이 세션 목록 조회 요청을 보낸다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 현재 사용자 ID를 가져온다.
4. `ChatService.getSessions(userId)`를 호출한다.
5. Repository가 해당 사용자의 세션 목록을 조회한다.
6. Service가 최근순 정렬, 요약 메시지 포함 여부를 가공한다.
7. 응답 DTO를 만든다.
8. Controller가 반환한다.

---

# 7. API 3 - 상담 세션 상세 조회

## 7-1. 엔드포인트
`GET /api/v1/chat/sessions/{sessionId}`

## 7-2. 왜 필요한가
사용자가 특정 상담방에 들어가 이전 대화 내용을 이어서 보기 위해 필요하다.

## 7-3. 어느 화면에서 쓰는가
- 채팅방 상세 화면

## 7-4. 관련 파일
- `ChatController.java`
- `ChatService.java`
- `ChatSessionRepository.java`
- `ChatMessageRepository.java`
- `ChatDetailResponse.java`

## 7-5. 실행 순서
1. 앱이 sessionId를 path variable로 요청한다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 현재 사용자 ID와 sessionId를 받는다.
4. `ChatService.getSessionDetail(userId, sessionId)`를 호출한다.
5. Repository가 세션 존재 여부를 확인한다.
6. Repository가 해당 세션의 메시지 목록을 조회한다.
7. Service가 메시지를 시간순으로 정리한다.
8. `ChatDetailResponse`를 만든다.
9. Controller가 응답을 반환한다.

## 7-6. 학습 포인트
세션 상세 조회는 “세션 1개”만 보는 게 아니라  
그 안의 메시지 목록까지 포함하는 경우가 많다.

---

# 8. API 4 - 채팅 메시지 전송 (핵심)

## 8-1. 엔드포인트
`POST /api/v1/chat/sessions/{sessionId}/messages`

## 8-2. 왜 필요한가
이 API가 실제 AI 상담의 중심이다.  
사용자가 메시지를 보내면 저장하고, 필요하면 AI 답변을 생성해서 다시 반환해야 한다.

## 8-3. 어느 화면에서 쓰는가
- AI 상담 채팅방

## 8-4. 요청 예시
```json
{
  "message": "오늘 하루가 너무 불안했고 잠도 잘 못 잘 것 같아요."
}
```

## 8-5. 응답 예시
```json
{
  "userMessageId": 1001,
  "assistantMessageId": 1002,
  "assistantReply": "오늘 많이 불안하셨겠어요. 지금 가장 크게 느껴지는 불안이 어떤 상황과 연결되는지 함께 정리해볼까요?"
}
```

## 8-6. 관련 파일 (Spring Boot)
- `ChatController.java`
- `ChatService.java`
- `ChatSessionRepository.java`
- `ChatMessageRepository.java`
- `AiResponseLogRepository.java`
- `AiChatClient.java`
- `SendChatMessageRequest.java`
- `SendChatMessageResponse.java`

## 8-7. 관련 파일 (FastAPI)
- `chat_router.py`
- `reply_generation_service.py`
- `risk_scoring_service.py`
- `generate_reply_request.py`
- `generate_reply_response.py`

## 8-8. 실행 순서
1. 앱이 sessionId와 사용자 메시지를 담아 요청한다.
2. JWT 필터가 인증을 확인한다.
3. `ChatController.sendMessage()`가 요청을 받는다.
4. body가 `SendChatMessageRequest` DTO로 변환된다.
5. Controller가 `ChatService.sendMessage(userId, sessionId, request)`를 호출한다.
6. Service가 세션 존재 여부와 소유권을 확인한다.
7. 사용자 메시지를 `ChatMessage` 엔티티로 먼저 저장한다.
8. 최근 메시지 목록을 조회해 AI 입력 문맥을 만든다.
9. 필요하면 source diary, 감정 정보, 사용자 설정도 같이 조회한다.
10. `AiChatClient`가 FastAPI 내부 API를 호출한다.
11. FastAPI의 router가 요청을 받는다.
12. FastAPI service가 감정 분석/안전 조건/프롬프트 조합을 수행한다.
13. 위험 신호가 높으면 일반 답변 대신 safety 응답을 만들 수 있다.
14. FastAPI가 응답을 반환한다.
15. Spring Boot의 `ChatService`가 AI 답변을 `ChatMessage` 또는 `AiResponseLog`에 저장한다.
16. `SendChatMessageResponse`를 만든다.
17. Controller가 최종 응답을 앱에 반환한다.

## 8-9. DB 영향
- `chat_message` insert (사용자 메시지)
- `chat_message` insert (AI 응답)
- `ai_response_log` insert
- 필요 시 `safety_event` insert

## 8-10. 예외 상황
- 존재하지 않는 sessionId
- 다른 사용자의 세션 접근
- FastAPI 타임아웃
- AI 응답 실패
- 고위험 발화 감지

## 8-11. 학습 포인트
이 API는 다른 CRUD와 달리:
- 저장
- 조회
- 내부 API 호출
- 안전 분기
- 응답 저장

이 한 흐름에 다 들어간다.

즉, Chat API를 이해하면  
**왜 Service가 흐름의 중심인지**를 가장 잘 알 수 있다.

---

# 9. AI 실패 시 어떻게 생각해야 하는가

Chat API에서 중요한 운영 질문이 있다.

**AI가 실패하면 사용자 메시지는 저장할까?**

대부분은 저장하는 편이 좋다.  
왜냐하면 사용자는 이미 메시지를 보냈고, 그 기록 자체는 유효하기 때문이다.

가능한 응답 전략:
- 사용자 메시지는 저장
- AI 응답은 실패 메시지 또는 재시도 안내로 반환
- 내부 로그는 남김

현재 구현 기준:
- Spring Boot `ChatService`는 사용자 메시지를 먼저 저장한다.
- ai-api `generate-reply` 호출이 실패하면 assistant 메시지는 fallback 문구로 저장한다.
- 응답 DTO의 `responseType`은 `FALLBACK`으로 내려간다.
- 서버 로그에는 `Chat AI reply generation failed` 경고 로그를 남긴다.

이렇게 해야:
- 대화 기록이 끊기지 않고
- 장애 분석이 가능하며
- 서비스 신뢰성이 올라간다

---

# 10. Safety Net이 Chat에 왜 중요한가

멘탈헬스 도메인에서는 모든 입력을 일반 대화처럼 처리하면 안 된다.

예:
- 자해 암시
- 극단적 사고 표현
- 즉각적 위험 상황 암시

이런 경우 ChatService나 FastAPI 서비스에서:
- 위험 점수를 계산하고
- 일반 공감 답변 대신
- 긴급 안내 / 안전 가이드 / 도움 요청 유도 문구를 우선해야 한다.

즉:
Chat 도메인은 단순 메시징 기능이 아니라  
**안전한 대화 흐름 관리 기능**이다.

현재 MVP 구현 기준:
- Chat 메시지를 받으면 Spring Boot가 ai-api `risk-score`를 먼저 호출한다.
- `riskLevel=HIGH`, `recommendedAction=SAFETY_RESPONSE`면 일반 reply 생성 대신 safety 문구로 바로 응답한다.
- `riskLevel=MEDIUM`, `recommendedAction=SUPPORTIVE_RESPONSE`면 일반 reply 생성 대신 더 보수적인 지원형 문구로 응답한다.
- 이 safety 문구도 일반 assistant 메시지처럼 세션 상세 조회에 저장된다.
- 이 지원형 문구도 일반 assistant 메시지처럼 세션 상세 조회에 저장된다.
- 실호출 기준으로 고위험 문장에서 safety assistant 메시지 누적 저장까지 확인했다.
- 실호출 기준으로 중위험 문장에서 supportive assistant 메시지 누적 저장까지 확인했다.
- 일반 문장에서는 `responseType=NORMAL`과 일반 공감 답변 유지까지 확인했다.

---

# 11. Chat 전체 흐름 한 번에 보기

## 세션 생성
앱 → JWT 필터 → Controller → Service → SessionRepository.save → DB → 앱

## 세션 상세 조회
앱 → JWT 필터 → Controller → Service → SessionRepository / MessageRepository → 앱

## 메시지 전송
앱 → JWT 필터 → Controller → Service → MessageRepository.save(user)  
→ AiChatClient → FastAPI  
→ FastAPI router → FastAPI service  
→ Spring Boot Service 복귀  
→ MessageRepository.save(ai) / AiResponseLogRepository.save  
→ Response DTO → 앱

---

# 12. Chat 도메인을 이해할 때 꼭 기억할 문장

- Chat의 시작은 메시지일 수도 있지만, 구조적으로는 세션이 중요하다.
- Chat API는 CRUD가 아니라 흐름 설계에 가깝다.
- 사용자 메시지는 가능하면 먼저 저장하는 편이 운영상 유리하다.
- AI 실패와 Safety 분기를 항상 고려해야 한다.
- ChatService는 저장, AI 호출, 안전 응답을 조율하는 지휘자다.
