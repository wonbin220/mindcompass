// 캘린더 조회 API를 웹 프론트에서 호출하는 클라이언트 파일이다.
import { apiGet } from "@/lib/api/http";

export type EmotionTagResponse = {
  emotionCode: string;
  intensity: number | null;
  sourceType: string;
};

export type DiarySummaryResponse = {
  diaryId: number;
  title: string;
  preview: string;
  primaryEmotion: string | null;
  emotionIntensity: number | null;
  emotionTags: EmotionTagResponse[];
  writtenAt: string;
};

export type CalendarDayEmotionResponse = {
  date: string;
  hasDiary: boolean;
  diaryCount: number;
  primaryEmotion: string | null;
  averageIntensity: number | null;
  emotionTags: EmotionTagResponse[];
};

export type MonthlyEmotionCalendarResponse = {
  year: number;
  month: number;
  days: CalendarDayEmotionResponse[];
};

export type DailyEmotionSummaryResponse = {
  date: string;
  hasDiary: boolean;
  diaryCount: number;
  primaryEmotion: string | null;
  averageIntensity: number | null;
  emotionTags: EmotionTagResponse[];
  latestDiary: DiarySummaryResponse | null;
};

export async function getMonthlyEmotions(year: number, month: number) {
  return apiGet<MonthlyEmotionCalendarResponse>(
    `/api/v1/calendar/monthly-emotions?year=${year}&month=${month}`
  );
}

export async function getDailySummary(date: string) {
  return apiGet<DailyEmotionSummaryResponse>(`/api/v1/calendar/daily-summary?date=${date}`);
}
