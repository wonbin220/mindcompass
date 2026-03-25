# Risk Score API 학습 문서

이 문서는 ai-api의 `위험 신호 스코어링` 내부 API를 학습하기 위한 문서다.

---

# 1. 이 API가 왜 필요한가

멘탈헬스 서비스에서 가장 중요한 것 중 하나는
사용자의 발화가 일반적인 고민인지,
아니면 즉시 더 안전하게 다뤄야 하는 위험 신호인지 구분하는 것이다.

예:
- 자해 암시
- 극단적 사고 표현
- 즉각적인 위기 정황
- 고립감/절망감의 급격한 상승

그래서 `risk-score` API가 필요하다.

이 API는 다음을 돕는다.
- 일반 응답과 safety 응답 분기
- 위기 상황 로그 저장
- 안전 안내 리소스 노출
- 고위험 대화의 후속 처리

---

# 2. 엔드포인트

`POST /internal/ai/risk-score`

---

# 3. 어떤 화면/기능에서 간접적으로 쓰이는가

- AI 상담 채팅 메시지 전송
- 일기 저장 후 고위험 감정 탐지
- safety net 작동 여부 판단
- 위기 상황 대응 UX 분기

사용자는 “risk-score API”를 직접 보지 않지만,
실제론 안전한 서비스 운영의 핵심이다.

---

# 4. 요청 예시

```json
{
  "userId": 1,
  "sessionId": 501,
  "text": "아무도 나를 이해하지 못하고 다 끝내고 싶다는 생각이 들어요.",
  "sourceType": "CHAT_MESSAGE"
}
```

---

# 5. 응답 예시

```json
{
  "riskLevel": "HIGH",
  "riskScore": 0.93,
  "signals": ["SELF_HARM_IMPLICIT", "HOPELESSNESS", "ISOLATION"],
  "recommendedAction": "SAFETY_RESPONSE"
}
```

---

# 6. 관련 파일 예시

## FastAPI
- `app/routers/safety_router.py`
- `app/schemas/risk_score.py`
- `app/services/risk_scoring_service.py`
- `app/services/prompt_builder_service.py`
- `app/clients/openai_client.py`

## Spring Boot 측 호출부
- `backend-api/.../client/AiSafetyClient.java`
- `backend-api/.../service/ChatService.java`
- `backend-api/.../service/SafetyService.java`

---

# 7. FastAPI 안에서 실행 순서

1. Spring Boot가 `/internal/ai/risk-score`를 호출한다.
2. `safety_router.py`가 요청을 받는다.
3. 요청 body가 `RiskScoreRequest` 스키마로 변환된다.
4. router가 `risk_scoring_service.score_risk()`를 호출한다.
5. service가 텍스트 전처리 및 위험 키워드/패턴을 확인한다.
6. 필요하면 LLM 기반 분류 또는 규칙 기반 점수를 계산한다.
7. 위험 신호 라벨을 정리한다.
8. 최종 riskLevel / riskScore / signals를 만든다.
9. `RiskScoreResponse` 스키마로 응답을 생성한다.
10. Spring Boot가 결과를 받아 safety 분기 여부를 결정한다.

---

# 8. 파일별 역할

## safety_router.py
위험 점수 요청 접수

## risk_score.py
위험 점수 요청/응답 스키마

## risk_scoring_service.py
핵심 분류 로직
- 규칙 기반
- 모델 기반
- 최종 점수 조합

## prompt_builder_service.py
LLM 분류 프롬프트 구성

## openai_client.py
외부 모델 호출

---

# 9. 왜 risk-score를 별도 API로 두는가

답변 생성과 위험 판단을 한 API에 합칠 수도 있다.
하지만 분리하면 장점이 있다.

- 테스트가 쉽다
- safety만 따로 고도화 가능하다
- chat 외 diary에도 재사용 가능하다
- 응답 생성 전에 위험 여부를 선판단할 수 있다

즉:
- risk-score = 안전 판단
- generate-reply = 답변 생성

이렇게 나누면 설계가 더 명확해진다.

---

# 10. 예외 상황

- 문맥 없이 짧은 문장이라 오판 가능성
- 농담/비유 표현 오인식
- LLM 점수와 규칙 기반 점수 충돌
- 타임아웃으로 점수 미산출

