-- 마인드 컴패스 MVP ERD Cloud 추천 스키마 초안이다.
-- 현재 운영 중인 핵심 테이블 8개와, 바로 다음 단계에서 검토할 추천 테이블 2개를 함께 정리한다.

create table users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    nickname varchar(50) not null,
    status varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted_at timestamp null,
    constraint chk_users_status
        check (status in ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'))
);

comment on table users is '서비스 사용자 계정 기본 테이블';
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
        foreign key (user_id) references users(id),
    constraint chk_user_settings_response_mode
        check (response_mode in ('EMPATHETIC', 'COACHING', 'CALM'))
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
comment on column refresh_tokens.token_hash is 'refresh token 해시';
comment on column refresh_tokens.expires_at is '만료 시각';
comment on column refresh_tokens.revoked_at is '폐기 시각';
comment on column refresh_tokens.created_at is '생성 시각';

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index idx_refresh_tokens_user_hash_active on refresh_tokens(user_id, token_hash);

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

comment on table diaries is '사용자가 작성한 감정 일기 기본 테이블';
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
        check (intensity is null or intensity between 1 and 5),
    constraint chk_diary_emotions_source_type
        check (source_type in ('USER', 'AI_ANALYSIS'))
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
        check (emotion_intensity is null or emotion_intensity between 1 and 5),
    constraint chk_diary_ai_analyses_confidence
        check (confidence is null or confidence between 0 and 1),
    constraint chk_diary_ai_analyses_risk_score
        check (risk_score is null or risk_score between 0 and 1),
    constraint chk_diary_ai_analyses_risk_level
        check (risk_level is null or risk_level in ('LOW', 'MEDIUM', 'HIGH')),
    constraint chk_diary_ai_analyses_recommended_action
        check (recommended_action is null or recommended_action in ('NONE', 'SUPPORTIVE_RESPONSE', 'SAFETY_RESPONSE', 'FALLBACK'))
);

comment on table diary_ai_analyses is '일기 AI 분석 및 위험도 결과 저장 테이블';
comment on column diary_ai_analyses.id is 'AI 분석 ID';
comment on column diary_ai_analyses.diary_id is '일기 ID';
comment on column diary_ai_analyses.primary_emotion is 'AI 해석 대표 감정';
comment on column diary_ai_analyses.emotion_intensity is 'AI 해석 감정 강도';
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
create index idx_diary_ai_analyses_risk_level on diary_ai_analyses(risk_level);

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
create index idx_chat_sessions_source_diary_id on chat_sessions(source_diary_id);

create table chat_messages (
    id bigserial primary key,
    session_id bigint not null,
    role varchar(20) not null,
    content text not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_chat_messages_session
        foreign key (session_id) references chat_sessions(id),
    constraint chk_chat_messages_role
        check (role in ('USER', 'ASSISTANT'))
);

comment on table chat_messages is '채팅 세션 내 사용자와 assistant 메시지 테이블';
comment on column chat_messages.id is '메시지 ID';
comment on column chat_messages.session_id is '세션 ID';
comment on column chat_messages.role is '메시지 역할(USER, ASSISTANT)';
comment on column chat_messages.content is '메시지 본문';
comment on column chat_messages.created_at is '생성 시각';

create index idx_chat_messages_session_id on chat_messages(session_id);
create index idx_chat_messages_session_created_at on chat_messages(session_id, created_at asc);

create table safety_events (
    id bigserial primary key,
    user_id bigint not null,
    diary_id bigint null,
    chat_session_id bigint null,
    chat_message_id bigint null,
    source_type varchar(20) not null,
    risk_level varchar(20) not null,
    risk_score numeric(4, 3) null,
    trigger_signals text null,
    action_taken varchar(40) not null,
    resolved boolean not null default false,
    created_at timestamp not null default current_timestamp,
    resolved_at timestamp null,
    constraint fk_safety_events_user
        foreign key (user_id) references users(id),
    constraint fk_safety_events_diary
        foreign key (diary_id) references diaries(id),
    constraint fk_safety_events_chat_session
        foreign key (chat_session_id) references chat_sessions(id),
    constraint fk_safety_events_chat_message
        foreign key (chat_message_id) references chat_messages(id),
    constraint chk_safety_events_source_type
        check (source_type in ('DIARY', 'CHAT')),
    constraint chk_safety_events_risk_level
        check (risk_level in ('MEDIUM', 'HIGH')),
    constraint chk_safety_events_risk_score
        check (risk_score is null or risk_score between 0 and 1),
    constraint chk_safety_events_action_taken
        check (action_taken in ('SUPPORTIVE_RESPONSE', 'SAFETY_RESPONSE', 'MANUAL_REVIEW', 'NO_ACTION')),
    constraint chk_safety_events_source_ref
        check (
            (source_type = 'DIARY' and diary_id is not null)
            or (source_type = 'CHAT' and chat_session_id is not null)
        )
);

