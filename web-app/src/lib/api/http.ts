// backend-api 보호 엔드포인트 호출 시 공통 헤더와 에러 처리를 맡는 파일이다.
import { clearTokens, readAccessToken } from "@/lib/auth/token-storage";
import { webConfig } from "@/lib/config";

type ErrorResponse = {
  message?: string;
};

function getAuthHeaders() {
  const accessToken = readAccessToken();

  if (!accessToken) {
    throw new Error("로그인이 필요합니다.");
  }

  return {
    Authorization: `Bearer ${accessToken}`
  };
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${webConfig.backendApiBaseUrl}${path}`, {
    method: "GET",
    headers: getAuthHeaders()
  });

  if (response.status === 401 || response.status === 403) {
    clearTokens();
    throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
  }

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ErrorResponse | null;
    throw new Error(errorBody?.message ?? "데이터를 불러오지 못했습니다.");
  }

  return (await response.json()) as T;
}

export async function apiPost<TRequest, TResponse>(
  path: string,
  body: TRequest
): Promise<TResponse> {
  const response = await fetch(`${webConfig.backendApiBaseUrl}${path}`, {
    method: "POST",
    headers: {
      ...getAuthHeaders(),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  if (response.status === 401 || response.status === 403) {
    clearTokens();
    throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
  }

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ErrorResponse | null;
    throw new Error(errorBody?.message ?? "요청 처리에 실패했습니다.");
  }

  return (await response.json()) as TResponse;
}
