# CODEX_RESTART_PROMPT

이 문서는 `mind-compass` 프로젝트를 **처음부터 다시 시작할 때 Codex에 바로 붙여넣어 사용할 수 있는 한글 프롬프트**입니다.

이번 업데이트는 업로드된 학습형 README 문서의 핵심 원칙을 명시적으로 반영했습니다.

반영한 핵심 원칙:
- 코드를 만들 때 **왜 그렇게 만드는지도 같이 남긴다**
- API를 만들 때마다 **실행 흐름을 문서화한다**
- **Controller / Service / Repository / Entity / DTO 역할을 구분해서 설명한다**
- **Spring Boot가 외부 요청의 진입점**이라는 사실을 계속 유지한다
- **AI 기능보다 먼저 기록, 저장, 조회, 인증, 안전성**을 우선한다
- 시스템 프롬프트 문서와 체크리스트 문서는 역할이 다르며, Codex는 그 둘을 함께 참고해야 한다

---

## 1. 권장 사용 상황

이 프롬프트는 아래 상황에서 사용하면 좋습니다.

- Codex에서 새 채팅을 시작할 때
- 프로젝트를 처음부터 다시 정리하고 싶을 때
- 전체 구조를 다시 읽히고 싶을 때
- 너무 큰 범위로 요청해서 Codex가 멈추거나 흔들렸던 경험이 있을 때
- MVP 기준으로 다시 차근차근 시작하고 싶을 때
- 코드만 생성하지 말고, 내가 **이유와 흐름까지 학습할 수 있게** 시작하고 싶을 때

---

## 2. 전제 조건

이 프롬프트는 아래 파일들이 프로젝트 안에 있다고 가정합니다.

### 루트/하위 AGENTS
- `AGENTS.md`
- `backend-api/AGENTS.md`
- `ai-api/AGENTS.md`

### 학습 문서
- `README.md`
- `docs/README.md`
- `docs/AUTH_API_LEARNING.md`
- `docs/DIARY_API_LEARNING.md`
- `docs/CALENDAR_API_LEARNING.md`
- `docs/CHAT_API_LEARNING.md`
- `docs/ai-api/README.md`
- `docs/ai-api/AI_API_OVERVIEW_LEARNING.md`
- `docs/ai-api/ANALYZE_DIARY_API_LEARNING.md`
- `docs/ai-api/RISK_SCORE_API_LEARNING.md`
- `docs/ai-api/GENERATE_REPLY_API_LEARNING.md`
- `docs/ai-api/RAG_CONTEXT_API_LEARNING.md`

### Skills
- `.agents/skills/create-spring-api-doc/SKILL.md`
- `.agents/skills/create-fastapi-internal-endpoint/SKILL.md`
- `.agents/skills/generate-erd-and-sql/SKILL.md`

---

## 3. Codex에 바로 붙여넣을 한글 프롬프트 (긴 버전)

```txt
변경을 시작하기 전에 저장소 지침을 먼저 읽고 반드시 따르세요.

먼저 읽을 파일:
- AGENTS.md
- backend-api 작업이면 backend-api/AGENTS.md
- ai-api 작업이면 ai-api/AGENTS.md
- README.md
- docs/README.md
- docs/AUTH_API_LEARNING.md
- docs/DIARY_API_LEARNING.md
- docs/CALENDAR_API_LEARNING.md
- docs/CHAT_API_LEARNING.md
- docs/ai-api/README.md
- docs/ai-api/AI_API_OVERVIEW_LEARNING.md
- docs/ai-api/ANALYZE_DIARY_API_LEARNING.md
- docs/ai-api/RISK_SCORE_API_LEARNING.md
- docs/ai-api/GENERATE_REPLY_API_LEARNING.md
- docs/ai-api/RAG_CONTEXT_API_LEARNING.md

관련되면 아래 Skills를 사용하세요:
- create-spring-api-doc
- create-fastapi-internal-endpoint
- generate-erd-and-sql

프로젝트 컨텍스트:
이 프로젝트 이름은 `mind-compass`입니다.
구조는 2서버 아키텍처입니다.
- backend-api = Spring Boot 공개 API 서버
- ai-api = FastAPI 내부 AI 추론 서버

핵심 아키텍처 원칙:
- 모바일 앱은 Spring Boot만 호출합니다
- Spring Boot가 유일한 공개 API 진입점입니다
- Spring Boot는 필요할 때 ai-api를 내부적으로 호출합니다
- 모바일 앱이 ai-api를 직접 의존하게 만들지 마세요

서비스 컨텍스트:
Mind Compass는 AI 감정일기 + 상담 서비스입니다.

MVP 우선순위:
1. Spring Boot 기본 뼈대
2. Auth / user
3. Diary CRUD
4. Calendar / 감정 조회
5. 최소 ai-api 엔드포인트
6. Chat 세션 / 메시지
7. Safety Net
8. Reports / 통계
9. 고도화 AI 기능

중요 작업 원칙:
- 너무 이른 복잡화보다 단순하고 확장 가능한 MVP 설계를 우선하세요
- 저장, 조회, 안전성이 고도화 AI보다 먼저입니다
- AI 장애가 전체 서비스 장애로 번지면 안 됩니다
- 멘탈헬스 도메인이므로 safety-first 관점을 유지하세요
- 설명은 주니어 백엔드 개발자도 이해할 수 있게 작성하세요
- 코드만 만들지 말고, 왜 그렇게 만드는지도 같이 설명하세요
- API를 만들 때마다 실행 흐름을 문서화하세요
- Controller / Service / Repository / Entity / DTO 또는 Router / Schema / Service / Client 역할을 구분해서 설명하세요
- 모든 API나 모듈에 대해 아래를 설명하세요:
  1. 왜 필요한지
  2. 어떤 화면/기능이 사용하는지
  3. 관련 파일
  4. 실행 순서
  5. DB 영향
  6. 예외 상황
  7. AI가 관련되면 fallback 동작

작업:
이 프로젝트를 처음부터 다시 시작합니다.
한 번에 전체 저장소를 구현하려고 하지 마세요.

1단계:
MVP를 처음부터 구현하기 위한 깔끔한 작업 계획을 세우세요.
작업 계획은 작고 검토 가능한 단위로 나누세요.

2단계:
그 계획을 바탕으로 아래의 초기 폴더/패키지 구조를 제안하세요.
- backend-api
- ai-api

3단계:
첫 번째 구현 배치만 제안하세요.
범위:
- Spring Boot 기본 뼈대
- Auth/user 기본 구조
- Diary 기본 CRUD 기반

4단계:
첫 번째 구현 배치에 대해 아래를 제공하세요.
- 관련 파일
- 패키지 구조
- 코드 골격만
- 실행 흐름 설명
- 관련 DB 테이블
- 예외 상황
- 이 설계를 왜 이렇게 나누는지에 대한 설명

5단계:
지금 단계에서는 Chat, Safety, 고급 RAG 구현까지 바로 진행하지 마세요.
필요하면 구조 수준에서만 언급하세요.

출력 형식:
1. 목표
2. 설계 판단
3. MVP 구현 계획
4. 관련 파일
5. 코드 골격
6. 실행 흐름 설명
7. DB 영향
8. 예외 / fallback 동작
9. 다음 단계

Reasoning effort:
계획 수립과 기본 구현 구조는 medium 수준으로 진행하세요.
만약 cross-layer 설계 문제나 safety-critical 흐름이 보이면 그 부분만 내부적으로 high 수준으로 더 깊게 검토하세요.
```

