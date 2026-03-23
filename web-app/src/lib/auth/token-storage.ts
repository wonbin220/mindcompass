// 로그인 성공 후 토큰을 브라우저에 저장하고 꺼내는 유틸 파일이다.
const ACCESS_TOKEN_KEY = "mindcompass.accessToken";
const REFRESH_TOKEN_KEY = "mindcompass.refreshToken";

type Tokens = {
  accessToken: string;
  refreshToken: string;
};

export function saveTokens(tokens: Tokens) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearTokens() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function readAccessToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function readRefreshToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}
