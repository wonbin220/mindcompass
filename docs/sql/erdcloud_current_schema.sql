-- Mind Compass 현재 backend-api 스키마를 ERDCloud에 넣기 위한 통합 DDL이다.

create table users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    nickname varchar(50) not null,
    status varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted_at timestamp null
);

comment on table users is '서비스의 사용자 계정 기준 테이블';
comment on column users.id is '사용자 ID';
comment on column users.email is '로그인 이메일';
comment on column users.password_hash is '암호화된 비밀번호';
comment on column users.nickname is '닉네임';
comment on column users.status is '사용자 상태';
comment on column users.created_at is '생성 시각';
comment on column users.updated_at is '수정 시각';
comment on column users.deleted_at is '소프트 삭제 시각';

create table user_settings (
    id bigserial primary key,
    user_id bigint not null unique,
    app_lock_enabled boolean not null default false,
    notification_enabled boolean not null default true,
    daily_reminder_time time null,
    response_mode varchar(30) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_user_settings_user
        foreign key (user_id) references users(id)
);

comment on table user_settings is '앱 잠금, 알림, 응답 모드 등 사용자 설정 테이블';
comment on column user_settings.id is '설정 ID';
comment on column user_settings.user_id is '사용자 ID';
comment on column user_settings.app_lock_enabled is '앱 잠금 사용 여부';
comment on column user_settings.notification_enabled is '알림 사용 여부';
comment on column user_settings.daily_reminder_time is '일일 리마인더 시간';
comment on column user_settings.response_mode is 'AI 응답 모드';
comment on column user_settings.created_at is '생성 시각';
comment on column user_settings.updated_at is '수정 시각';

create table refresh_tokens (
    id bigserial primary key,
    user_id bigint not null,
    token_hash varchar(255) not null,
    expires_at timestamp not null,
    revoked_at timestamp null,
    created_at timestamp not null default current_timestamp,
    constraint fk_refresh_tokens_user
        foreign key (user_id) references users(id)
);

comment on table refresh_tokens is '토큰 재발급을 위한 refresh token 해시 저장 테이블';
comment on column refresh_tokens.id is '토큰 이력 ID';
comment on column refresh_tokens.user_id is '사용자 ID';
comment on column refresh_tokens.token_hash is 'refresh token 해시값';
comment on column refresh_tokens.expires_at is '만료 시각';
comment on column refresh_tokens.revoked_at is '폐기 시각';
comment on column refresh_tokens.created_at is '생성 시각';

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index idx_refresh_tokens_user_hash_active
    on refresh_tokens(user_id, token_hash);

create table diaries (
    id bigserial primary key,
    user_id bigint not null,
    title varchar(100) not null,
    content text not null,
    primary_emotion varchar(30) null,
    emotion_intensity integer null,
    written_at timestamp not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted_at timestamp null,
    constraint fk_diaries_user
        foreign key (user_id) references users(id),
    constraint chk_diaries_emotion_intensity
        check (emotion_intensity is null or emotion_intensity between 1 and 5)
);

comment on table diaries is '사용자가 작성한 감정 일기 본문 테이블';
comment on column diaries.id is '일기 ID';
comment on column diaries.user_id is '작성 사용자 ID';
comment on column diaries.title is '일기 제목';
comment on column diaries.content is '일기 본문';
comment on column diaries.primary_emotion is '대표 감정';
comment on column diaries.emotion_intensity is '감정 강도(1~5)';
comment on column diaries.written_at is '사용자가 기록한 날짜/시간';
comment on column diaries.created_at is '생성 시각';
comment on column diaries.updated_at is '수정 시각';
comment on column diaries.deleted_at is '소프트 삭제 시각';

create index idx_diaries_user_id on diaries(user_id);
create index idx_diaries_user_written_at on diaries(user_id, written_at desc);
create index idx_diaries_user_deleted_at on diaries(user_id, deleted_at);

create table diary_emotions (
    id bigserial primary key,
    diary_id bigint not null,
    emotion_code varchar(30) not null,
    intensity integer null,
    source_type varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_diary_emotions_diary
        foreign key (diary_id) references diaries(id),
    constraint chk_diary_emotions_intensity
        check (intensity is null or intensity between 1 and 5)
);