---

## 4. Codex에 바로 붙여넣을 한글 프롬프트 (짧은 버전)

```txt
먼저 AGENTS.md, README.md, docs/README.md, 그리고 관련 도메인 문서를 읽고 따르세요.
관련되면 repository skills도 사용하세요.

우리는 `mind-compass` 프로젝트를 처음부터 다시 시작합니다.

2서버 아키텍처를 반드시 따르세요:
- backend-api = Spring Boot 공개 API
- ai-api = FastAPI 내부 AI 서비스
- 모바일 앱 -> Spring Boot만 호출

처음부터 전체 구현을 한 번에 하지 마세요.
MVP 기준으로 작은 단위로 나누어 시작하세요.

먼저:
- 단계별 MVP 계획을 세우고
- backend-api / ai-api 초기 패키지 구조를 제안하고
- 첫 구현 배치로 아래만 다루세요:
  - Spring Boot 기본 뼈대
  - Auth/user 기본 구조
  - Diary 기본 CRUD 기반

각 API/모듈에 대해 아래를 설명하세요:
- 왜 필요한지
- 어떤 화면/기능에서 쓰는지
- 관련 파일
- 실행 흐름
- DB 영향
- 예외 상황
- AI 관련이면 fallback

코드만 만들지 말고, 내가 학습할 수 있도록 이유와 실행 순서도 함께 설명하세요.

출력 형식:
1. 목표
2. 설계 판단
3. MVP 계획
4. 파일/패키지 구조
5. 코드 골격
6. 실행 흐름
7. 다음 단계

Use medium reasoning.
```

---

## 5. 왜 이 프롬프트가 좋은가

이 프롬프트는 아래 장점이 있습니다.

### 5-1. 저장소 규칙을 먼저 고정한다
`AGENTS.md`와 `docs`를 먼저 읽게 해서  
프로젝트 구조와 설명 방식이 흔들리지 않게 합니다.

### 5-2. Skills를 작업 방식으로 연결한다
Spring Boot API, ai-api 내부 엔드포인트, DB 스키마 작업을  
반복 가능한 방식으로 수행하게 만듭니다.

### 5-3. 범위를 처음부터 좁힌다
“전체 구현”이 아니라  
**첫 번째 구현 배치만** 하게 해서 Codex가 과하게 넓게 가지 않게 막습니다.

### 5-4. 설명까지 같이 받는다
코드만 생성하는 것이 아니라,
왜 필요한지 / 어떤 파일이 실행되는지 / DB 영향이 무엇인지
같이 설명하게 해서 학습용으로도 적합합니다.

### 5-5. 업로드된 학습형 README 원칙과 맞춘다
이 프롬프트는 프로젝트를 이해하는 README와
Codex용 시스템 프롬프트/체크리스트 문서의 역할 차이를 반영하여,
방향 고정 + 작은 실행 단위라는 두 목적을 동시에 잡습니다.

---

## 6. 추천 사용 순서

1. 새 Codex 세션에서 긴 버전을 먼저 넣는다
2. Codex가 첫 구현 배치를 제안하면
3. 그다음부터는 API별 프롬프트(`CODEX_PROMPT_EXAMPLES.md`)로 좁혀간다
4. 복잡한 Chat / Safety / RAG는 나중에 high reasoning으로 분리 요청한다

---

## 7. 한 줄 요약

이 문서는  
**업로드된 학습형 README의 원칙까지 반영해서, AGENTS.md + docs + Skills 구조를 전제로 Mind Compass 프로젝트를 Codex에서 처음부터 다시 안정적으로 시작하기 위한 한글 스타트 프롬프트**입니다.
