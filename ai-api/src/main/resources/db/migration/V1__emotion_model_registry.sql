-- ai-api 내부 emotion model registry 초기 스키마이다.

create schema if not exists ai_internal;

create table if not exists ai_internal.emotion_model_registry (
    id bigserial primary key,
    experiment_name varchar(120) not null unique,
    model_name varchar(120) not null,
    base_model_name varchar(255) not null,
    artifact_dir text not null,
    metrics_json_path text not null,
    label_metadata_path text,
    training_config_path text,
    label_map_path text,
    training_dataset_tag varchar(120) not null,
    validation_dataset_tag varchar(120) not null,
    fallback_policy varchar(40) not null,
    status varchar(20) not null,
    is_active boolean not null default false,
    is_shadow boolean not null default false,
    accuracy numeric(6,4),
    macro_f1 numeric(6,4),
    happy_f1 numeric(6,4),
    calm_f1 numeric(6,4),
    anxious_f1 numeric(6,4),
    sad_f1 numeric(6,4),
    angry_f1 numeric(6,4),
    serving_notes text,
    approval_note text,
    rejection_reason text,
    approved_at timestamp,
    rejected_at timestamp,
    activated_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint chk_emotion_model_registry_status
        check (status in ('TRAINED', 'APPROVED', 'REJECTED', 'SHADOW', 'ACTIVE', 'ARCHIVED')),
    constraint chk_emotion_model_registry_fallback_policy
        check (fallback_policy in ('TIRED_FALLBACK_ONLY')),
    constraint chk_emotion_model_registry_active_status
        check (
            (is_active = true and status = 'ACTIVE')
            or (is_active = false and status in ('TRAINED', 'APPROVED', 'REJECTED', 'SHADOW', 'ARCHIVED'))
        )
);

create unique index if not exists uq_emotion_model_registry_active_true
    on ai_internal.emotion_model_registry (is_active)
    where is_active = true;

create index if not exists idx_emotion_model_registry_status
    on ai_internal.emotion_model_registry (status, created_at desc);

create index if not exists idx_emotion_model_registry_shadow
    on ai_internal.emotion_model_registry (is_shadow, created_at desc);

create index if not exists idx_emotion_model_registry_macro_f1
    on ai_internal.emotion_model_registry (macro_f1 desc nulls last);

create table if not exists ai_internal.emotion_model_registry_status_history (
    id bigserial primary key,
    registry_id bigint not null references ai_internal.emotion_model_registry(id),
    from_status varchar(20),
    to_status varchar(20) not null,
    change_reason text,
    changed_at timestamp not null default current_timestamp
);

create index if not exists idx_emotion_model_registry_status_history_registry_id
    on ai_internal.emotion_model_registry_status_history (registry_id, changed_at desc);