comment on table diary_emotions is '일기별 감정 태그 테이블';
comment on column diary_emotions.id is '감정 태그 ID';
comment on column diary_emotions.diary_id is '일기 ID';
comment on column diary_emotions.emotion_code is '감정 코드';
comment on column diary_emotions.intensity is '감정 강도(1~5)';
comment on column diary_emotions.source_type is '감정 태그 출처(USER, AI_ANALYSIS)';
comment on column diary_emotions.created_at is '생성 시각';

create index idx_diary_emotions_diary_id on diary_emotions(diary_id);
create index idx_diary_emotions_diary_source on diary_emotions(diary_id, source_type);

create table diary_ai_analyses (
    id bigserial primary key,
    diary_id bigint not null unique,
    primary_emotion varchar(30) null,
    emotion_intensity integer null,
    summary text null,
    confidence numeric(4, 3) null,
    raw_payload text null,
    risk_level varchar(20) null,
    risk_score numeric(4, 3) null,
    risk_signals text null,
    recommended_action varchar(40) null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_diary_ai_analyses_diary
        foreign key (diary_id) references diaries(id),
    constraint chk_diary_ai_analyses_intensity
        check (emotion_intensity is null or emotion_intensity between 1 and 5)
);

comment on table diary_ai_analyses is '일기 AI 분석 및 위험도 결과 저장 테이블';
comment on column diary_ai_analyses.id is 'AI 분석 ID';
comment on column diary_ai_analyses.diary_id is '일기 ID';
comment on column diary_ai_analyses.primary_emotion is 'AI가 해석한 대표 감정';
comment on column diary_ai_analyses.emotion_intensity is 'AI가 해석한 감정 강도';
comment on column diary_ai_analyses.summary is '일기 요약';
comment on column diary_ai_analyses.confidence is '분석 신뢰도';
comment on column diary_ai_analyses.raw_payload is 'ai-api 원본 응답';
comment on column diary_ai_analyses.risk_level is '위험도 수준';
comment on column diary_ai_analyses.risk_score is '위험 점수';
comment on column diary_ai_analyses.risk_signals is '감지된 위험 신호';
comment on column diary_ai_analyses.recommended_action is '권장 액션';
comment on column diary_ai_analyses.created_at is '생성 시각';
comment on column diary_ai_analyses.updated_at is '수정 시각';

create index idx_diary_ai_analyses_diary_id on diary_ai_analyses(diary_id);

create table chat_sessions (
    id bigserial primary key,
    user_id bigint not null,
    source_diary_id bigint null,
    title varchar(100) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_chat_sessions_user
        foreign key (user_id) references users(id),
    constraint fk_chat_sessions_source_diary
        foreign key (source_diary_id) references diaries(id)
);

comment on table chat_sessions is '사용자별 채팅 세션 테이블';
comment on column chat_sessions.id is '세션 ID';
comment on column chat_sessions.user_id is '세션 소유자 ID';
comment on column chat_sessions.source_diary_id is '연결된 원본 일기 ID';
comment on column chat_sessions.title is '세션 제목';
comment on column chat_sessions.created_at is '생성 시각';
comment on column chat_sessions.updated_at is '수정 시각';

create index idx_chat_sessions_user_id on chat_sessions(user_id);
create index idx_chat_sessions_user_updated_at on chat_sessions(user_id, updated_at desc);

create table chat_messages (
    id bigserial primary key,
    session_id bigint not null,
    role varchar(20) not null,
    content text not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_chat_messages_session
        foreign key (session_id) references chat_sessions(id)
);

comment on table chat_messages is '채팅 세션 내 사용자/assistant 메시지 테이블';
comment on column chat_messages.id is '메시지 ID';
comment on column chat_messages.session_id is '세션 ID';
comment on column chat_messages.role is '메시지 역할(USER, ASSISTANT)';
comment on column chat_messages.content is '메시지 본문';
comment on column chat_messages.created_at is '생성 시각';

create index idx_chat_messages_session_id on chat_messages(session_id);
create index idx_chat_messages_session_created_at on chat_messages(session_id, created_at asc);
