// 리포트 요약과 감정 추이를 보여주는 페이지다.
import { AppShell } from "@/components/layout/app-shell";
import { ReportDashboard } from "@/components/report/report-dashboard";

export default function ReportPage() {
  return (
    <AppShell
      title="리포트"
      description="월간 요약과 감정 및 위험도 추이를 실제 backend-api 응답으로 보여줍니다."
    >
      <ReportDashboard />
    </AppShell>
  );
}
