---
name: generate-erd-and-sql
description: Design or update the Mind Compass MVP database schema, ERD, and PostgreSQL SQL draft. Use this when defining tables, relationships, constraints, and teaching why each table exists and which APIs depend on it.
---

# Generate ERD and SQL Skill

This skill is for the `mind-compass` backend schema design workflow.

Use this skill when the task involves:
- defining MVP DB schema
- creating or updating ERD
- writing PostgreSQL DDL / Flyway draft
- explaining why tables exist
- mapping tables to Auth / Diary / Calendar / Chat / Safety flows

Do not use this skill for:
- frontend work
- isolated DTO-only changes with no schema impact
- ai-api prompt tuning without schema changes

## Project assumptions

The repository uses a 2-server architecture, but the primary business data lives in Spring Boot persistence.

Main schema areas:
- users / user settings / refresh tokens
- diaries / diary emotions / diary AI analyses
- chat sessions / chat messages / AI response logs
- safety events
- monthly reports

## Schema design principles

- Start with MVP, not maximum complexity
- Keep Spring Boot as the source of truth for core business records
- Separate user-entered data from AI-generated analysis where useful
- Prefer clear ownership via `user_id`
- Make Chat and Safety flows traceable
- Keep future expansion possible without overdesign

## What this skill should produce

For each schema task, produce:

1. Goal
2. Design principles used
3. Table list
4. Relationship summary
5. Table-by-table explanation
6. PostgreSQL SQL draft
7. Index / constraint notes
8. Suggested implementation priority
9. Next step

## Required explanation style

For each table, explain:
- why the table exists
- what it stores
- which APIs/features depend on it
- what its main foreign keys are
- why it should or should not be separated from another table

## Minimum MVP entities to consider

- `users`
- `user_settings`
- `refresh_tokens`
- `diaries`
- `diary_emotions`
- `diary_ai_analyses`
- `chat_sessions`
- `chat_messages`
- `ai_response_logs`
- `safety_events`
- `monthly_reports`

## Relationship reminders

- `users` owns most user-facing records
- `diaries` may feed `chat_sessions`
- `chat_sessions` contain many `chat_messages`
- `diaries` may have one AI analysis and many emotion tags
- `safety_events` may be linked to a diary or chat session
- `monthly_reports` summarize user records

## SQL rules

- Prefer PostgreSQL-friendly types
- Add PK / FK / UNIQUE / CHECK constraints when useful
- Include practical indexes
- Use `JSONB` only for flexible AI payloads or report payloads
- Avoid overusing JSONB where structured columns are clearer

## Output template

```md
# <Schema Task Name>

## 1. Goal

## 2. Design principles

## 3. Table list

## 4. Relationship summary

## 5. Table explanations

## 6. SQL draft

## 7. Index / constraint notes

## 8. Implementation priority

## 9. Next step
```

## Good example tasks

- Design Mind Compass MVP schema
- Create ERD for Auth + Diary + Chat
- Write initial PostgreSQL DDL for Flyway
- Explain why `diary_ai_analyses` should be separate from `diaries`
- Add Safety Net schema support
