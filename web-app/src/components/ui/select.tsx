// shadcn 스타일의 공통 선택 입력 컴포넌트다.
import { forwardRef, SelectHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        "flex h-11 w-full rounded-2xl border border-[var(--line-soft)] bg-white px-4 py-3 text-sm text-[var(--text-strong)] outline-none transition focus-visible:ring-2 focus-visible:ring-[var(--accent-strong)] focus-visible:ring-offset-2",
        className
      )}
      {...props}
    >
      {children}
    </select>
  )
);

Select.displayName = "Select";
