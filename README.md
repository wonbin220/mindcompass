# Mind Compass 백엔드 학습형 README (Spring AI 기준)

이 문서는 현재 레포에서 `ai-api`를 **Spring AI 기반 내부 AI 서버**로 운영하기 위한 메인 기준 문서다.

이 문서의 목적은 다음과 같다.

1. 왜 `Responsive Web + backend-api + ai-api + ai-api-fastapi` 구조로 가는지 이해한다.
2. `backend-api`, `ai-api`, `ai-api-fastapi`가 각각 무엇을 맡는지 구분한다.
3. Spring AI와 모델 서빙 계층을 함께 운영할 때 어떤 기준으로 구조를 유지해야 하는지 정리한다.
4. Codex가 Spring AI 관련 작업을 할 때 어떤 톤과 형식으로 답해야 하는지 고정한다.

## 로컬 실행 원칙

- 기본 로컬 개발은 `backend-api -> ai-api(dev)` 조합으로 맞춘다.
- 이 기본 조합에서는 `ai-api`가 OpenAI를 실제로 호출하지 않으므로 zero-cost 개발이 가능하다.
- 실제 OpenAI 품질 검증은 `ai-api(manual)`을 명시적으로 켠 뒤 필요한 시나리오만 짧게 확인한다.
- 자세한 실행 예시는 `docs/BACKEND_AI_LOCAL_RUN_GUIDE.md`와 `docs/ai-api/OPENAI_USAGE_AND_PROFILE_GUIDE.md`를 따른다.

---

# 1. 왜 문서를 이렇게 나눴는가

기존 `README.md`는 프로젝트를 시작할 때의 학습 기준이자,
`Spring Boot + FastAPI` 2서버 구조를 이해하기 위한 문서였다.

하지만 지금은 구조가 조금 달라졌다.

- `backend-api`는 그대로 Spring Boot 공개 API 서버다.
- `ai-api`는 새 Spring AI 기반 내부 AI 오케스트레이터 서버다.
- `ai-api-fastapi`는 감정분류 모델 서빙 계층으로 유지한다.

즉, FastAPI 설명을 완전히 지워버리기보다
**기존 문서는 보존하고, 현재 기준은 별도 문서로 정리하는 편이 더 안전하다.**

이렇게 하면:

- 예전 구조를 복기하기 쉽고
- Spring AI 전환 이유를 따로 설명할 수 있고
- FastAPI와 Spring AI를 나란히 비교할 수 있고
- Codex에게도 현재 기준과 비교 기준을 동시에 줄 수 있다.

---

# 2. 현재 구조를 한 문장으로 말하면

현재 구조는 아래처럼 이해하면 된다.

- 반응형 웹은 `backend-api`만 호출한다.
- `backend-api`는 공개 API, 인증, 저장, 비즈니스 흐름의 중심이다.
- `ai-api`는 Spring AI 기반 내부 AI 오케스트레이터다.
- `ai-api-fastapi`는 감정분류 모델 서빙 계층이다.
- `backend-api`는 `ai-api`만 내부적으로 호출하고, `ai-api`가 필요 시 `ai-api-fastapi`를 호출한다.

즉:

`Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`

---

# 3. 왜 Spring Boot + Spring AI 내부 서버 구조로 가는가

이 프로젝트의 핵심 구조는 다음과 같다.

- 반응형 웹은 **Spring Boot만 호출**
- Spring Boot는 **정본 데이터와 업무 로직의 중심**
- Spring AI 기반 `ai-api`는 **AI 오케스트레이션 담당**
- `ai-api-fastapi`는 **감정분류 모델 서빙 담당**
- Spring Boot는 `ai-api`만 내부적으로 호출한다

이렇게 가는 이유는 다음과 같다.

## 3-1. Spring Boot가 맡아야 하는 것

`backend-api`는 아래처럼 **안정적이고 비즈니스 중심인 영역**을 맡는다.

- 회원가입
- 로그인
- JWT 인증/인가
- 일기 저장/조회/수정/삭제
- 감정 캘린더 조회
- 상담 세션 관리
- 채팅 메시지 저장
- 리포트 저장/조회
- DB 트랜잭션 관리
- 외부 앱이 호출하는 공식 API

