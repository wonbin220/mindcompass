// 채팅 세션과 대화 흐름을 보여주는 페이지다.
import { ChatWorkspace } from "@/components/chat/chat-workspace";
import { AppShell } from "@/components/layout/app-shell";

export default function ChatPage() {
  return (
    <AppShell
      title="채팅"
      description="세션 생성, 세션 목록 조회, 메시지 전송과 응답 상태를 한 화면에서 확인합니다."
    >
      <ChatWorkspace />
    </AppShell>
  );
}
