-- 백엔드 MVP의 기본 사용자/인증/일기 테이블을 생성합니다.
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

-- 사용자별 기본 앱 설정을 저장합니다.
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

-- 로그인 세션 유지를 위한 refresh token 이력을 저장합니다.
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

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index idx_refresh_tokens_user_hash_active
    on refresh_tokens(user_id, token_hash);

-- 사용자가 작성한 감정 일기 본문과 대표 감정을 저장합니다.
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

create index idx_diaries_user_id on diaries(user_id);
create index idx_diaries_user_written_at on diaries(user_id, written_at desc);
create index idx_diaries_user_deleted_at on diaries(user_id, deleted_at);
