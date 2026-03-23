// shadcn 스타일의 공통 라벨 컴포넌트다.
import { LabelHTMLAttributes, forwardRef } from "react";

import { cn } from "@/lib/utils";

export const Label = forwardRef<HTMLLabelElement, LabelHTMLAttributes<HTMLLabelElement>>(
  ({ className, ...props }, ref) => (
    <label
      ref={ref}
      className={cn(
        "mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-[var(--text-muted)]",
        className
      )}
      {...props}
    />
  )
);

Label.displayName = "Label";
