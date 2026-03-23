# Frontend Progress

이 문서는 `web-app`의 현재 UI/API 연동 상태와 중요한 프론트 결정 사항을 정리하는 문서다.

## 현재 기준
- 기준 날짜: 2026-03-24
- 작업 범위: `web-app`
- 기술 스택
  - `Next.js 13.5.11`
  - `React 18`
  - `Tailwind CSS 3`
  - `shadcn/ui` 스타일의 로컬 공통 컴포넌트

## 현재 구조
- `src/app`
- `src/components/layout`
- `src/components/ui`
- `src/components/auth`
- `src/components/calendar`
- `src/components/diary`
- `src/components/chat`
- `src/components/report`
- `src/lib/api`
- `src/lib/auth`

## 구현 완료

### 공통 구조
- `AppShell` 공통 레이아웃
- 데스크톱 sticky 사이드바
- 모바일 상단 퀵 네비게이션
- `AuthProvider` 기반 전역 인증 복구/보호 라우트
- 세션 요약 드롭다운과 로그아웃

### 로그인
- `POST /api/v1/auth/login` 연동
- 토큰 `localStorage` 저장
- 로그인 성공 후 redirect 복귀 지원

### 캘린더
- `GET /api/v1/users/me`
- `GET /api/v1/calendar/monthly-emotions`
- `GET /api/v1/calendar/daily-summary`
- 월간 감정 표시와 날짜 선택 요약

### 일기
- `POST /api/v1/diaries`
- `GET /api/v1/diaries/{diaryId}`
- 저장 후 상세 재조회로 AI 분석/위험도 표시

### 채팅
- `POST /api/v1/chat/sessions`
- `GET /api/v1/chat/sessions`
- `GET /api/v1/chat/sessions/{sessionId}`
- `POST /api/v1/chat/sessions/{sessionId}/messages`
- `responseType` 상태 배지 표시

### 리포트
- `GET /api/v1/reports/monthly-summary`
- `GET /api/v1/reports/emotions/weekly`
- `GET /api/v1/reports/risks/monthly`
- 월간 요약, 주간 감정 추이, 월간 위험도 추이 표시
- 주간 감정 추이에 기준 날짜 input 추가
- 이전 7일 / 다음 7일 버튼 추가
- 월간 위험도 추이 카드에 내부 스크롤 적용
- `scrollbar-soft` 클래스로 스크롤바 톤 보정

## 공통 UI
- `Button`
- `Card`
- `Input`
- `Textarea`
- `Select`
- `Label`
- `ScreenCard`
- `StatusPill`

## 환경 메모
- `web-app/.env.local`
  - `NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080`
- `backend-api`
  - `WEB_ALLOWED_ORIGINS=http://localhost:3000`

## 검증 상태
- `web-app`: `npm install` 완료
- `web-app`: `npm run build` 통과
- `backend-api`: `./gradlew.bat compileJava` 통과

## 현재 보류
- 차트 시각화 고도화
- 모바일 실제 코드 연동

## 다음 추천 작업
1. 리포트 차트 시각화 고도화
2. 캘린더/리포트 디자인 디테일 리터치
3. 모바일 앱 코드와 실제 연동
