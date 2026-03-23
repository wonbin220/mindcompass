# Mind Compass Web App

웹 프론트엔드용 `Next.js + Tailwind CSS + shadcn/ui` 골격이다.

## 목적
- `backend-api`와 연결되는 웹 클라이언트 시작점 제공
- 모바일 UI 흐름을 웹 반응형으로 확장
- 로그인 / 캘린더 / 일기 / 채팅 / 리포트 화면의 공통 레이아웃 확보

## 현재 상태
- 설치 전 단계의 구조와 샘플 화면 골격까지 포함
- 패키지 설치와 기본 실행 확인 완료
- 로그인 화면은 `backend-api`의 `POST /api/v1/auth/login`과 연결됨

## 다음 단계
1. `.env.local` 생성 후 `NEXT_PUBLIC_BACKEND_API_BASE_URL` 설정
2. 캘린더/일기/채팅/리포트 API 클라이언트 추가
3. 토큰 복구와 보호 라우트 처리
4. shadcn/ui 컴포넌트 실제 생성
