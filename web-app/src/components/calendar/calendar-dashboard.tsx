"use client";

// 로그인 사용자 정보와 캘린더 데이터를 함께 보여주는 대시보드 컴포넌트다.
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { ScreenCard } from "@/components/ui/screen-card";
import { StatusPill } from "@/components/ui/status-pill";
import {
  CalendarDayEmotionResponse,
  DailyEmotionSummaryResponse,
  getDailySummary,
  getMonthlyEmotions
} from "@/lib/api/calendar";
import { getMe, UserMeResponse } from "@/lib/api/user";

function formatDateLabel(dateText: string) {
  return dateText.replaceAll("-", ".");
}

function getToday() {
  return new Date().toISOString().slice(0, 10);
}

export function CalendarDashboard() {
  const router = useRouter();
  const today = getToday();
  const [selectedDate, setSelectedDate] = useState(today);
  const [me, setMe] = useState<UserMeResponse | null>(null);
  const [days, setDays] = useState<CalendarDayEmotionResponse[]>([]);
  const [dailySummary, setDailySummary] = useState<DailyEmotionSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;

    async function loadInitialData() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [meResponse, monthlyResponse, dailyResponse] = await Promise.all([
          getMe(),
          getMonthlyEmotions(year, month),
          getDailySummary(today)
        ]);

        if (cancelled) {
          return;
        }

        setMe(meResponse);
        setDays(monthlyResponse.days);
        setDailySummary(dailyResponse);
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message =
          error instanceof Error ? error.message : "캘린더 데이터를 불러오지 못했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadInitialData();

    return () => {
      cancelled = true;
    };
  }, [router, today]);

  async function handleSelectDate(date: string) {
    setSelectedDate(date);
    setErrorMessage(null);

    try {
      const response = await getDailySummary(date);
      setDailySummary(response);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "하루 요약을 불러오지 못했습니다.";
      setErrorMessage(message);

      if (message.includes("로그인") || message.includes("세션")) {
        router.replace("/login");
      }
    }
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
      <ScreenCard
        title="월간 감정 흐름"
        description={
          me
            ? `${me.nickname}님의 월간 기록을 날짜 단위로 요약합니다.`
            : "`GET /api/v1/calendar/monthly-emotions` 연동 결과"
        }
      >
        {isLoading ? (
          <div className="grid grid-cols-7 gap-2">
            {Array.from({ length: 35 }, (_, index) => (
              <div
                key={index}
                className="aspect-square animate-pulse rounded-2xl bg-[var(--accent-soft)]"
              />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-7 gap-2">
            {days.map((day) => (
              <Button
                key={day.date}
                type="button"
                variant={selectedDate === day.date ? "default" : "outline"}
                onClick={() => void handleSelectDate(day.date)}
                className="flex aspect-square h-auto flex-col rounded-2xl px-1 py-3 text-center"
              >
                <span className="text-sm font-semibold">{new Date(day.date).getDate()}</span>
                <span className="mt-1 text-[10px] font-medium">
                  {day.primaryEmotion ?? (day.hasDiary ? "기록" : "-")}
                </span>
              </Button>
            ))}
          </div>
        )}
      </ScreenCard>

      <ScreenCard title="선택 날짜 요약" description="`GET /api/v1/calendar/daily-summary` 연동 결과">
        <div className="space-y-4">
          <StatusPill>{formatDateLabel(selectedDate)}</StatusPill>

          {errorMessage ? (
            <div className="rounded-2xl bg-[var(--safety-soft)] p-4 text-sm text-[var(--safety-text)]">
              {errorMessage}
            </div>
          ) : null}

          <div className="rounded-2xl bg-[var(--accent-soft)] p-4">
            <p className="text-sm text-[var(--text-muted)]">대표 감정</p>
            <p className="mt-2 text-xl font-semibold text-[var(--text-strong)]">
              {dailySummary?.primaryEmotion ?? "기록 없음"}
            </p>
          </div>

          <div className="rounded-2xl border border-[var(--line-soft)] bg-white p-4 text-sm leading-6 text-[var(--text-muted)]">
            {dailySummary?.latestDiary ? (
              <>
                <p className="font-semibold text-[var(--text-strong)]">
                  {dailySummary.latestDiary.title}
                </p>
                <p className="mt-2">{dailySummary.latestDiary.preview}</p>
              </>
            ) : (
              "이 날짜에는 아직 저장한 일기가 없습니다."
            )}
          </div>

          <div className="flex flex-wrap gap-2">
            {(dailySummary?.emotionTags ?? []).length > 0 ? (
              dailySummary?.emotionTags.map((tag, index) => (
                <StatusPill key={`${tag.emotionCode}-${index}`}>{tag.emotionCode}</StatusPill>
              ))
            ) : (
              <span className="text-sm text-[var(--text-muted)]">감정 태그가 아직 없습니다.</span>
            )}
          </div>
        </div>
      </ScreenCard>
    </div>
  );
}
