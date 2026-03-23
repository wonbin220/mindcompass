-- 상담 세션과 대화 메시지를 저장하는 채팅 테이블을 생성합니다.
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

create index idx_chat_sessions_user_id on chat_sessions(user_id);
create index idx_chat_sessions_user_updated_at on chat_sessions(user_id, updated_at desc);

-- 세션 안에서 오가는 사용자/AI 메시지를 저장합니다.
create table chat_messages (
    id bigserial primary key,
    session_id bigint not null,
    role varchar(20) not null,
    content text not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_chat_messages_session
        foreign key (session_id) references chat_sessions(id)
);

create index idx_chat_messages_session_id on chat_messages(session_id);
create index idx_chat_messages_session_created_at on chat_messages(session_id, created_at asc);