이 경우 대응 전략:
- 규칙 기반 fallback
- 보수적인 safety 정책
- uncertainty 플래그 추가

---

# 11. 학습 포인트

- 이 API는 “좋은 답변”보다 “안전한 분기”를 위한 API다.
- 멘탈헬스 서비스에서는 생성 품질보다 safety 우선 판단이 더 중요할 수 있다.
- 규칙 기반 + LLM 기반 혼합 구조로 가면 운영이 더 안정적일 수 있다.

---

# 12. 현재 MVP 구현 메모

현재 구현은 규칙 기반 MVP다.

- ai-api `POST /internal/ai/risk-score`를 추가했다.
- `risk_scoring_service.py`에서 고위험/중위험 키워드를 기준으로 `LOW`, `MEDIUM`, `HIGH`를 분류한다.
- 문자열 완전일치가 아니라 정규식 기반으로 바꿔 `없고`, `힘들어요`, `버티기 힘들어` 같은 어미 변화도 일부 허용한다.
- Spring Boot `ChatService`는 `generate-reply` 전에 `risk-score`를 먼저 호출한다.
- 응답이 `HIGH` + `SAFETY_RESPONSE`면 일반 답변 생성 대신 safety 안내 문구를 assistant 메시지로 저장한다.
- 응답이 `MEDIUM` + `SUPPORTIVE_RESPONSE`면 일반 답변 생성 대신 지원형 안내 문구를 assistant 메시지로 저장한다.

즉 현재 흐름은:

1. 사용자가 Chat 메시지를 보낸다.
2. Spring Boot가 ai-api `risk-score`를 먼저 호출한다.
3. HIGH면 safety 응답으로 즉시 분기한다.
4. MEDIUM이면 supportive 응답으로 즉시 분기한다.
5. HIGH/MEDIUM이 아니면 기존 `generate-reply`를 호출한다.

이 구현은 최소 MVP 기준이며, 추후에는 아래를 붙일 수 있다.

- diary 저장 시 risk-score 재사용
- safety event DB 저장
- 지역별 도움 리소스 연결
- 규칙 기반 + LLM 기반 혼합 점수화

---

# 13. 실호출 확인 메모

실호출 기준으로 아래를 확인했다.

- ai-api에 고위험 문장 예시
  - `"다 끝내고 싶고 사라지고 싶어요."`
- `POST /internal/ai/risk-score` 응답
  - `riskLevel=HIGH`
  - `recommendedAction=SAFETY_RESPONSE`
- Spring Boot Chat 메시지 전송 응답/저장
  - 일반 공감 답변 대신 safety 안내 문구 사용
  - `responseType=SAFETY` 기준으로 해석 가능
  - 세션 상세 조회에서 assistant 메시지로 누적 저장 확인
- ai-api에 중위험 문장 예시
  - `"아무도 없고 너무 힘들어서 버티기 힘들어요."`
- `POST /internal/ai/risk-score` 응답
  - `riskLevel=MEDIUM`
  - `recommendedAction=SUPPORTIVE_RESPONSE`
- Spring Boot Chat 메시지 전송 응답/저장
  - 일반 공감 답변 대신 지원형 안내 문구 사용
  - `responseType=SUPPORTIVE` 기준으로 해석 가능
  - 세션 상세 조회에서 assistant 메시지로 누적 저장 확인
- 일반 문장 예시
  - `"오늘은 좀 불안했지만 그래도 버틸 수 있었어요."`
- Spring Boot Chat 메시지 전송 응답/저장
  - `responseType=NORMAL`
  - 일반 공감 답변 유지 확인
- Diary 중위험 본문 예시
  - `"아무도 없고 너무 힘들어서 버티기 힘들어요."`
- Diary 상세 조회 응답
  - `riskLevel=MEDIUM`
  - `recommendedAction=SUPPORTIVE_RESPONSE`
- Diary 고위험 본문 예시
  - `"다 끝내고 싶고 사라지고 싶어요."`
- Diary 상세 조회 응답
  - `riskLevel=HIGH`
  - `recommendedAction=SAFETY_RESPONSE`

즉 현재 MVP Safety Net은
`위험 감지 -> chat safety/supportive 분기 -> diary 위험도 저장`
까지 확인된 상태다.

---

# 14. 실패 시 운영 가이드

