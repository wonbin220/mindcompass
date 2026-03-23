-- 일기별 다중 감정 태그를 분리 저장하는 테이블을 생성합니다.
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

create index idx_diary_emotions_diary_id on diary_emotions(diary_id);
create index idx_diary_emotions_diary_source on diary_emotions(diary_id, source_type);
