// backend-api 인증 엔드포인트를 웹 앱에서 호출하는 클라이언트 파일이다.
import { webConfig } from "@/lib/config";

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: {
    userId: number;
    nickname: string;
  };
};

type ErrorResponse = {
  message?: string;
};

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(`${webConfig.backendApiBaseUrl}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ErrorResponse | null;
    throw new Error(errorBody?.message ?? "로그인에 실패했습니다.");
  }

  return (await response.json()) as LoginResponse;
}
