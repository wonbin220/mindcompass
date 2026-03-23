// 웹 프론트 전체 메타데이터와 전역 스타일, 인증 컨텍스트를 연결하는 루트 레이아웃이다.
import type { Metadata } from "next";

import { AuthProvider } from "@/components/auth/auth-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mind Compass Web",
  description: "Mind Compass web client scaffold"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
