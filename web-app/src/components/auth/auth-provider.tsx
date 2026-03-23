"use client";

// 전역 인증 상태와 보호 라우트 처리를 담당하는 컨텍스트 컴포넌트다.
import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import { type LoginResponse } from "@/lib/api/auth";
import { getMe, type UserMeResponse } from "@/lib/api/user";
import { clearTokens, readAccessToken, saveTokens } from "@/lib/auth/token-storage";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  status: AuthStatus;
  user: UserMeResponse | null;
  completeLogin: (response: LoginResponse) => Promise<void>;
  logout: () => void;
  refreshSession: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const protectedPrefixes = ["/calendar", "/diary", "/chat", "/report"];
const authPages = ["/login"];

function isProtectedPath(pathname: string) {
  return protectedPrefixes.some((prefix) => pathname.startsWith(prefix));
}

function isAuthPage(pathname: string) {
  return authPages.some((prefix) => pathname.startsWith(prefix));
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<UserMeResponse | null>(null);
  const [isBootstrapped, setIsBootstrapped] = useState(false);

  const restoreSession = useCallback(async () => {
    const accessToken = readAccessToken();

    if (!accessToken) {
      setUser(null);
      setStatus("unauthenticated");
      return;
    }

    setStatus("loading");

    try {
      const me = await getMe();
      setUser(me);
      setStatus("authenticated");
    } catch {
      clearTokens();
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      await restoreSession();

      if (!cancelled) {
        setIsBootstrapped(true);
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [restoreSession]);

  useEffect(() => {
    if (!isBootstrapped) {
      return;
    }

    if (status === "unauthenticated" && isProtectedPath(pathname)) {
      const redirectPath = encodeURIComponent(pathname);
      router.replace(`/login?redirect=${redirectPath}`);
      return;
    }

    if (status === "authenticated" && isAuthPage(pathname)) {
      const redirectTarget = searchParams.get("redirect");
      router.replace(redirectTarget || "/calendar");
      return;
    }

    if (status === "authenticated" && pathname === "/") {
      router.replace("/calendar");
      return;
    }

    if (status === "unauthenticated" && pathname === "/") {
      router.replace("/login");
    }
  }, [isBootstrapped, pathname, router, searchParams, status]);

  const completeLogin = useCallback(
    async (response: LoginResponse) => {
      saveTokens({
        accessToken: response.accessToken,
        refreshToken: response.refreshToken
      });

      await restoreSession();
    },
    [restoreSession]
  );

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
    setStatus("unauthenticated");
    router.replace("/login");
  }, [router]);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      completeLogin,
      logout,
      refreshSession: restoreSession
    }),
    [completeLogin, logout, restoreSession, status, user]
  );

  if (!isBootstrapped && (isProtectedPath(pathname) || pathname === "/")) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--bg)] px-6">
        <div className="rounded-3xl border border-[var(--line-soft)] bg-[var(--surface)] px-6 py-5 text-sm text-[var(--text-muted)] shadow-[var(--panel-shadow)]">
          인증 상태를 확인하고 있습니다...
        </div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
