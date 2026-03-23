// 웹 앱의 공통 레이아웃과 상단 헤더 영역을 제공하는 컴포넌트다.
import { ReactNode } from "react";

import { SessionSummary } from "@/components/layout/session-summary";
import { SideNav } from "@/components/layout/side-nav";

type AppShellProps = {
  title: string;
  description: string;
  children: ReactNode;
};

export function AppShell({ title, description, children }: AppShellProps) {
  return (
    <div className="min-h-screen bg-[var(--bg)]">
      <div className="mx-auto grid min-h-screen max-w-[1440px] gap-6 px-4 py-6 md:grid-cols-[240px_1fr] md:px-6 xl:px-10">
        <div className="hidden md:block">
          <div className="sticky top-6">
            <SideNav />
          </div>
        </div>
        <main className="space-y-6">
          <div className="md:hidden">
            <SideNav compact />
          </div>
          <div className="grid gap-4 xl:grid-cols-[1fr_280px]">
            <header className="rounded-[28px] border border-[var(--line-soft)] bg-[var(--hero)] p-6 shadow-[var(--panel-shadow)]">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--text-muted)]">
                Mind Compass Web
              </p>
              <h1 className="mt-3 text-3xl font-semibold tracking-tight text-[var(--text-strong)]">
                {title}
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--text-muted)]">
                {description}
              </p>
            </header>
            <SessionSummary />
          </div>
          <section className="space-y-6">{children}</section>
        </main>
      </div>
    </div>
  );
}
