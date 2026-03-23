# Auth API 학습 문서

이 문서는 `Auth` 도메인을 학습하기 위한 문서다.  
목표는 단순히 로그인/회원가입 API를 나열하는 것이 아니라, **왜 이 API가 필요한지**, **외부 요청이 들어오면 어떤 파일이 어떤 순서로 실행되는지**, **Controller / Service / Repository / DTO / Entity가 어떻게 협력하는지**를 이해하는 것이다.

---

# 1. Auth 도메인이 왜 가장 먼저 필요한가

이 프로젝트는 감정일기 앱이지만, 실제 저장 단위는 모두 **사용자 기준**이다.

예를 들면:
- 일기는 누가 썼는가?
- 감정 캘린더는 누구의 기록인가?
- 상담 세션은 누구의 대화인가?
- 통계 리포트는 누구의 데이터인가?

이 질문에 답하려면 먼저 사용자를 식별해야 한다.  
그래서 백엔드 개발 우선순위에서도 `Auth`는 가장 앞쪽에 온다.

즉:

- Auth가 있어야 Diary가 사용자별로 저장된다.
- Auth가 있어야 Calendar가 “내 감정 캘린더”가 된다.
- Auth가 있어야 Chat이 “내 상담 세션”이 된다.

---

# 2. Auth 도메인에서 이해해야 할 핵심 개념

## 2-1. 회원가입
사용자를 새로 만드는 API다.

## 2-2. 로그인
사용자를 검증하고 토큰을 발급하는 API다.

## 2-3. Access Token
짧게 쓰는 인증 토큰이다. API 요청 시 헤더에 넣는다.

## 2-4. Refresh Token
Access Token이 만료되었을 때 새 토큰을 받기 위해 쓰는 토큰이다.

## 2-5. 내 정보 조회
로그인한 사용자가 “내가 누구인지” 확인하는 API다.

## 2-6. 사용자 설정
앱 잠금 여부, 알림 여부, 기본 설정 등을 저장하는 API다.

---

# 3. Auth 패키지 예시 구조

```text
backend-api/
└─ src/main/java/com/mindcompass/api/
   ├─ auth/
   │  ├─ controller/
   │  │  └─ AuthController.java
   │  ├─ service/
   │  │  └─ AuthService.java
   │  ├─ repository/
   │  │  ├─ UserRepository.java
   │  │  └─ UserSettingsRepository.java
   │  ├─ domain/
   │  │  ├─ User.java
   │  │  └─ UserSettings.java
   │  ├─ dto/
   │  │  ├─ request/
   │  │  │  ├─ SignUpRequest.java
   │  │  │  ├─ LoginRequest.java
   │  │  │  ├─ RefreshTokenRequest.java
   │  │  │  └─ UpdateUserSettingsRequest.java
   │  │  └─ response/
   │  │     ├─ SignUpResponse.java
   │  │     ├─ LoginResponse.java
   │  │     ├─ UserMeResponse.java
   │  │     └─ UserSettingsResponse.java
   │  └─ security/
   │     ├─ JwtTokenProvider.java
   │     ├─ JwtAuthenticationFilter.java
   │     └─ CustomUserDetailsService.java
   └─ common/
      ├─ response/
      └─ exception/
```

---

# 4. Auth에서 가장 중요한 실행 흐름

Auth 도메인은 다른 도메인과 달리 **Spring Security와 JWT 필터**가 매우 중요하다.

기본 흐름은 아래와 같다.

1. 앱이 HTTP 요청을 보낸다.
2. 공개 API인지 보호 API인지 구분된다.
3. 보호 API면 `JwtAuthenticationFilter`가 먼저 토큰을 검사한다.
4. 인증이 통과되면 Controller로 요청이 들어간다.
5. Controller가 DTO로 요청을 받는다.
6. Service가 실제 비즈니스 로직을 수행한다.
7. Repository가 DB를 조회/저장한다.
8. Service가 응답 DTO를 만든다.
9. Controller가 최종 응답을 반환한다.

---

# 5. API 1 - 회원가입

## 5-1. 엔드포인트
`POST /api/v1/auth/signup`

## 5-2. 왜 필요한가
서비스를 처음 쓰는 사용자가 계정을 만들어야 한다.  
이후 Diary, Calendar, Chat, Report 전부 사용자 ID 기준으로 동작하므로 회원가입은 시작점이다.

## 5-3. 어느 화면에서 쓰는가
- 앱의 회원가입 화면
- 이메일 가입 플로우
- 추후 소셜 로그인 연동 전 기본 회원 시스템

## 5-4. 요청 예시
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!",
  "nickname": "냥집사"
}
```

## 5-5. 응답 예시
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "냥집사",
  "createdAt": "2026-03-18T10:00:00"
}
```

