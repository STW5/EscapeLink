alter table team
    add column final_stage_entered_at timestamptz,
    add column final_stage_cleared_at timestamptz;
