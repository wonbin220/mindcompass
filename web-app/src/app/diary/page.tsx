// 일기 작성과 AI 분석 결과 확인을 위한 페이지다.
import { DiaryWorkspace } from "@/components/diary/diary-workspace";
import { AppShell } from "@/components/layout/app-shell";

export default function DiaryPage() {
  return (
    <AppShell
      title="일기"
      description="일기를 저장한 뒤 상세 정보와 AI 분석 결과를 같은 화면에서 확인합니다."
    >
      <DiaryWorkspace />
    </AppShell>
  );
}
