-- 내부 AI 분석 결과를 일기와 1:1로 저장하는 테이블을 생성합니다.
create table diary_ai_analyses (
    id bigserial primary key,
    diary_id bigint not null unique,
    primary_emotion varchar(30) null,
    emotion_intensity integer null,
    summary text null,
    confidence numeric(4, 3) null,
    raw_payload text null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_diary_ai_analyses_diary
        foreign key (diary_id) references diaries(id),
    constraint chk_diary_ai_analyses_intensity
        check (emotion_intensity is null or emotion_intensity between 1 and 5)
);

create index idx_diary_ai_analyses_diary_id on diary_ai_analyses(diary_id);