즉, Spring Boot는 **서비스의 중심 서버**다.

## 3-2. Spring AI 기반 ai-api가 맡아야 하는 것

`ai-api`는 아래처럼 **자주 바뀌고 실험이 많은 AI 영역**을 맡는다.

- 감정 분석
- 위험 신호 점수 계산
- 상담 답변 초안 생성
- 프롬프트 조립
- 모델 호출 전략 실험
- 추후 RAG, 메모리, 멀티모달 확장

즉, `ai-api`는 **Spring 생태계 안에서 프롬프트, 메모리, RAG, safety, fallback을 조합하는 내부 AI 오케스트레이터**다.

## 3-3. ai-api-fastapi는 왜 남겨두는가

`ai-api-fastapi`를 유지하는 이유는 단순하다.

- 감정분류 추론을 Python / PyTorch 스택에 더 자연스럽게 올릴 수 있다.
- 모델 버전, threshold, calibration, inference metadata를 별도 계층에서 관리할 수 있다.
- ai-api가 모델 엔진을 직접 품지 않아도 되므로 역할이 분명해진다.
- 필요 시 실험 라우팅이나 모델 교체를 `ai-api` 바깥에서 관리할 수 있다.

즉, `ai-api-fastapi`는 단순 비교 서버가 아니라
**감정분류 모델 서빙 계층** 역할이다.

## 3-4. 왜 앱이 AI 서버를 직접 부르면 안 되는가

반응형 웹이 `ai-api`나 `ai-api-fastapi`를 직접 호출하면 다음 문제가 생긴다.

- 인증 흐름이 분산된다.
- AI 서버가 외부에 직접 노출된다.
- 장애 포인트가 늘어난다.
- 앱에서 호출해야 할 API 진입점이 여러 개가 된다.
- 나중에 AI 구현체를 바꾸면 앱도 같이 수정해야 한다.

그래서 반응형 웹은 **무조건 `backend-api`만 호출**하게 만든다.

이렇게 해야 웹 클라이언트 입장에서는 진입점이 하나라서 단순하고,
내부 AI 구현체가 `Spring AI`에서 다른 형태로 바뀌어도 앱은 거의 영향을 받지 않는다.

---

# 4. 각 서버가 맡는 역할

## 4-1. backend-api

책임:
- 공개 API 진입점
- 인증 / 인가
- 사용자 기준 데이터 소유권
- Diary / Calendar / Chat / Report 저장과 조회
- `ai-api` 호출
- 화면 응답용 DTO 조립과 저장 흐름 유지

## 4-2. ai-api

책임:
- Spring AI 기반 내부 오케스트레이션
- `analyze-diary`, `risk-score`, `generate-reply` 제공
- 프롬프트 / 메모리 / RAG / safety / fallback 조합
- 필요 시 `ai-api-fastapi`, LLM Provider, RAG store 호출

## 4-3. ai-api-fastapi

책임:
- 감정분류 모델 추론
- 모델 버전 관리
- threshold / calibration
- inference metadata 반환

---

# 5. MVP 우선순위는 무엇인가

Spring AI 서버를 추가해도 MVP 우선순위 자체는 크게 바뀌지 않는다.

1. `backend-api` 안정성 유지
2. Auth / User
3. Diary CRUD
4. Calendar / emotion query
5. `ai-api` 최소 엔드포인트 안정화
6. Chat session / message 흐름
7. Safety Net
8. Report / statistics
9. 고도화 AI 비교 실험

중요한 점은:

- AI 서버를 바꾸는 실험보다
- 공개 API 안정성과 데이터 저장 안정성이 먼저다.

즉, Spring AI 실험은 중요하지만
**기록 저장과 안전 흐름을 깨지 않는 범위 안에서 진행해야 한다.**

---

# 6. 멘탈헬스 서비스에서 왜 Safety Net이 중요한가

