"use client";

// 채팅 세션 생성, 목록 조회, 상세 조회, 메시지 전송을 화면에서 처리하는 컴포넌트다.
import { FormEvent, useEffect, useMemo, useState, useTransition } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScreenCard } from "@/components/ui/screen-card";
import { StatusPill } from "@/components/ui/status-pill";
import { Textarea } from "@/components/ui/textarea";
import {
  createChatSession,
  getChatSessionDetail,
  getChatSessions,
  sendChatMessage,
  type ChatDetailResponse,
  type ChatSessionResponse
} from "@/lib/api/chat";

function toneFromResponseType(responseType: string | null) {
  if (responseType === "SAFETY") {
    return "safety" as const;
  }

  if (responseType === "SUPPORTIVE") {
    return "supportive" as const;
  }

  return "default" as const;
}

export function ChatWorkspace() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [sessionTitle, setSessionTitle] = useState("오늘 감정 상담");
  const [messageInput, setMessageInput] = useState("");
  const [sessions, setSessions] = useState<ChatSessionResponse[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [selectedSession, setSelectedSession] = useState<ChatDetailResponse | null>(null);
  const [lastResponseType, setLastResponseType] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const selectedSessionTitle = useMemo(() => {
    return selectedSession?.title ?? "세션을 선택하거나 새로 만들어 주세요.";
  }, [selectedSession]);

  useEffect(() => {
    let cancelled = false;

    async function loadSessions() {
      try {
        const response = await getChatSessions();

        if (cancelled) {
          return;
        }

        setSessions(response.sessions);

        if (response.sessions.length > 0) {
          setSelectedSessionId(response.sessions[0].sessionId);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message =
          error instanceof Error ? error.message : "채팅 세션을 불러오지 못했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      }
    }

    void loadSessions();

    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    let cancelled = false;

    async function loadSessionDetail() {
      if (!selectedSessionId) {
        setSelectedSession(null);
        return;
      }

      try {
        const response = await getChatSessionDetail(selectedSessionId);

        if (!cancelled) {
          setSelectedSession(response);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message =
          error instanceof Error ? error.message : "채팅 상세를 불러오지 못했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      }
    }

    void loadSessionDetail();

    return () => {
      cancelled = true;
    };
  }, [router, selectedSessionId]);

  const handleCreateSession = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    startTransition(async () => {
      try {
        const created = await createChatSession({
          title: sessionTitle.trim()
        });

        const refreshed = await getChatSessions();
        setSessions(refreshed.sessions);
        setSelectedSessionId(created.sessionId);
        setSessionTitle("오늘 감정 상담");
      } catch (error) {
        const message =
          error instanceof Error ? error.message : "채팅 세션을 생성하지 못했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      }
    });
  };

  const handleSendMessage = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedSessionId) {
      setErrorMessage("먼저 상담 세션을 만들어 주세요.");
      return;
    }

    setErrorMessage(null);

    startTransition(async () => {
      try {
        const response = await sendChatMessage(selectedSessionId, {
          message: messageInput.trim()
        });

        const detail = await getChatSessionDetail(selectedSessionId);
        setSelectedSession(detail);
        setLastResponseType(response.responseType);
        setMessageInput("");

        const refreshed = await getChatSessions();
        setSessions(refreshed.sessions);
      } catch (error) {
        const message =
          error instanceof Error ? error.message : "메시지를 전송하지 못했습니다.";
        setErrorMessage(message);

        if (message.includes("로그인") || message.includes("세션")) {
          router.replace("/login");
        }
      }
    });
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[320px_1fr]">
      <ScreenCard title="세션 목록" description="세션 생성과 목록 조회를 함께 처리합니다.">
        <form className="mb-4 space-y-3" onSubmit={handleCreateSession}>
          <Input
            value={sessionTitle}
            onChange={(event) => setSessionTitle(event.target.value)}
            placeholder="새 상담 세션 제목"
          />
          <Button
            type="submit"
            disabled={isPending || sessionTitle.trim().length === 0}
            className="w-full"
          >
            새 세션 만들기
          </Button>
        </form>

        <div className="space-y-3">
          {sessions.map((session) => (
            <Button
              key={session.sessionId}
              type="button"
              variant={selectedSessionId === session.sessionId ? "default" : "outline"}
              onClick={() => setSelectedSessionId(session.sessionId)}
              className="block h-auto w-full rounded-2xl px-4 py-3 text-left"
            >
              <p className="font-semibold">{session.title}</p>
              <p className="mt-1 text-xs opacity-80">
                {new Date(session.updatedAt).toLocaleString()}
              </p>
            </Button>
          ))}
          {sessions.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-[var(--line-soft)] bg-white px-4 py-5 text-sm text-[var(--text-muted)]">
              아직 생성한 상담 세션이 없습니다.
            </div>
          ) : null}
        </div>
      </ScreenCard>

      <ScreenCard
        title="대화방"
        description="세션 상세와 메시지 전송 흐름을 같은 화면에서 처리합니다."
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-[var(--text-strong)]">
                {selectedSessionTitle}
              </h3>
              {selectedSession?.sourceDiaryId ? (
                <p className="mt-1 text-xs text-[var(--text-muted)]">
                  sourceDiaryId: {selectedSession.sourceDiaryId}
                </p>
              ) : null}
            </div>
            <StatusPill tone={toneFromResponseType(lastResponseType)}>
              {lastResponseType ?? "NORMAL"}
            </StatusPill>
          </div>

          {errorMessage ? (
            <div className="rounded-2xl bg-[var(--safety-soft)] px-4 py-3 text-sm text-[var(--safety-text)]">
              {errorMessage}
            </div>
          ) : null}

          <div className="space-y-3 rounded-3xl border border-[var(--line-soft)] bg-[var(--bg)] p-4">
            {selectedSession?.messages.length ? (
              selectedSession.messages.map((message) => (
                <div
                  key={message.messageId}
                  className={`max-w-xl rounded-[24px] p-4 text-sm leading-6 ${
                    message.role === "USER"
                      ? "bg-[var(--accent-soft)] text-[var(--text-strong)]"
                      : "ml-auto bg-[var(--supportive-soft)] text-[var(--supportive-text)]"
                  }`}
                >
                  <p>{message.content}</p>
                  <p className="mt-2 text-[11px] opacity-70">
                    {new Date(message.createdAt).toLocaleString()}
                  </p>
                </div>
              ))
            ) : (
              <div className="rounded-2xl bg-white px-4 py-5 text-sm text-[var(--text-muted)]">
                먼저 메시지를 보내 상담을 시작해 주세요.
              </div>
            )}
          </div>

          <form className="space-y-3" onSubmit={handleSendMessage}>
            <Textarea
              value={messageInput}
              onChange={(event) => setMessageInput(event.target.value)}
              rows={4}
              placeholder="지금 느끼는 감정이나 상황을 적어주세요."
            />
            <Button
              type="submit"
              disabled={isPending || messageInput.trim().length === 0 || !selectedSessionId}
              className="w-full"
            >
              {isPending ? "전송 중..." : "메시지 보내기"}
            </Button>
          </form>
        </div>
      </ScreenCard>
    </div>
  );
}
