# Mind Compass Apidog import용 OpenAPI JSON 생성 스크립트입니다.
import json
from pathlib import Path

IMPLEMENTED = "\uad6c\ud604 API"
PLANNED = "\ubbf8\uad6c\ud604 API"
OUT = Path(__file__).resolve().parent / "MindCompass.apidog.openapi.json"


def body(example):
    return {"required": True, "content": {"application/json": {"example": example}}}


def res(desc, example=None):
    item = {"description": desc}
    if example is not None:
        item["content"] = {"application/json": {"example": example}}
    return item


def op(folder, status, domain, method, path, summary, description, request=None, params=None, security=False, responses=None):
    item = {
        "tags": [folder, status, domain],
        "summary": summary,
        "description": description,
        "operationId": f"{method}_{path.strip('/').replace('/', '_').replace('{', '').replace('}', '')}",
        "responses": responses or {"200": res("OK")},
    }
    if request is not None:
        item["requestBody"] = body(request)
    if params:
        item["parameters"] = params
    if security:
        item["security"] = [{"bearerAuth": []}]
    return path, method, item


spec = {
    "openapi": "3.0.3",
    "info": {
        "title": "Mind Compass API",
        "version": "0.1.0-apidog",
        "description": "Mind Compass backend-api public API spec for Apidog import. Operations are grouped into folder-style tags `구현 API` and `미구현 API`. Status tags use `Implemented` and `Planned`. Planned endpoints include example request/response payloads, but actual server calls may currently fail because those routes are not implemented yet."
    },
    "servers": [{"url": "http://localhost:8080/api/v1", "description": "Local backend-api"}],
    "tags": [
        {"name": IMPLEMENTED, "description": "Implemented public APIs."},
        {"name": PLANNED, "description": "Planned APIs. Not implemented yet."},
        {"name": "Implemented", "description": "Implemented backend-api endpoint."},
        {"name": "Planned", "description": "Planned endpoint. Not implemented yet."},
        {"name": "Authentication"}, {"name": "Users"}, {"name": "Diaries"}, {"name": "Calendar"},
        {"name": "Chat"}, {"name": "Reports"}, {"name": "Personas"}, {"name": "Contents"}, {"name": "Payments"}
    ],
    "x-tagGroups": [
        {"name": "Folders", "tags": [IMPLEMENTED, PLANNED]},
        {"name": "Status", "tags": ["Implemented", "Planned"]},
        {"name": "Domains", "tags": ["Authentication", "Users", "Diaries", "Calendar", "Chat", "Reports", "Personas", "Contents", "Payments"]}
    ],
    "paths": {},
    "components": {
        "securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}}
    }
}

