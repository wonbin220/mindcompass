"use client";

// 웹 앱의 전역 네비게이션과 현재 위치 하이라이트를 제공하는 컴포넌트다.
import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { href: "/calendar", label: "캘린더" },
  { href: "/diary", label: "일기" },
  { href: "/chat", label: "채팅" },
  { href: "/report", label: "리포트" }
];

type SideNavProps = {
  compact?: boolean;
};

export function SideNav({ compact = false }: SideNavProps) {
  const pathname = usePathname();

  if (compact) {
    return (
      <nav className="flex gap-2 overflow-x-auto pb-1">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition ${
                isActive
                  ? "bg-[var(--accent-strong)] text-white"
                  : "border border-[var(--line-soft)] bg-[var(--surface)] text-[var(--text-muted)]"
              }`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
    );
  }

  return (
    <aside className="rounded-3xl border border-[var(--line-soft)] bg-[var(--surface)] p-5 shadow-[var(--panel-shadow)]">
      <div className="mb-6">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--text-muted)]">
          Mind Compass
        </p>
        <h2 className="mt-2 text-xl font-semibold text-[var(--text-strong)]">웹 클라이언트</h2>
      </div>
      <nav className="space-y-2">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`block rounded-2xl px-4 py-3 text-sm font-medium transition ${
                isActive
                  ? "bg-[var(--accent-soft)] text-[var(--text-strong)]"
                  : "text-[var(--text-muted)] hover:bg-[var(--accent-soft)] hover:text-[var(--text-strong)]"
              }`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
