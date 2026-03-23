// 화면 섹션을 카드 형태로 감싸는 공통 UI 컴포넌트다.
import { ReactNode } from "react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type ScreenCardProps = {
  title: string;
  description?: string;
  children: ReactNode;
};

export function ScreenCard({ title, description, children }: ScreenCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  );
}
