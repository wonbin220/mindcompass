// 웹 로그인 화면을 렌더링하는 페이지다.
import { LoginForm } from "@/components/auth/login-form";
import { AppShell } from "@/components/layout/app-shell";
import { ScreenCard } from "@/components/ui/screen-card";

export default function LoginPage() {
  return (
    <AppShell
      title="로그인"
      description="backend-api의 JWT 로그인 API와 연결해 웹에서도 같은 인증 흐름을 사용합니다."
    >
      <div className="mx-auto max-w-xl">
        <ScreenCard
          title="계정 로그인"
          description="`POST /api/v1/auth/login` 호출 후 access token과 refresh token을 브라우저에 저장합니다."
        >
          <LoginForm />
        </ScreenCard>
      </div>
    </AppShell>
  );
}