## 5-6. 관련 파일
- `AuthController.java`
- `AuthService.java`
- `UserRepository.java`
- `User.java`
- `UserSettings.java`
- `SignUpRequest.java`
- `SignUpResponse.java`

## 5-7. 실행 순서
1. 앱이 `POST /api/v1/auth/signup` 요청을 보낸다.
2. `AuthController.signup()`이 요청을 받는다.
3. 요청 body가 `SignUpRequest` DTO로 변환된다.
4. Controller가 `AuthService.signup(request)`를 호출한다.
5. `AuthService`가 이메일 중복 여부를 확인한다.
6. `UserRepository.findByEmail()`이 DB에서 이메일 존재 여부를 조회한다.
7. 중복이 없으면 비밀번호를 암호화한다.
8. `User` 엔티티를 생성한다.
9. 기본 설정이 필요하면 `UserSettings` 엔티티도 생성한다.
10. `UserRepository.save()`로 사용자를 저장한다.
11. 저장 결과를 `SignUpResponse`로 변환한다.
12. Controller가 응답을 반환한다.

## 5-8. DB 영향
- `users` 테이블 insert
- `user_settings` 테이블 insert (기본 설정을 함께 만들 경우)

## 5-9. 예외 상황
- 이미 존재하는 이메일
- 비밀번호 정책 불일치
- 닉네임 길이 초과
- 필수값 누락

## 5-10. 학습 포인트
회원가입 API의 핵심은:
- **사용자 생성**
- **중복 검증**
- **비밀번호 암호화**
- **기본 설정 초기화**

즉, “데이터를 저장한다”는 점에서 Diary와 비슷해 보일 수 있지만,  
실제로는 **보안과 식별 체계의 시작점**이라는 점이 더 중요하다.

---

# 6. API 2 - 로그인

## 6-1. 엔드포인트
`POST /api/v1/auth/login`

## 6-2. 왜 필요한가
사용자 본인인지 확인하고, 이후 API 호출에 사용할 토큰을 발급해야 한다.

## 6-3. 어느 화면에서 쓰는가
- 앱 로그인 화면
- 앱 재실행 후 재인증 플로우

## 6-4. 요청 예시
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!"
}
```

## 6-5. 응답 예시
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "user": {
    "userId": 1,
    "nickname": "냥집사"
  }
}
```

## 6-6. 관련 파일
- `AuthController.java`
- `AuthService.java`
- `UserRepository.java`
- `JwtTokenProvider.java`
- `LoginRequest.java`
- `LoginResponse.java`

## 6-7. 실행 순서
1. 앱이 이메일/비밀번호를 담아 로그인 요청을 보낸다.
2. `AuthController.login()`이 요청을 받는다.
3. 요청 body가 `LoginRequest` DTO로 변환된다.
4. `AuthService.login()`이 호출된다.
5. `UserRepository.findByEmail()`로 사용자를 조회한다.
6. Service가 `PasswordEncoder.matches()`로 비밀번호를 검증한다.
7. 검증 성공 시 `JwtTokenProvider`가 access token을 생성한다.
8. 필요하면 refresh token도 생성한다.
9. refresh token을 DB 또는 별도 저장소에 저장할 수 있다.
10. `LoginResponse` DTO를 만든다.
11. Controller가 응답을 반환한다.

## 6-8. DB 영향
- 기본적으로 조회
- refresh token 저장 전략을 쓰면 token 관련 테이블 update/insert 가능

## 6-9. 예외 상황
- 존재하지 않는 이메일
- 비밀번호 불일치
- 비활성 사용자
- 탈퇴한 사용자

## 6-10. 학습 포인트
로그인은 회원가입과 다르게 “새 데이터 생성”보다  
**자격 검증 + 토큰 발급**이 핵심이다.

즉:
- 회원가입은 사용자를 만든다.
- 로그인은 사용자를 검증한다.

---

# 7. API 3 - 토큰 재발급

## 7-1. 엔드포인트
`POST /api/v1/auth/refresh`

## 7-2. 왜 필요한가
Access Token은 보통 수명이 짧다.  
사용자가 앱을 오래 쓰더라도 매번 다시 로그인하지 않도록 refresh token으로 access token을 재발급한다.

## 7-3. 어느 화면에서 쓰는가
- 앱 내부에서 자동 호출
- 사용자는 보통 직접 의식하지 않는다

## 7-4. 요청 예시
```json
{
  "refreshToken": "stored-refresh-token"
}
```

## 7-5. 응답 예시
```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token"
}
```

## 7-6. 관련 파일
- `AuthController.java`
- `AuthService.java`
- `JwtTokenProvider.java`
- `RefreshTokenRequest.java`
- `LoginResponse.java` 또는 별도 `TokenRefreshResponse.java`

