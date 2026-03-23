-- 일기 AI 분석 테이블에 위험도 결과 저장 컬럼을 추가한다.
alter table diary_ai_analyses
    add column if not exists risk_level varchar(20) null,
    add column if not exists risk_score numeric(4, 3) null,
    add column if not exists risk_signals text null,
    add column if not exists recommended_action varchar(40) null;
