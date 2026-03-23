"use client";

// 현재 로그인 사용자와 세션 액션 메뉴를 상단에 보여주는 요약 카드다.
import { useEffect, useMemo, useRef, useState, useTransition } from "react";

import { useAuth } from "@/components/auth/auth-provider";

export function SessionSummary() {
  const { status, user, logout, refreshSession } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isRefreshing, startTransition] = useTransition();
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    }

    if (isMenuOpen) {
      window.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      window.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isMenuOpen]);

  const helperText = useMemo(() => {
    if (status === "loading") {
      return "인증 상태를 확인하고 있습니다.";
    }

    if (status === "authenticated" && user) {
      return "현재 세션이 유효하며 보호 화면에 접근할 수 있습니다.";
    }

    return "로그인이 필요합니다.";
  }, [status, user]);

  const statusLabel =
    status === "authenticated" ? "로그인됨" : status === "loading" ? "확인 중" : "로그인 필요";

  const handleRefresh = () => {
    setFeedbackMessage(null);

    startTransition(async () => {
      try {
        await refreshSession();
        setFeedbackMessage("세션 정보를 새로 불러왔습니다.");
      } catch {
        setFeedbackMessage("세션을 새로고침하지 못했습니다.");
      }
    });
  };

  return (
    <aside className="rounded-[28px] border border-[var(--line-soft)] bg-[var(--surface)] p-5 shadow-[var(--panel-shadow)]">
      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--text-muted)]">
        Session
      </p>
      <div className="mt-4 space-y-3">
        <div ref={menuRef} className="relative">
          <button
            type="button"
            onClick={() => setIsMenuOpen((value) => !value)}
            className="w-full rounded-2xl border border-[var(--line-soft)] px-4 py-3 text-left transition hover:bg-[var(--accent-soft)]"
          >
            <p className="text-sm font-semibold text-[var(--text-strong)]">
              {user?.nickname ?? "비로그인 상태"}
            </p>
            <p className="mt-1 text-sm leading-6 text-[var(--text-muted)]">{helperText}</p>
          </button>

          {isMenuOpen ? (
            <div className="absolute right-0 top-[calc(100%+8px)] z-10 w-full rounded-2xl border border-[var(--line-soft)] bg-[var(--surface)] p-2 shadow-[var(--panel-shadow)]">
              <div className="rounded-xl px-3 py-2 text-xs leading-5 text-[var(--text-muted)]">
                <p>이메일: {user?.email ?? "-"}</p>
                <p>응답 모드: {user?.settings.responseMode ?? "-"}</p>
              </div>
              <button
                type="button"
                onClick={handleRefresh}
                disabled={status !== "authenticated" || isRefreshing}
                className="mt-1 w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-[var(--text-strong)] transition hover:bg-[var(--accent-soft)] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isRefreshing ? "세션 새로고침 중..." : "세션 새로고침"}
              </button>
              <button
                type="button"
                onClick={logout}
                disabled={status !== "authenticated"}
                className="mt-1 w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-[var(--safety-text)] transition hover:bg-[var(--safety-soft)] disabled:cursor-not-allowed disabled:opacity-50"
              >
                로그아웃
              </button>
            </div>
          ) : null}
        </div>

        <div className="rounded-2xl bg-[var(--accent-soft)] px-4 py-3 text-xs leading-6 text-[var(--text-muted)]">
          상태: <span className="font-semibold text-[var(--text-strong)]">{statusLabel}</span>
        </div>

        {feedbackMessage ? (
          <div className="rounded-2xl bg-[var(--supportive-soft)] px-4 py-3 text-xs leading-6 text-[var(--supportive-text)]">
            {feedbackMessage}
          </div>
        ) : null}
      </div>
    </aside>
  );
}
