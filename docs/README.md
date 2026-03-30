# Mind Compass API 학습 문서 모음

이 폴더는 도메인별 API 학습 문서를 모아둔 곳이다.

문서 목록:
- `AUTH_API_LEARNING.md`
- `DIARY_API_LEARNING.md`
- `CALENDAR_API_LEARNING.md`
- `CHAT_API_LEARNING.md`
- `REPORT_API_LEARNING.md`
- `DB_TABLE_SPECIFICATION.md`
- `BACKEND_AI_LOCAL_RUN_GUIDE.md`
- `sql/erdcloud_current_schema.sql`
- `OPERATIONS_GUIDE.md`
- `MOBILE_INTEGRATION_HANDOFF.md` (legacy filename, now used as responsive web integration handoff)
- `SCREEN_TO_API_MAPPING.md`

이 문서들의 목적:
1. 각 API가 왜 필요한지 이해한다.
2. 어떤 화면에서 호출되는지 이해한다.
3. 어떤 파일들이 순차적으로 실행되는지 이해한다.
4. Controller / Service / Repository / DTO / Entity 역할을 구분한다.
5. Codex에게 API를 시킬 때 단순 코드 생성이 아니라 학습 가능한 형태로 요청한다.

권장 읽는 순서:
1. Auth
2. Diary
3. Calendar
4. Chat
5. Report
6. DB Table Specification

이 순서가 좋은 이유:
- Auth가 사용자 식별의 시작점
- Diary가 핵심 기록 기능
- Calendar가 기록을 보는 조회 기능
- Chat이 가장 복잡한 AI 연계 흐름
- Report가 기록과 안전 신호를 기간 단위 인사이트로 요약
- DB Table Specification이 실제 테이블 구조와 관계를 한 번에 정리

앞으로 새 도메인을 추가할 때도 같은 형식으로 문서를 늘리면 된다.

AI 운영과 manual OpenAI 검증 관련 참고:
- `BACKEND_AI_LOCAL_RUN_GUIDE.md`
- `ai-api/OPENAI_USAGE_AND_PROFILE_GUIDE.md`
- `ai-api/MANUAL_OPENAI_QUALITY_CHECKLIST.md`

## 웹 프론트 시작점

`web-app`은 `Next.js + Tailwind CSS + shadcn/ui` 기준의 웹 클라이언트 골격이다.

현재 포함 범위:
- App Router 기준 기본 구조
- 공통 레이아웃과 사이드 네비게이션
- `login`, `calendar`, `diary`, `chat`, `report` 샘플 페이지

아직 하지 않은 것:
- 패키지 설치
- 실제 `backend-api` API 클라이언트 연결
- shadcn/ui 컴포넌트 실생성

중요 원칙:
- 반응형 웹은 `backend-api`만 호출한다.
- `ai-api`와 `ai-api-fastapi`는 내부 서버 계층으로만 사용한다.
