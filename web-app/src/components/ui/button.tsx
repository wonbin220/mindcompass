// shadcn 스타일의 공통 버튼 컴포넌트다.
import { ButtonHTMLAttributes, forwardRef } from "react";

import { cn } from "@/lib/utils";

type ButtonVariant = "default" | "outline" | "ghost" | "destructive";
type ButtonSize = "default" | "sm" | "lg";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const variantClassMap: Record<ButtonVariant, string> = {
  default:
    "bg-[var(--accent-strong)] text-white hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-50",
  outline:
    "border border-[var(--line-soft)] bg-white text-[var(--text-strong)] hover:bg-[var(--accent-soft)] disabled:cursor-not-allowed disabled:opacity-50",
  ghost:
    "bg-transparent text-[var(--text-strong)] hover:bg-[var(--accent-soft)] disabled:cursor-not-allowed disabled:opacity-50",
  destructive:
    "bg-[var(--safety-soft)] text-[var(--safety-text)] hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-50"
};

const sizeClassMap: Record<ButtonSize, string> = {
  default: "h-11 px-4 py-2 text-sm",
  sm: "h-9 px-3 text-sm",
  lg: "h-12 px-5 text-base"
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "default", size = "default", type = "button", ...props }, ref) => {
    return (
      <button
        ref={ref}
        type={type}
        className={cn(
          "inline-flex items-center justify-center rounded-2xl font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-strong)] focus-visible:ring-offset-2",
          variantClassMap[variant],
          sizeClassMap[size],
          className
        )}
        {...props}
      />
    );
  }
);

Button.displayName = "Button";
