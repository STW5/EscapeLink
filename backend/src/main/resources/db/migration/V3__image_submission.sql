create table image_submission (
    id                  bigserial primary key,
    team_id             bigint       not null references team (id),
    quiz_id             bigint       not null references quiz (id),
    run_no              int          not null,
    submission_version  int          not null,
    file_path           varchar(500) not null,
    status              varchar(10)  not null,
    reject_reason       varchar(255),
    created_at          timestamptz  not null,
    reviewed_at         timestamptz,
    reviewed_by         varchar(255),
    constraint uk_image_submission_version unique (team_id, quiz_id, run_no, submission_version)
);

create index ix_image_submission_team_quiz_run on image_submission (team_id, quiz_id, run_no);
create index ix_image_submission_status on image_submission (status);
