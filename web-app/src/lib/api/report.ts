// 리포트 조회 API를 웹 프론트에서 호출하는 클라이언트 파일이다.
import { apiGet } from "@/lib/api/http";

export type EmotionCountResponse = {
  emotion: string;
  count: number;
};

export type RiskSummaryResponse = {
  mediumCount: number;
  highCount: number;
};

export type MonthlyReportResponse = {
  year: number;
  month: number;
  diaryCount: number;
  averageEmotionIntensity: number | null;
  topPrimaryEmotions: EmotionCountResponse[];
  riskSummary: RiskSummaryResponse;
};

export type EmotionTrendPointResponse = {
  date: string;
  hasDiary: boolean;
  diaryCount: number;
  primaryEmotion: string | null;
  averageEmotionIntensity: number | null;
};

export type WeeklyEmotionTrendResponse = {
  startDate: string;
  endDate: string;
  items: EmotionTrendPointResponse[];
};

export type RiskTrendPointResponse = {
  date: string;
  mediumCount: number;
  highCount: number;
};

export type MonthlyRiskTrendResponse = {
  year: number;
  month: number;
  items: RiskTrendPointResponse[];
};

export async function getMonthlySummary(year: number, month: number) {
  return apiGet<MonthlyReportResponse>(
    `/api/v1/reports/monthly-summary?year=${year}&month=${month}`
  );
}

export async function getWeeklyEmotionTrend(date?: string) {
  const query = date ? `?date=${date}` : "";
  return apiGet<WeeklyEmotionTrendResponse>(`/api/v1/reports/emotions/weekly${query}`);
}

export async function getMonthlyRiskTrend(year: number, month: number) {
  return apiGet<MonthlyRiskTrendResponse>(
    `/api/v1/reports/risks/monthly?year=${year}&month=${month}`
  );
}
