// 상태 표시용 배지를 일관된 톤으로 보여주는 공통 컴포넌트다.
import { ReactNode } from "react";

import { cn } from "@/lib/utils";

type StatusPillProps = {
  tone?: "default" | "supportive" | "safety";
  children: ReactNode;
};

const toneClassMap = {
  default: "bg-[var(--accent-soft)] text-[var(--text-strong)]",
  supportive: "bg-[var(--supportive-soft)] text-[var(--supportive-text)]",
  safety: "bg-[var(--safety-soft)] text-[var(--safety-text)]"
};

export function StatusPill({ tone = "default", children }: StatusPillProps) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-[0.02em]",
        toneClassMap[tone]
      )}
    >
      {children}
    </span>
  );
}
