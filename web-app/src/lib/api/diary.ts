// 일기 생성과 상세 조회 API를 웹 프론트에서 호출하는 클라이언트 파일이다.
import { apiGet, apiPost } from "@/lib/api/http";

export type PrimaryEmotion =
  | "RELIEVED"
  | "ANGRY"
  | "SAD"
  | "LONELY"
  | "NUMB"
  | "CALM"
  | "HAPPY"
  | "TIRED"
  | "ANXIOUS"
  | "OVERWHELMED";

export type EmotionTagRequest = {
  emotionCode: PrimaryEmotion;
  intensity: number;
};

export type EmotionTagResponse = {
  emotionCode: PrimaryEmotion;
  intensity: number | null;
  sourceType: string;
};

export type CreateDiaryRequest = {
  title: string;
  content: string;
  primaryEmotion: PrimaryEmotion;
  emotionIntensity: number;
  emotionTags: EmotionTagRequest[];
  writtenAt: string;
};

export type DiaryDetailResponse = {
  diaryId: number;
  userId: number;
  title: string;
  content: string;
  primaryEmotion: PrimaryEmotion | null;
  emotionIntensity: number | null;
  emotionTags: EmotionTagResponse[];
  riskLevel: string | null;
  riskScore: number | null;
  riskSignals: string | null;
  recommendedAction: string | null;
  writtenAt: string;
  createdAt: string;
  updatedAt: string;
};

export async function createDiary(request: CreateDiaryRequest) {
  return apiPost<CreateDiaryRequest, DiaryDetailResponse>("/api/v1/diaries", request);
}

export async function getDiary(diaryId: number) {
  return apiGet<DiaryDetailResponse>(`/api/v1/diaries/${diaryId}`);
}
