"use client";

// 월간 요약, 주간 감정 추이, 월간 위험도 추이를 보여주는 리포트 컴포넌트다.
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScreenCard } from "@/components/ui/screen-card";
import {
  getMonthlyRiskTrend,
  getMonthlySummary,
  getWeeklyEmotionTrend,
  type MonthlyReportResponse,
  type MonthlyRiskTrendResponse,
  type WeeklyEmotionTrendResponse
} from "@/lib/api/report";

function getCurrentYearMonth() {
  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1
  };
}

function formatDateInputValue(date: Date) {
  return date.toISOString().slice(0, 10);
}

function shiftDate(value: string, days: number) {
  const target = new Date(`${value}T00:00:00`);
  target.setDate(target.getDate() + days);
  return formatDateInputValue(target);
}

export function ReportDashboard() {
  const router = useRouter();
  const { year, month } = useMemo(() => getCurrentYearMonth(), []);
  const [selectedWeekDate, setSelectedWeekDate] = useState(() => formatDateInputValue(new Date()));
  const [monthlySummary, setMonthlySummary] = useState<MonthlyReportResponse | null>(null);
  const [weeklyTrend, setWeeklyTrend] = useState<WeeklyEmotionTrendResponse | null>(null);
  const [monthlyRiskTrend, setMonthlyRiskTrend] = useState<MonthlyRiskTrendResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadReports() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [monthlySummaryResponse, weeklyTrendResponse, monthlyRiskTrendResponse] =
          await Promise.all([
            getMonthlySummary(year, month),
            getWeeklyEmotionTrend(selectedWeekDate),
            getMonthlyRiskTrend(year, month)
          ]);

        if (cancelled) {
          return;
        }

        setMonthlySummary(monthlySummaryResponse);
        setWeeklyTrend(weeklyTrendResponse);
        setMonthlyRiskTrend(monthlyRiskTrendResponse);
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message =
          error instanceof Error ? error.message : "리포트 데이터를 불러오지 못했습니다.";
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

    void loadReports();

    return () => {
      cancelled = true;
    };
  }, [month, router, selectedWeekDate, year]);

  return (
    <div className="grid gap-6">
      {errorMessage ? (
        <div className="rounded-2xl bg-[var(--safety-soft)] px-4 py-3 text-sm text-[var(--safety-text)]">
          {errorMessage}
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-3">
        <ScreenCard title="월간 요약">
          <div className="text-3xl font-semibold text-[var(--text-strong)]">
            {isLoading ? "-" : monthlySummary?.diaryCount ?? 0}
          </div>
          <p className="mt-2 text-sm text-[var(--text-muted)]">이번 달 기록 수</p>
        </ScreenCard>

        <ScreenCard title="평균 감정 강도">
          <div className="text-3xl font-semibold text-[var(--text-strong)]">
            {isLoading ? "-" : monthlySummary?.averageEmotionIntensity ?? "-"}
          </div>
          <p className="mt-2 text-sm text-[var(--text-muted)]">월간 평균 intensity</p>
        </ScreenCard>

        <ScreenCard title="고위험 기록">
          <div className="text-3xl font-semibold text-[var(--text-strong)]">
            {isLoading ? "-" : monthlySummary?.riskSummary.highCount ?? 0}
          </div>
          <p className="mt-2 text-sm text-[var(--text-muted)]">SAFETY 대응 필요 건수</p>
        </ScreenCard>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <ScreenCard
          title="주간 감정 추이"
          description="선택한 날짜를 포함한 최근 7일 감정 흐름을 날짜별 카드로 보여줍니다."
        >
          <div className="mb-4 flex flex-col gap-3 rounded-2xl border border-[var(--line-soft)] bg-[#fcfaf6] p-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold text-[var(--text-strong)]">기준 날짜 선택</p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                {weeklyTrend
                  ? `${weeklyTrend.startDate} ~ ${weeklyTrend.endDate}`
                  : "선택 날짜를 기준으로 앞뒤 일주일 흐름을 보여줍니다."}
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="outline"
                className="px-3 py-2 text-xs"
                onClick={() => setSelectedWeekDate((current) => shiftDate(current, -7))}
              >
                이전 7일
              </Button>
              <Input
                type="date"
                value={selectedWeekDate}
                onChange={(event) => setSelectedWeekDate(event.target.value)}
                className="w-[168px] bg-white"
              />
              <Button
                type="button"
                variant="outline"
                className="px-3 py-2 text-xs"
                onClick={() => setSelectedWeekDate((current) => shiftDate(current, 7))}
              >
                다음 7일
              </Button>
            </div>
          </div>

          <div className="grid gap-3 xl:min-h-[560px]">
            {(weeklyTrend?.items ?? []).map((item) => (
              <div
                key={item.date}
                className="flex items-center justify-between rounded-2xl border border-[var(--line-soft)] bg-white px-4 py-3 text-sm"
              >
                <div>
                  <p className="font-semibold text-[var(--text-strong)]">{item.date}</p>
                  <p className="mt-1 text-[var(--text-muted)]">
                    {item.primaryEmotion ?? "기록 없음"}
                  </p>
                </div>
                <div className="text-right text-[var(--text-muted)]">
                  <p>count {item.diaryCount}</p>
                  <p>avg {item.averageEmotionIntensity ?? "-"}</p>
                </div>
              </div>
            ))}

            {!isLoading && (weeklyTrend?.items.length ?? 0) === 0 ? (
              <div className="rounded-2xl border border-dashed border-[var(--line-soft)] bg-white px-4 py-5 text-sm text-[var(--text-muted)]">
                선택한 기간의 기록이 없습니다.
              </div>
            ) : null}
          </div>
        </ScreenCard>

        <ScreenCard
          title="월간 위험도 추이"
          description="중위험과 고위험 건수를 날짜별 막대 형태로 보여줍니다."
        >
          <div className="scrollbar-soft space-y-3 xl:max-h-[560px] xl:overflow-y-auto xl:pr-2">
            {(monthlyRiskTrend?.items ?? []).map((item) => (
              <div key={item.date} className="rounded-2xl border border-[var(--line-soft)] bg-white p-4">
                <div className="mb-3 flex items-center justify-between gap-3 text-sm">
                  <p className="font-semibold text-[var(--text-strong)]">{item.date}</p>
                  <p className="text-xs text-[var(--text-muted)]">
                    medium {item.mediumCount} / high {item.highCount}
                  </p>
                </div>

                <div className="space-y-2">
                  <div>
                    <div className="mb-1 flex items-center justify-between text-[11px] text-[var(--text-muted)]">
                      <span>중위험</span>
                      <span>{item.mediumCount}</span>
                    </div>
                    <div className="h-3 rounded-full bg-[#f2eee8]">
                      <div
                        className="h-3 rounded-full bg-[var(--supportive-text)]"
                        style={{ width: `${Math.min(item.mediumCount * 24, 100)}%` }}
                      />
                    </div>
                  </div>

                  <div>
                    <div className="mb-1 flex items-center justify-between text-[11px] text-[var(--text-muted)]">
                      <span>고위험</span>
                      <span>{item.highCount}</span>
                    </div>
                    <div className="h-3 rounded-full bg-[#f6ebe5]">
                      <div
                        className="h-3 rounded-full bg-[var(--safety-text)]"
                        style={{ width: `${Math.min(item.highCount * 36, 100)}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            ))}

            {!isLoading && (monthlyRiskTrend?.items.length ?? 0) === 0 ? (
              <div className="rounded-2xl border border-dashed border-[var(--line-soft)] bg-white px-4 py-5 text-sm text-[var(--text-muted)]">
                이번 달 위험도 기록이 없습니다.
              </div>
            ) : null}
          </div>
        </ScreenCard>
      </div>

      <ScreenCard
        title="대표 감정"
        description="월간 요약 응답의 상위 감정 빈도를 그대로 보여줍니다."
      >
        <div className="flex flex-wrap gap-3">
          {(monthlySummary?.topPrimaryEmotions ?? []).map((emotion) => (
            <div
              key={emotion.emotion}
              className="rounded-2xl bg-[var(--accent-soft)] px-4 py-3 text-sm font-medium text-[var(--text-strong)]"
            >
              {emotion.emotion} / {emotion.count}회
            </div>
          ))}

          {!isLoading && (monthlySummary?.topPrimaryEmotions.length ?? 0) === 0 ? (
            <span className="text-sm text-[var(--text-muted)]">대표 감정 데이터가 없습니다.</span>
          ) : null}
        </div>
      </ScreenCard>
    </div>
  );
}