## 실패 징후
- 고위험 문장을 보내도 `responseType=SAFETY`가 아니라 일반 reply 또는 fallback이 내려온다.
- `POST /internal/ai/risk-score` 직접 호출에서 기대한 `HIGH`가 나오지 않는다.
- 서버 로그에 Chat AI 오케스트레이션 경고만 남고 safety 분기 기록이 보이지 않는다.

## backend-api 영향
- risk-score 실패 시 현재 구현은 전체 AI 오케스트레이션을 fallback으로 처리할 수 있다.
- 즉 고위험 분기보다 보수적으로 fallback reply가 나올 수 있다.

## 1차 확인 순서
1. ai-api `POST /internal/ai/risk-score` 직접 호출
2. 고위험 문장 예시로 `riskLevel=HIGH`, `recommendedAction=SAFETY_RESPONSE` 확인
3. backend-api의 `AI_API_BASE_URL`과 ai-api 포트 확인
4. Chat 메시지 전송 후 `responseType=SAFETY` 또는 assistant safety 문구 저장 여부 확인

## 운영 메모
- 현재 MVP는 규칙 기반 분류라 문장 표현이 바뀌면 탐지 민감도가 달라질 수 있다.
- Diary도 `sourceType=DIARY`로 같은 risk-score를 재사용하고, 저장 후 위험도 결과만 후처리로 반영한다.
- 추후에는 safety event 저장, 위험도 기반 리포트 집계가 추가로 필요하다.

## MEDIUM 테스트 문장 예시
- `"아무도 없고 너무 힘들어서 버티기 힘들어요."`
- `"혼자라서 너무 힘들고 버티기 힘들어요."`

---

# 15. Spring AI 비교 서버 기준 현재 구조

현재 `ai-api`의 `risk-score`는 단순 규칙 기반만 쓰지 않고,
아래 순서로 동작하도록 정리했다.

1. Spring Boot가 `POST /internal/ai/risk-score`를 호출한다.
2. `SafetyAiController`가 요청을 받는다.
3. `RiskScoreService`가 먼저 Spring AI 프롬프트 분류를 시도한다.
4. 모델 응답이 계약에 맞는 JSON이면 그 값을 그대로 사용한다.
5. 모델 응답이 비어 있거나 JSON 파싱에 실패하면 규칙 기반 fallback으로 내려간다.
6. 최종적으로 `RiskScoreResponse`를 Spring Boot에 반환한다.

즉 현재 구조는:

- 1차: Spring AI 프롬프트 시도
- 2차: 구조화 JSON 파싱
- 실패 시: 안전한 규칙 기반 fallback

이렇게 되어 있다.

## 왜 이렇게 바꿨는가

비교 목적상 `ai-api`도 실제 모델 시도를 해봐야
나중에 `ai-api-fastapi`와 응답 품질, 유지보수성, fallback 안정성을 비교할 수 있다.

하지만 멘탈헬스 서비스에서는 위험 분류 실패가 치명적일 수 있으므로,
프롬프트 실패 때문에 전체 흐름이 깨지지 않도록 fallback을 반드시 유지한다.

## 현재 코드 기준 역할

- `SafetyAiController`
  - 내부 `/risk-score` 엔드포인트 진입점
- `RiskScoreService`
  - 프롬프트 시도
  - JSON 파싱
  - fallback 분기
  - 최종 응답 생성
- `RiskScorePromptClient`
  - 프롬프트 호출 인터페이스
- `SpringRiskScorePromptClient`
  - Spring AI `ChatClient` 기반 실제 호출 구현

## 현재 fallback 원칙

- 입력 텍스트가 비어 있으면 `LOW` / `NORMAL_RESPONSE`
- 프롬프트 응답이 비어 있거나 계약에 맞지 않으면 규칙 기반 분류
- 규칙 기반에서도
  - 고위험 표현은 `HIGH` / `SAFETY_RESPONSE`
  - 중위험 표현은 `MEDIUM` / `SUPPORTIVE_RESPONSE`
  - 그 외는 `LOW` / `NORMAL_RESPONSE`

## 현재 테스트 포인트

- 프롬프트가 정상 JSON을 반환하면 그 값을 그대로 반영하는지
- 프롬프트 응답이 깨졌을 때 fallback으로 내려가는지
- 프롬프트 클라이언트가 없을 때도 fallback이 유지되는지
- 빈 입력에서 낮은 위험 기본값을 주는지
