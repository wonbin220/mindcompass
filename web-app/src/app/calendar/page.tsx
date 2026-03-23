// 캘린더 메인 화면을 렌더링하는 페이지다.
import { CalendarDashboard } from "@/components/calendar/calendar-dashboard";
import { AppShell } from "@/components/layout/app-shell";

export default function CalendarPage() {
  return (
    <AppShell
      title="캘린더"
      description="로그인한 사용자의 월간 감정 캘린더와 선택 날짜의 요약 정보를 함께 보여줍니다."
    >
      <CalendarDashboard />
    </AppShell>
  );
}