## 7-7. 실행 순서
1. 앱이 저장된 refresh token으로 재발급 요청을 보낸다.
2. `AuthController.refresh()`가 요청을 받는다.
3. `RefreshTokenRequest` DTO로 body가 파싱된다.
4. `AuthService.refreshToken()`이 호출된다.
5. Service가 refresh token의 서명/만료 여부를 검증한다.
6. 저장된 token과 비교하는 정책이면 DB/저장소를 조회한다.
7. 유효하면 새 access token을 발급한다.
8. refresh token rotation 정책이면 새 refresh token도 발급한다.
9. 응답 DTO를 생성한다.
10. Controller가 응답을 반환한다.

## 7-8. 학습 포인트
이 API는 사용자가 잘 보지 않지만 운영에서는 중요하다.  
토큰 정책이 꼬이면:
- 로그인은 되는데 보호 API가 안 되거나
- 앱이 계속 로그아웃되거나
- 보안 이슈가 생길 수 있다.

---

# 8. API 4 - 내 정보 조회

## 8-1. 엔드포인트
`GET /api/v1/users/me`

## 8-2. 왜 필요한가
로그인한 사용자가 누구인지, 앱 상단 프로필/설정 화면에서 보여주기 위해 필요하다.

## 8-3. 어느 화면에서 쓰는가
- 내 정보 화면
- 설정 화면
- 앱 초기 진입 후 로그인 상태 확인

## 8-4. 관련 파일
- `UserController.java` 또는 `AuthController.java`
- `UserService.java`
- `UserRepository.java`
- `JwtAuthenticationFilter.java`
- `UserMeResponse.java`

## 8-5. 실행 순서
1. 앱이 access token을 헤더에 담아 요청한다.
2. `JwtAuthenticationFilter`가 토큰을 먼저 검증한다.
3. 인증에 성공하면 Security Context에 사용자 정보가 들어간다.
4. Controller가 요청을 받는다.
5. Controller는 현재 인증된 사용자 ID를 꺼낸다.
6. `UserService.getMe(userId)`를 호출한다.
7. `UserRepository.findById()`가 DB를 조회한다.
8. 사용자 정보를 `UserMeResponse` DTO로 변환한다.
9. Controller가 응답을 반환한다.

## 8-6. 학습 포인트
이 API가 중요한 이유는:
**로그인 성공 = 토큰 발급**이고  
**내 정보 조회 성공 = 토큰으로 실제 인증이 동작한다는 증거**이기 때문이다.

---

# 9. API 5 - 사용자 설정 수정

## 9-1. 엔드포인트
`PATCH /api/v1/users/me/settings`

## 9-2. 왜 필요한가
앱 잠금, 알림 수신, 상담 톤, 기록 리마인드 등 사용자별 설정을 저장하려면 필요하다.

## 9-3. 어느 화면에서 쓰는가
- 설정 화면

## 9-4. 요청 예시
```json
{
  "appLockEnabled": true,
  "notificationEnabled": true,
  "dailyReminderTime": "22:00"
}
```

## 9-5. 관련 파일
- `UserController.java`
- `UserService.java`
- `UserSettingsRepository.java`
- `UserSettings.java`
- `UpdateUserSettingsRequest.java`
- `UserSettingsResponse.java`

## 9-6. 실행 순서
1. 앱이 access token과 설정 변경값을 담아 요청한다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 현재 사용자 ID를 확인한다.
4. `UserService.updateSettings(userId, request)`를 호출한다.
5. `UserSettingsRepository.findByUserId()`로 기존 설정을 조회한다.
6. 변경 가능한 필드를 수정한다.
7. `UserSettingsRepository.save()`로 저장한다.
8. 저장 결과를 응답 DTO로 만든다.
9. Controller가 응답을 반환한다.

## 9-7. 학습 포인트
`PATCH`는 보통 전체를 덮어쓰기보다 **일부 필드만 변경**하는 데 적합하다.

---

# 10. Auth 도메인 전체 흐름 한 번에 보기

## 회원가입
앱 → Controller → Service → UserRepository → DB 저장 → Response

## 로그인
앱 → Controller → Service → UserRepository → 비밀번호 검증 → JwtTokenProvider → Response

## 보호 API (`/users/me`)
앱 → JwtAuthenticationFilter → Controller → Service → Repository → Response

이 차이를 이해하면:
- 공개 API
- 보호 API
- 토큰 발급 API
의 차이가 머리에 들어온다.

---

# 11. Auth를 이해할 때 꼭 기억할 문장

- 회원가입은 사용자를 만든다.
- 로그인은 사용자를 검증하고 토큰을 만든다.
- 보호 API는 Controller보다 먼저 JWT 필터를 지난다.
- Auth가 안정적이어야 Diary, Calendar, Chat이 의미를 가진다.