comment on table safety_events is '고위험 또는 주의 필요 안전 이벤트 추적 테이블';
comment on column safety_events.id is '안전 이벤트 ID';
comment on column safety_events.user_id is '사용자 ID';
comment on column safety_events.diary_id is '연결 일기 ID';
comment on column safety_events.chat_session_id is '연결 채팅 세션 ID';
comment on column safety_events.chat_message_id is '연결 채팅 메시지 ID';
comment on column safety_events.source_type is '이벤트 출처(DIARY, CHAT)';
comment on column safety_events.risk_level is '위험도 수준';
comment on column safety_events.risk_score is '위험 점수';
comment on column safety_events.trigger_signals is '트리거 신호';
comment on column safety_events.action_taken is '실행한 조치';
comment on column safety_events.resolved is '후속 조치 완료 여부';
comment on column safety_events.created_at is '생성 시각';
comment on column safety_events.resolved_at is '해결 시각';

create index idx_safety_events_user_created_at on safety_events(user_id, created_at desc);
create index idx_safety_events_diary_id on safety_events(diary_id);
create index idx_safety_events_chat_session_id on safety_events(chat_session_id);
create index idx_safety_events_risk_level on safety_events(risk_level);

create table ai_response_logs (
    id bigserial primary key,
    user_id bigint not null,
    diary_id bigint null,
    chat_session_id bigint null,
    chat_message_id bigint null,
    request_type varchar(30) not null,
    provider varchar(50) null,
    model_name varchar(100) null,
    request_payload jsonb null,
    response_payload jsonb null,
    fallback_used boolean not null default false,
    latency_ms integer null,
    created_at timestamp not null default current_timestamp,
    constraint fk_ai_response_logs_user
        foreign key (user_id) references users(id),
    constraint fk_ai_response_logs_diary
        foreign key (diary_id) references diaries(id),
    constraint fk_ai_response_logs_chat_session
        foreign key (chat_session_id) references chat_sessions(id),
    constraint fk_ai_response_logs_chat_message
        foreign key (chat_message_id) references chat_messages(id),
    constraint chk_ai_response_logs_request_type
        check (request_type in ('ANALYZE_DIARY', 'RISK_SCORE', 'GENERATE_REPLY'))
);

comment on table ai_response_logs is 'AI 요청과 응답 감사 로그 테이블';
comment on column ai_response_logs.id is '로그 ID';
comment on column ai_response_logs.user_id is '사용자 ID';
comment on column ai_response_logs.diary_id is '연결 일기 ID';
comment on column ai_response_logs.chat_session_id is '연결 채팅 세션 ID';
comment on column ai_response_logs.chat_message_id is '연결 채팅 메시지 ID';
comment on column ai_response_logs.request_type is 'AI 요청 종류';
comment on column ai_response_logs.provider is 'AI 제공자';
comment on column ai_response_logs.model_name is '모델 이름';
comment on column ai_response_logs.request_payload is '요청 payload';
comment on column ai_response_logs.response_payload is 'c';
comment on column ai_response_logs.fallback_used is 'fallback 사용 여부';
comment on column ai_response_logs.latency_ms is '응답 지연 시간(ms)';
comment on column ai_response_logs.created_at is '생성 시각';

create index idx_ai_response_logs_user_created_at on ai_response_logs(user_id, created_at desc);
create index idx_ai_response_logs_request_type on ai_response_logs(request_type);
create index idx_ai_response_logs_chat_session_id on ai_response_logs(chat_session_id);
