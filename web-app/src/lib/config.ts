// 웹 프론트에서 backend-api 주소를 읽는 설정 파일이다.
export const webConfig = {
  backendApiBaseUrl:
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ?? "http://localhost:8080"
};
