alter table team
    add column final_stage_unlocked boolean not null default false,
    add column final_stage_forced  boolean not null default false;