이 서비스는 일반 챗봇이 아니라 멘탈헬스 관련 흐름을 다룬다.
그래서 답변 생성보다 먼저 봐야 하는 것이 **위험 신호 감지와 안전 분기**다.

Safety Net이 중요한 이유:

- 사용자가 고위험 표현을 남길 수 있다.
- 일반 공감 답변이 오히려 부적절할 수 있다.
- 모델 응답이 그럴듯해 보여도 안전하지 않을 수 있다.
- AI 실패보다 위험 분기 실패가 더 큰 문제다.

그래서 원칙은 다음과 같다.

- 위험 점수는 일반 답변 생성보다 우선한다.
- 고위험이면 `SAFETY_RESPONSE` 계열이 일반 답변보다 먼저 와야 한다.
- AI가 실패해도 저장 흐름은 가능하면 유지한다.
- 불확실할수록 더 보수적으로 분기한다.

한 줄로 요약하면:

**이 프로젝트에서 AI 품질보다 먼저 지켜야 하는 것은 안전 분기다.**

---

# 7. Spring AI 비교 실험을 할 때 지켜야 할 기준

FastAPI와 Spring AI를 비교할 때는 단순히 “둘 다 동작한다”로 보면 안 된다.

비교 기준은 최소한 아래를 같이 봐야 한다.

- 같은 요청/응답 계약을 유지하는가
- 같은 입력에서 비슷한 정책으로 응답하는가
- fallback 동작이 같은가
- 위험 신호 분기가 더 안정적인가
- 운영과 로그 확인이 더 쉬운가
- 테스트 작성과 유지보수가 더 쉬운가

즉, 비교는 프레임워크 취향 문제가 아니라
**구조 적합성, 안전성, 운영성, 유지보수성**을 같이 보는 작업이다.

---

# 8. Codex는 어떤 톤과 어떤 구조로 답해야 하는가

Spring AI 관련 작업에서도 Codex는 단순 코드 생성기로 행동하면 안 된다.

항상 다음을 같이 설명해야 한다.

1. 왜 이 API 또는 변경이 필요한가
2. 어떤 기능이나 화면이 간접적으로 쓰는가
3. 어떤 파일이 관련되는가
4. 요청이 어떤 순서로 실행되는가
5. fallback과 예외 상황이 무엇인가
6. FastAPI 비교 관점에서 무엇이 달라졌는가

답변 순서는 가능하면 아래를 따른다.

1. Goal
2. Design decision
3. Related files
4. Code skeleton
5. Execution flow explanation
6. Failure / fallback behavior
7. Next step

톤은 다음을 지킨다.

- 주니어 개발자도 따라올 수 있게 쉽게 설명한다.
- 왜 그렇게 설계했는지 이유를 같이 적는다.
- 멘탈헬스 도메인이라 안전 관련 판단을 먼저 짚는다.
- FastAPI와 비교할 때 장단점을 감추지 않는다.

---

# 9. 이 문서를 읽고 바로 이해해야 할 핵심 문장

- `backend-api`는 공개 API와 저장 흐름의 중심이다.
- `ai-api`는 Spring AI 기반 내부 AI 오케스트레이터다.
- `ai-api-fastapi`는 감정분류 모델 서빙 계층이다.
- 반응형 웹은 AI 서버를 직접 호출하면 안 된다.
- Safety Net은 일반 답변 생성보다 우선한다.
- Spring AI 실험은 서비스 안정성을 깨지 않는 범위에서 진행해야 한다.

---

# 10. 다음 추천 작업

현재 이 문서 기준으로 바로 이어서 하기 좋은 작업은 다음과 같다.

1. `backend-api`에서 `ai-api` / `ai-api-fastapi`를 설정값으로 전환 가능하게 만들기
2. 세 내부 엔드포인트의 요청/응답 계약을 두 서버에서 완전히 동일하게 맞추기
3. 같은 샘플 입력으로 응답 품질/속도/fallback 비교표 만들기
4. Spring AI 실제 모델 호출과 더미 fallback 응답을 분리하기
5. Safety 분기 테스트를 두 서버에서 공통으로 돌릴 수 있게 만들기
