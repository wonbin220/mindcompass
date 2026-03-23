"use client";

// 로그인 폼 입력과 backend-api 호출을 담당하는 클라이언트 컴포넌트다.
import { FormEvent, useMemo, useState, useTransition } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { useAuth } from "@/components/auth/auth-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login } from "@/lib/api/auth";
import { webConfig } from "@/lib/config";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { completeLogin } = useAuth();
  const [isPending, startTransition] = useTransition();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const redirectTarget = useMemo(() => searchParams.get("redirect") || "/calendar", [searchParams]);
  const isSubmitDisabled =
    isPending || email.trim().length === 0 || password.trim().length === 0;

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    startTransition(async () => {
      try {
        const result = await login({
          email: email.trim(),
          password
        });

        await completeLogin(result);

        setSuccessMessage(`${result.user.nickname}님으로 로그인되었습니다.`);
        router.replace(redirectTarget);
      } catch (error) {
        setErrorMessage(
          error instanceof Error ? error.message : "로그인 처리 중 알 수 없는 오류가 발생했습니다."
        );
      }
    });
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      <div>
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          type="email"
          placeholder="test@example.com"
        />
      </div>

      <div>
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          type="password"
          placeholder="password123!"
        />
      </div>

      <Button type="submit" disabled={isSubmitDisabled} className="w-full">
        {isPending ? "로그인 중..." : "로그인"}
      </Button>

      <div className="rounded-2xl bg-[var(--accent-soft)] px-4 py-3 text-xs leading-6 text-[var(--text-muted)]">
        backend-api: {webConfig.backendApiBaseUrl}
      </div>

      {redirectTarget !== "/calendar" ? (
        <div className="rounded-2xl bg-[var(--accent-soft)] px-4 py-3 text-sm text-[var(--text-muted)]">
          로그인 후 원래 보려던 화면으로 이동합니다.
        </div>
      ) : null}

      {errorMessage ? (
        <div className="rounded-2xl bg-[var(--safety-soft)] px-4 py-3 text-sm text-[var(--safety-text)]">
          {errorMessage}
        </div>
      ) : null}

      {successMessage ? (
        <div className="rounded-2xl bg-[var(--supportive-soft)] px-4 py-3 text-sm text-[var(--supportive-text)]">
          {successMessage}
        </div>
      ) : null}
    </form>
  );
}
