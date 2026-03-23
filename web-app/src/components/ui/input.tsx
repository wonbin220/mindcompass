// shadcn 스타일의 공통 입력창 컴포넌트다.
import { forwardRef, InputHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "flex h-11 w-full rounded-2xl border border-[var(--line-soft)] bg-white px-4 py-3 text-sm text-[var(--text-strong)] outline-none transition placeholder:text-[var(--text-muted)] focus-visible:ring-2 focus-visible:ring-[var(--accent-strong)] focus-visible:ring-offset-2",
        className
      )}
      {...props}
    />
  )
);

Input.displayName = "Input";
