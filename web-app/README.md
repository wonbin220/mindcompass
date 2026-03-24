# Mind Compass Web App

웹 프론트엔드용 `Next.js + Tailwind CSS + shadcn/ui` 기반 클라이언트다.

## 목적
- `backend-api`와 연결되는 웹 클라이언트 시작점을 제공한다.
- 로그인, 캘린더, 일기, 채팅, 리포트 화면 흐름을 실제 API 응답 기준으로 점검한다.
- 모바일 연동 전에 웹에서 API 응답 구조와 보호 라우트를 먼저 확인한다.

## 현재 상태
- 주요 페이지와 공통 레이아웃 코드가 존재한다.
- `shadcn/ui` 설정 파일인 `components.json`과 `src/components/ui` 컴포넌트가 포함되어 있다.
- 로그인, 캘린더, 일기, 채팅, 리포트용 API 클라이언트 코드가 연결되어 있다.
- 인증 복구, 보호 라우트, 토큰 저장 흐름 코드가 들어 있다.
- `npm install`, `.env.local` 설정, `npm run build` 검증까지 완료했다.

## 실행 전 준비
1. `web-app/.env.example`를 참고해 `.env.local`을 준비한다.
2. `NEXT_PUBLIC_BACKEND_API_BASE_URL`을 로컬 `backend-api` 주소로 맞춘다.
3. `backend-api`가 먼저 실행 중인지 확인한다.

예시:

```env
NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080
```

## 로컬 실행
1. `npm install`
2. `npm run build`
3. `npm run dev`

기본 개발 주소:
- `http://localhost:3000`

## 화면별 API 연결 상태

### 1. Login
- 화면: `src/app/login/page.tsx`
- 컴포넌트: `src/components/auth/login-form.tsx`
- API:
  - `POST /api/v1/auth/login`
  - 로그인 완료 후 `GET /api/v1/users/me`로 세션 복구
- 상태:
  - 연결됨
  - 로그인 성공 시 `/calendar` 또는 redirect 대상 화면으로 이동

### 2. Diary
- 화면: `src/app/diary/page.tsx`
- 컴포넌트: `src/components/diary/diary-workspace.tsx`
- API:
  - `POST /api/v1/diaries`
  - `GET /api/v1/diaries/{diaryId}`
- 상태:
  - 연결됨
  - 작성 후 상세 조회를 다시 호출해 AI 분석 / 위험도 결과를 화면에 반영
  - diary 상세 응답의 `aiPrimaryEmotion`, `aiEmotionIntensity`, `aiSummary`, `aiConfidence`까지 표시하도록 계약 반영

### 3. Calendar
- 화면: `src/app/calendar/page.tsx`
- 컴포넌트: `src/components/calendar/calendar-dashboard.tsx`
- API:
  - `GET /api/v1/users/me`
  - `GET /api/v1/calendar/monthly-emotions`
  - `GET /api/v1/calendar/daily-summary`
- 상태:
  - 연결됨
  - 초기 진입 시 사용자 정보, 월간 캘린더, 오늘 요약을 함께 로드
  - 날짜 선택 시 일별 요약을 다시 조회

### 4. Chat
- 화면: `src/app/chat/page.tsx`
- 컴포넌트: `src/components/chat/chat-workspace.tsx`
- API:
  - `POST /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions/{sessionId}`
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
- 상태:
  - 연결됨
  - 세션 생성, 목록 조회, 상세 조회, 메시지 전송까지 한 화면에서 처리

### 5. Report
- 화면: `src/app/report/page.tsx`
- 컴포넌트: `src/components/report/report-dashboard.tsx`
- API:
  - `GET /api/v1/reports/monthly-summary`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly`
- 상태:
  - 연결됨
  - 월간 요약, 주간 감정 추이, 월간 위험도 추이를 함께 조회

## 인증 흐름
- `src/components/auth/auth-provider.tsx`가 전역 인증 상태를 관리한다.
- 보호 페이지 경로:
  - `/calendar`
  - `/diary`
  - `/chat`
  - `/report`
- 토큰이 없거나 만료되면 `/login`으로 이동한다.

## 확인 메모
- 현재 코드 기준으로 화면별 API 연결은 모두 들어가 있다.
- 실제 사용 전에는 `backend-api`를 켠 상태에서 아래 순서로 수동 확인하는 것이 좋다.
  1. login
  2. diary
  3. calendar
  4. chat
  5. report

## 다음 작업
1. 브라우저에서 `login -> diary -> calendar -> chat -> report` 순서로 실제 호출 확인
2. 필요 시 각 화면에 로딩 / 에러 / 빈 상태 문구를 더 다듬기
3. 모바일 연동 전에 웹 기준 API 응답 필드 누락 여부 점검

## Report 화면 계약 메모
- report 화면은 개별 diary의 `aiSummary`를 직접 표시하지 않는다.
- 월간 요약, 주간 감정 추이, 월간 위험도처럼 집계형 통계만 표시한다.
- 문장형 AI 해석은 diary 상세 화면에서 확인하는 흐름을 유지한다.