ops = []
ops += [
    op(IMPLEMENTED, "Implemented", "Authentication", "post", "/auth/signup", "Sign up", "Create a new user account.", request={"email": "user@example.com", "password": "Abcd1234!", "nickname": "minduser"}, responses={"201": res("Created", {"userId": 1, "email": "user@example.com", "nickname": "minduser", "createdAt": "2026-03-30T10:00:00"}), "400": res("Bad Request"), "409": res("Conflict")}),
    op(IMPLEMENTED, "Implemented", "Authentication", "post", "/auth/login", "Login", "Authenticate a user and issue access and refresh tokens.", request={"email": "user@example.com", "password": "Abcd1234!"}, responses={"200": res("OK", {"accessToken": "jwt-access-token", "refreshToken": "jwt-refresh-token", "user": {"userId": 1, "nickname": "minduser"}}), "400": res("Bad Request"), "401": res("Unauthorized")}),
    op(IMPLEMENTED, "Implemented", "Authentication", "post", "/auth/refresh", "Refresh token", "Refresh the access token using a refresh token.", request={"refreshToken": "jwt-refresh-token"}, responses={"200": res("OK", {"accessToken": "new-access-token", "refreshToken": "new-refresh-token"}), "401": res("Unauthorized")}),
    op(PLANNED, "Planned", "Authentication", "post", "/auth/logout", "Logout", "Not implemented yet. Planned logout endpoint for invalidating access and refresh tokens.", security=True, responses={"204": res("No Content")}),
    op(PLANNED, "Planned", "Authentication", "post", "/auth/password/reset-request", "Request password reset", "Not implemented yet. Planned endpoint for sending a password reset email or temporary password.", request={"email": "user@example.com"}, responses={"200": res("OK", {"message": "Temporary password sent successfully."})}),
    op(PLANNED, "Planned", "Authentication", "post", "/auth/social/google", "Google social login", "Not implemented yet. Planned Google social login endpoint.", request={"idToken": "google-id-token"}, responses={"200": res("OK", {"accessToken": "jwt-access-token", "refreshToken": "jwt-refresh-token", "user": {"userId": 1, "nickname": "minduser"}})}),
    op(PLANNED, "Planned", "Authentication", "put", "/auth/password", "Change password", "Not implemented yet. Planned endpoint for changing the current user's password.", request={"currentPassword": "Abcd1234!", "newPassword": "Xyz!2345"}, security=True, responses={"200": res("OK", {"message": "Password updated successfully."})}),
    op(IMPLEMENTED, "Implemented", "Users", "get", "/users/me", "Get my profile", "Get the current user's profile and settings.", security=True, responses={"200": res("OK", {"userId": 1, "email": "user@example.com", "nickname": "minduser", "status": "ACTIVE", "settings": {"appLockEnabled": False, "notificationEnabled": True, "dailyReminderTime": "22:00:00", "responseMode": "EMPATHETIC"}}), "403": res("Forbidden")}),
    op(PLANNED, "Planned", "Users", "patch", "/users/me/settings", "Update settings", "Not implemented yet. Planned endpoint for updating user settings.", request={"notificationEnabled": True, "dailyReminderTime": "22:00:00", "responseMode": "EMPATHETIC"}, security=True, responses={"200": res("OK", {"userId": 1, "settings": {"notificationEnabled": True, "dailyReminderTime": "22:00:00", "responseMode": "EMPATHETIC"}})}),
    op(PLANNED, "Planned", "Users", "get", "/users/me/statistics", "Get statistics", "Not implemented yet. Planned endpoint for user-level usage statistics.", security=True, responses={"200": res("OK", {"totalDiaryCount": 12, "totalChatSessionCount": 5, "totalMessageCount": 42})}),
    op(PLANNED, "Planned", "Users", "patch", "/users/me/interests", "Update interests", "Not implemented yet. Planned endpoint for updating user interests.", request={"interests": ["sleep", "anxiety", "self-care"]}, security=True, responses={"200": res("OK", {"interests": ["sleep", "anxiety", "self-care"]})}),
    op(IMPLEMENTED, "Implemented", "Diaries", "post", "/diaries", "Create diary", "Create a diary entry. AI enrichment fields are included in the response.", request={"title": "Evening journal", "content": "I felt tired after work, but a short walk helped me calm down.", "primaryEmotion": "TIRED", "emotionIntensity": 4, "writtenAt": "2026-03-30T21:30:00"}, security=True, responses={"201": res("Created", {"diaryId": 101, "title": "Evening journal", "content": "I felt tired after work, but a short walk helped me calm down.", "primaryEmotion": "TIRED", "emotionIntensity": 4, "writtenAt": "2026-03-30T21:30:00", "riskLevel": "LOW", "recommendedAction": "NORMAL_RESPONSE"}), "400": res("Bad Request"), "403": res("Forbidden")}),
    op(IMPLEMENTED, "Implemented", "Diaries", "get", "/diaries", "Get diaries by date", "Get diary items for a specific date.", params=[{"name": "date", "in": "query", "required": True, "schema": {"type": "string", "format": "date"}, "example": "2026-03-30"}], security=True, responses={"200": res("OK", {"date": "2026-03-30", "items": [{"diaryId": 101, "title": "Evening journal", "primaryEmotion": "TIRED", "emotionIntensity": 4, "writtenAt": "2026-03-30T21:30:00"}]})}),
    op(IMPLEMENTED, "Implemented", "Diaries", "get", "/diaries/{diaryId}", "Get diary detail", "Get diary detail by diary id.", params=[{"name": "diaryId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 101}], security=True, responses={"200": res("OK", {"diaryId": 101, "title": "Evening journal", "content": "I felt tired after work, but a short walk helped me calm down.", "primaryEmotion": "TIRED", "emotionIntensity": 4}), "404": res("Not Found")}),
    op(IMPLEMENTED, "Implemented", "Diaries", "patch", "/diaries/{diaryId}", "Update diary", "Update diary title, content, and emotion fields.", params=[{"name": "diaryId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 101}], request={"title": "Updated evening journal", "content": "It was a hard day, but I felt calmer later.", "primaryEmotion": "CALM", "emotionIntensity": 3}, security=True, responses={"200": res("OK", {"diaryId": 101, "title": "Updated evening journal", "primaryEmotion": "CALM", "emotionIntensity": 3})}),
    op(IMPLEMENTED, "Implemented", "Diaries", "delete", "/diaries/{diaryId}", "Delete diary", "Delete a diary entry by diary id.", params=[{"name": "diaryId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 101}], security=True, responses={"204": res("No Content"), "404": res("Not Found")})
]
ops += [
    op(IMPLEMENTED, "Implemented", "Calendar", "get", "/calendar/monthly-emotions", "Get monthly emotions", "Get monthly emotion calendar data.", params=[{"name": "year", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 2026}, {"name": "month", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 3}], security=True, responses={"200": res("OK", {"year": 2026, "month": 3, "days": [{"date": "2026-03-30", "hasDiary": True, "primaryEmotion": "CALM", "emotionIntensity": 3}]})}),
    op(IMPLEMENTED, "Implemented", "Calendar", "get", "/calendar/daily-summary", "Get daily summary", "Get daily summary for the selected date.", params=[{"name": "date", "in": "query", "required": True, "schema": {"type": "string", "format": "date"}, "example": "2026-03-30"}], security=True, responses={"200": res("OK", {"date": "2026-03-30", "hasDiary": True, "diaryCount": 1, "primaryEmotion": "CALM", "emotionIntensity": 3, "summary": "The user recovered some emotional stability later."})}),
    op(PLANNED, "Planned", "Calendar", "get", "/calendar/emotion-timeline", "Get emotion timeline", "Not implemented yet. Planned endpoint for timeline-style emotion history.", params=[{"name": "startDate", "in": "query", "required": True, "schema": {"type": "string", "format": "date"}, "example": "2026-03-01"}, {"name": "endDate", "in": "query", "required": True, "schema": {"type": "string", "format": "date"}, "example": "2026-03-30"}], security=True, responses={"200": res("OK", {"items": [{"date": "2026-03-30", "primaryEmotion": "CALM", "emotionIntensity": 3}]})}),
    op(IMPLEMENTED, "Implemented", "Chat", "post", "/chat/sessions", "Create chat session", "Create a new chat session.", request={"title": "Today emotion chat", "sourceDiaryId": 101}, security=True, responses={"201": res("Created", {"sessionId": 501, "title": "Today emotion chat", "sourceDiaryId": 101})}),
    op(IMPLEMENTED, "Implemented", "Chat", "get", "/chat/sessions", "Get chat sessions", "Get the current user's chat sessions.", security=True, responses={"200": res("OK", {"items": [{"sessionId": 501, "title": "Today emotion chat", "sourceDiaryId": 101}]})}),
    op(IMPLEMENTED, "Implemented", "Chat", "get", "/chat/sessions/{sessionId}", "Get chat session detail", "Get chat session detail with messages.", params=[{"name": "sessionId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 501}], security=True, responses={"200": res("OK", {"sessionId": 501, "title": "Today emotion chat", "messages": [{"messageId": 1001, "role": "USER", "content": "I felt very anxious today."}, {"messageId": 1002, "role": "ASSISTANT", "content": "That sounds like a heavy day."}]}), "404": res("Not Found")}),
    op(IMPLEMENTED, "Implemented", "Chat", "post", "/chat/sessions/{sessionId}/messages", "Send chat message", "Send a message and receive an AI assistant reply.", params=[{"name": "sessionId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 501}], request={"message": "I felt very anxious today and I do not think I can sleep."}, security=True, responses={"201": res("Created", {"userMessageId": 1003, "assistantMessageId": 1004, "assistantReply": "That sounds overwhelming.", "responseType": "SUPPORTIVE"}), "404": res("Not Found"), "503": res("Service Unavailable")}),
    op(PLANNED, "Planned", "Chat", "get", "/chat/sessions/{sessionId}/messages/history", "Get message history", "Not implemented yet. Planned endpoint for paged message history.", params=[{"name": "sessionId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 501}], security=True, responses={"200": res("OK", {"items": [{"messageId": 1001, "role": "USER", "content": "I felt very anxious today."}], "nextCursor": None})}),
    op(PLANNED, "Planned", "Chat", "get", "/chat/usage/today", "Get today's usage", "Not implemented yet. Planned endpoint for today's chat usage summary.", security=True, responses={"200": res("OK", {"date": "2026-03-30", "messageCount": 8, "sessionCount": 2})}),
    op(IMPLEMENTED, "Implemented", "Reports", "get", "/reports/monthly-summary", "Get monthly summary", "Get monthly report summary.", params=[{"name": "year", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 2026}, {"name": "month", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 3}], security=True, responses={"200": res("OK", {"year": 2026, "month": 3, "diaryCount": 12, "averageEmotionIntensity": 3.2, "riskSummary": {"mediumCount": 1, "highCount": 0}})}),
    op(IMPLEMENTED, "Implemented", "Reports", "get", "/reports/emotions/weekly", "Get weekly emotions", "Get weekly emotion trend data.", params=[{"name": "date", "in": "query", "required": False, "schema": {"type": "string", "format": "date"}, "example": "2026-03-30"}], security=True, responses={"200": res("OK", {"startDate": "2026-03-24", "endDate": "2026-03-30", "items": [{"date": "2026-03-30", "primaryEmotion": "CALM", "emotionIntensity": 3}]})}),
    op(IMPLEMENTED, "Implemented", "Reports", "get", "/reports/risks/monthly", "Get monthly risks", "Get monthly risk trend data.", params=[{"name": "year", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 2026}, {"name": "month", "in": "query", "required": True, "schema": {"type": "integer"}, "example": 3}], security=True, responses={"200": res("OK", {"year": 2026, "month": 3, "items": [{"date": "2026-03-30", "riskLevel": "MEDIUM", "count": 1}]})}),
    op(PLANNED, "Planned", "Reports", "get", "/reports/personas", "Get persona report", "Not implemented yet. Planned endpoint for persona-oriented report summaries.", security=True, responses={"200": res("OK", {"recommendedPersona": {"personaId": 3, "name": "Calm Coach"}, "reasonSummary": "Recent patterns suggest a reflective support persona."})})
]
ops += [
    op(PLANNED, "Planned", "Personas", "get", "/personas", "Get personas", "Not implemented yet. Planned endpoint for persona list.", security=True, responses={"200": res("OK", {"items": [{"personaId": 1, "name": "Gentle Listener", "description": "A warm and empathetic counselor persona."}]})}),
    op(PLANNED, "Planned", "Personas", "get", "/personas/{personaId}", "Get persona detail", "Not implemented yet. Planned endpoint for persona detail.", params=[{"name": "personaId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 1}], security=True, responses={"200": res("OK", {"personaId": 1, "name": "Gentle Listener", "description": "A warm and empathetic counselor persona.", "tone": "warm"})}),
    op(PLANNED, "Planned", "Personas", "get", "/personas/recommendations", "Get persona recommendations", "Not implemented yet. Planned endpoint for user-tailored persona recommendations.", security=True, responses={"200": res("OK", {"items": [{"personaId": 2, "name": "Calm Coach", "matchScore": 0.92}]})}),
    op(PLANNED, "Planned", "Personas", "get", "/personas/recommendations/today", "Get today's persona recommendation", "Not implemented yet. Planned endpoint for today's persona recommendation.", security=True, responses={"200": res("OK", {"personaId": 2, "name": "Calm Coach", "reason": "Recommended based on this week's emotional stability trend."})}),
    op(PLANNED, "Planned", "Contents", "get", "/contents/recommendations", "Get content recommendations", "Not implemented yet. Planned endpoint for persona-based content recommendations.", security=True, responses={"200": res("OK", {"items": [{"contentId": 11, "type": "YOUTUBE", "title": "5 minute calming breathing routine"}]})}),
    op(PLANNED, "Planned", "Contents", "get", "/contents/recommendations/realtime", "Get realtime content recommendations", "Not implemented yet. Planned endpoint for real-time content recommendations based on chat context.", security=True, responses={"200": res("OK", {"items": [{"contentId": 21, "type": "BOOK", "title": "The Anxiety Toolkit", "reason": "Matches the current conversation context."}]})}),
    op(PLANNED, "Planned", "Contents", "post", "/contents/bookmarks", "Create bookmark", "Not implemented yet. Planned endpoint for bookmarking a content item.", request={"contentId": 11}, security=True, responses={"201": res("Created", {"bookmarkId": 301, "contentId": 11, "createdAt": "2026-03-30T22:30:00"})}),
    op(PLANNED, "Planned", "Contents", "get", "/contents/bookmarks", "Get bookmarks", "Not implemented yet. Planned endpoint for bookmark list.", security=True, responses={"200": res("OK", {"items": [{"bookmarkId": 301, "contentId": 11, "title": "5 minute calming breathing routine"}]})}),
    op(PLANNED, "Planned", "Contents", "delete", "/contents/bookmarks/{bookmarkId}", "Delete bookmark", "Not implemented yet. Planned endpoint for deleting a bookmark.", params=[{"name": "bookmarkId", "in": "path", "required": True, "schema": {"type": "integer"}, "example": 301}], security=True, responses={"204": res("No Content")}),
    op(PLANNED, "Planned", "Payments", "post", "/payments/subscription-link", "Create subscription link", "Not implemented yet. Planned endpoint for generating a Polar subscription checkout URL.", security=True, responses={"200": res("OK", {"checkoutUrl": "https://polar.sh/checkout/session/abc123"})}),
    op(PLANNED, "Planned", "Payments", "get", "/payments/subscription-status", "Get subscription status", "Not implemented yet. Planned endpoint for subscription status.", security=True, responses={"200": res("OK", {"status": "ACTIVE", "plan": "PREMIUM_MONTHLY", "renewalDate": "2026-04-30"})}),
    op(PLANNED, "Planned", "Payments", "get", "/payments/customer-portal", "Get customer portal URL", "Not implemented yet. Planned endpoint for customer portal access.", security=True, responses={"200": res("OK", {"portalUrl": "https://polar.sh/customer/portal/abc123"})}),
    op(PLANNED, "Planned", "Payments", "post", "/payments/polar/webhook", "Handle Polar webhook", "Not implemented yet. Planned endpoint for handling Polar webhook events.", request={"type": "subscription.updated", "data": {"subscriptionId": "sub_123", "status": "ACTIVE"}}, responses={"200": res("OK", {"received": True})})
]

for path, method, item in ops:
    spec["paths"].setdefault(path, {})[method] = item

OUT.write_text(json.dumps(spec, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Wrote {OUT}")
