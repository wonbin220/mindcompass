// 사용자 내 정보 조회 API를 웹 프론트에서 호출하는 클라이언트 파일이다.
import { apiGet } from "@/lib/api/http";

export type UserMeResponse = {
  userId: number;
  email: string;
  nickname: string;
  status: string;
  createdAt: string;
  settings: {
    appLockEnabled: boolean;
    notificationEnabled: boolean;
    dailyReminderTime: string | null;
    responseMode: string;
  };
};

export async function getMe() {
  return apiGet<UserMeResponse>("/api/v1/users/me");
}
