// shadcn 스타일의 공통 텍스트영역 컴포넌트다.
import { forwardRef, TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        "flex min-h-[120px] w-full rounded-2xl border border-[var(--line-soft)] bg-white px-4 py-3 text-sm leading-6 text-[var(--text-strong)] outline-none transition placeholder:text-[var(--text-muted)] focus-visible:ring-2 focus-visible:ring-[var(--accent-strong)] focus-visible:ring-offset-2",
        className
      )}
      {...props}
    />
  )
);

Textarea.displayName = "Textarea";
