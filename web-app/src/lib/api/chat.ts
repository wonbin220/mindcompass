// 채팅 세션과 메시지 API를 웹 프론트에서 호출하는 클라이언트 파일이다.
import { apiGet, apiPost } from "@/lib/api/http";

export type ChatSessionResponse = {
  sessionId: number;
  title: string;
  sourceDiaryId: number | null;
  createdAt: string;
  updatedAt: string;
};

export type ChatSessionListResponse = {
  count: number;
  sessions: ChatSessionResponse[];
};

export type ChatMessageResponse = {
  messageId: number;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
};

export type ChatDetailResponse = {
  sessionId: number;
  title: string;
  sourceDiaryId: number | null;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessageResponse[];
};

export type CreateChatSessionRequest = {
  title: string;
  sourceDiaryId?: number | null;
};

export type SendChatMessageRequest = {
  message: string;
};

export type SendChatMessageResponse = {
  sessionId: number;
  userMessageId: number;
  assistantMessageId: number;
  assistantReply: string;
  responseType: string;
};

export async function createChatSession(request: CreateChatSessionRequest) {
  return apiPost<CreateChatSessionRequest, ChatSessionResponse>("/api/v1/chat/sessions", request);
}

export async function getChatSessions() {
  return apiGet<ChatSessionListResponse>("/api/v1/chat/sessions");
}

export async function getChatSessionDetail(sessionId: number) {
  return apiGet<ChatDetailResponse>(`/api/v1/chat/sessions/${sessionId}`);
}

export async function sendChatMessage(sessionId: number, request: SendChatMessageRequest) {
  return apiPost<SendChatMessageRequest, SendChatMessageResponse>(
    `/api/v1/chat/sessions/${sessionId}/messages`,
    request
  );
}
