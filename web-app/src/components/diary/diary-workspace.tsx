"use client";

// 일기 작성과 AI 분석 결과 확인을 함께 처리하는 컴포넌트입니다.
import { FormEvent, useState, useTransition } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { StatusPill } from "@/components/ui/status-pill";
import { Textarea } from "@/components/ui/textarea";
import {
  createDiary,
  getDiary,
  PrimaryEmotion,
  type DiaryDetailResponse
} from "@/lib/api/diary";

const emotions: PrimaryEmotion[] = ["ANXIOUS", "OVERWHELMED", "CALM", "TIRED", "SAD", "HAPPY"];

function nowLocalDateTime() {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  const local = new Date(now.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
}

function getRiskTone(riskLevel: string | null) {
  if (riskLevel === "HIGH") {
    return "safety" as const;
  }

  if (riskLevel === "MEDIUM") {
    return "supportive" as const;
  }

  return "default" as const;
}

export function DiaryWorkspace() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [primaryEmotion, setPrimaryEmotion] = useState<PrimaryEmotion>("ANXIOUS");
  const [emotionIntensity, setEmotionIntensity] = useState(3);
  const [writtenAt, setWrittenAt] = useState(nowLocalDateTime());
  const [createdDiary, setCreatedDiary] = useState<DiaryDetailResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    startTransition(async () => {
      try {
        const created = await createDiary({
          title: title.trim(),
          content: content.trim(),
          primaryEmotion,
          emotionIntensity,
          emotionTags: [
            {
              emotionCode: primaryEmotion,
              intensity: emotionIntensity
            }
          ],
          writtenAt: `${writtenAt}:00`
        });

        const detail = await getDiary(created.diaryId);
        setCreatedDiary(detail);
        setSuccessMessage("일기가 저장되고 AI 분석 결과까지 반영되었습니다.");
      } catch (error) {
        const message =
          error instanceof Error ? error.message : "일기 저장 중 알 수 없는 오류가 발생했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      }
    });
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[1fr_0.9fr]">
      <section className="rounded-[28px] border border-[var(--line-soft)] bg-[var(--surface)] p-5 shadow-[var(--panel-shadow)]">
        <div className="mb-4">
          <h2 className="text-lg font-semibold text-[var(--text-strong)]">일기 작성</h2>
          <p className="mt-1 text-sm leading-6 text-[var(--text-muted)]">
            `POST /api/v1/diaries`로 기록을 저장하고, 저장 직후 `GET /api/v1/diaries/{'{'}diaryId{'}'}`
            로 상세 결과를 다시 불러옵니다.
          </p>
        </div>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <Label htmlFor="diary-title">Title</Label>
            <Input
              id="diary-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="오늘의 감정 제목"
            />
          </div>

          <div>
            <Label htmlFor="diary-content">Content</Label>
            <Textarea
              id="diary-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="오늘 있었던 일과 감정을 적어주세요."
              rows={8}
            />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="diary-emotion">Primary Emotion</Label>
              <Select
                id="diary-emotion"
                value={primaryEmotion}
                onChange={(event) => setPrimaryEmotion(event.target.value as PrimaryEmotion)}
              >
                {emotions.map((emotion) => (
                  <option key={emotion} value={emotion}>
                    {emotion}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <Label htmlFor="diary-intensity">Emotion Intensity</Label>
              <Input
                id="diary-intensity"
                value={emotionIntensity}
                onChange={(event) => setEmotionIntensity(Number(event.target.value))}
                type="range"
                min={1}
                max={5}
                className="px-0"
              />
              <p className="mt-2 text-sm font-medium text-[var(--text-strong)]">
                현재 강도: {emotionIntensity}
              </p>
            </div>
          </div>

          <div>
            <Label htmlFor="diary-written-at">Written At</Label>
            <Input
              id="diary-written-at"
              value={writtenAt}
              onChange={(event) => setWrittenAt(event.target.value)}
              type="datetime-local"
            />
          </div>

          <Button
            type="submit"
            disabled={isPending || title.trim().length === 0 || content.trim().length === 0}
            className="w-full"
          >
            {isPending ? "저장 중..." : "일기 저장하기"}
          </Button>
        </form>

        {errorMessage ? (
          <div className="mt-4 rounded-2xl bg-[var(--safety-soft)] px-4 py-3 text-sm text-[var(--safety-text)]">
            {errorMessage}
          </div>
        ) : null}

        {successMessage ? (
          <div className="mt-4 rounded-2xl bg-[var(--supportive-soft)] px-4 py-3 text-sm text-[var(--supportive-text)]">
            {successMessage}
          </div>
        ) : null}
      </section>

      <section className="rounded-[28px] border border-[var(--line-soft)] bg-[var(--surface)] p-5 shadow-[var(--panel-shadow)]">
        <div className="mb-4">
          <h2 className="text-lg font-semibold text-[var(--text-strong)]">저장된 상세 결과</h2>
          <p className="mt-1 text-sm leading-6 text-[var(--text-muted)]">
            diary 상세 응답에서 사용자 입력 감정과 AI 분석 감정, 위험도 필드를 함께 확인합니다.
          </p>
        </div>

        {createdDiary ? (
          <div className="space-y-4">
            <StatusPill tone={getRiskTone(createdDiary.riskLevel)}>
              {createdDiary.riskLevel ?? "NO RISK DATA"}
            </StatusPill>

            <div className="rounded-2xl bg-[var(--accent-soft)] p-4">
              <p className="text-sm text-[var(--text-muted)]">제목</p>
              <p className="mt-2 text-xl font-semibold text-[var(--text-strong)]">
                {createdDiary.title}
              </p>
            </div>

            <div className="rounded-2xl border border-[var(--line-soft)] bg-white p-4 text-sm leading-6 text-[var(--text-muted)]">
              {createdDiary.content}
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <div className="rounded-2xl bg-white p-4">
                <p className="text-sm text-[var(--text-muted)]">사용자 대표 감정</p>
                <p className="mt-2 text-lg font-semibold text-[var(--text-strong)]">
                  {createdDiary.primaryEmotion ?? "-"}
                </p>
              </div>
              <div className="rounded-2xl bg-white p-4">
                <p className="text-sm text-[var(--text-muted)]">사용자 감정 강도</p>
                <p className="mt-2 text-lg font-semibold text-[var(--text-strong)]">
                  {createdDiary.emotionIntensity ?? "-"}
                </p>
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <div className="rounded-2xl bg-white p-4">
                <p className="text-sm text-[var(--text-muted)]">AI 대표 감정</p>
                <p className="mt-2 text-lg font-semibold text-[var(--text-strong)]">
                  {createdDiary.aiPrimaryEmotion ?? "-"}
                </p>
              </div>
              <div className="rounded-2xl bg-white p-4">
                <p className="text-sm text-[var(--text-muted)]">AI 감정 강도</p>
                <p className="mt-2 text-lg font-semibold text-[var(--text-strong)]">
                  {createdDiary.aiEmotionIntensity ?? "-"}
                </p>
              </div>
            </div>

            <div className="rounded-2xl bg-white p-4 text-sm leading-6 text-[var(--text-muted)]">
              <p className="font-semibold text-[var(--text-strong)]">AI 감정 요약</p>
              <p className="mt-2">{createdDiary.aiSummary ?? "AI 분석 요약이 아직 없습니다."}</p>
              <p className="mt-2">aiConfidence: {createdDiary.aiConfidence ?? "-"}</p>
            </div>

            <div className="rounded-2xl bg-white p-4">
              <p className="text-sm text-[var(--text-muted)]">감정 태그</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {createdDiary.emotionTags.length > 0 ? (
                  createdDiary.emotionTags.map((tag, index) => (
                    <StatusPill key={`${tag.emotionCode}-${index}`}>
                      {tag.emotionCode} / {tag.sourceType}
                    </StatusPill>
                  ))
                ) : (
                  <span className="text-sm text-[var(--text-muted)]">
                    감정 태그가 아직 없습니다.
                  </span>
                )}
              </div>
            </div>

            <div className="rounded-2xl bg-white p-4 text-sm leading-6 text-[var(--text-muted)]">
              <p className="font-semibold text-[var(--text-strong)]">위험도 해석</p>
              <p className="mt-2">riskScore: {createdDiary.riskScore ?? "-"}</p>
              <p>riskSignals: {createdDiary.riskSignals ?? "-"}</p>
              <p>recommendedAction: {createdDiary.recommendedAction ?? "-"}</p>
            </div>
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-[var(--line-soft)] bg-white p-6 text-sm leading-6 text-[var(--text-muted)]">
            아직 저장한 일기가 없습니다. 왼쪽 폼으로 일기를 저장하면 이 영역에서 AI 분석과 위험도
            결과까지 바로 확인할 수 있습니다.
          </div>
        )}
      </section>
    </div>
  );
}
