create table game (
    id          bigserial primary key,
    title       varchar(255) not null,
    status      varchar(20)  not null,
    start_at    timestamptz,
    end_at      timestamptz,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);

create table team (
    id              bigserial primary key,
    game_id         bigint       not null references game (id),
    name            varchar(255) not null,
    invite_token    varchar(64)  not null,
    current_run_no  int          not null default 1,
    created_at      timestamptz  not null,
    constraint uk_team_invite_token unique (invite_token)
);

create index ix_team_game_id on team (game_id);

create table team_session (
    id              bigserial primary key,
    team_id         bigint       not null references team (id),
    session_token   varchar(128) not null,
    device_id       varchar(255),
    created_at      timestamptz  not null,
    last_access_at  timestamptz  not null,
    expires_at      timestamptz  not null,
    revoked         boolean      not null default false,
    constraint uk_team_session_token unique (session_token)
);

create index ix_team_session_team_id on team_session (team_id);

create table quiz (
    id                  bigserial primary key,
    game_id             bigint       not null references game (id),
    title               varchar(255) not null,
    content             text         not null,
    type                varchar(10)  not null,
    order_no            int          not null,
    hint                text,
    hint_delay_seconds  int          not null default 0,
    created_at          timestamptz  not null,
    updated_at          timestamptz  not null
);

create index ix_quiz_game_id on quiz (game_id);

create table quiz_secret (
    id       bigserial primary key,
    quiz_id  bigint       not null references quiz (id),
    answer   varchar(255) not null,
    constraint uk_quiz_secret_quiz_id unique (quiz_id)
);

create table quiz_progress (
    id                    bigserial primary key,
    team_id               bigint      not null references team (id),
    quiz_id               bigint      not null references quiz (id),
    run_no                int         not null,
    status                varchar(10) not null,
    first_entered_at      timestamptz,
    solved_at             timestamptz,
    last_wrong_answer_at  timestamptz,
    version               bigint      not null default 0,
    constraint uk_quiz_progress_team_quiz_run unique (team_id, quiz_id, run_no)
);

create index ix_quiz_progress_team_run on quiz_progress (team_id, run_no);
