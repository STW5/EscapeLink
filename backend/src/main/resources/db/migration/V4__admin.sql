create table admin_account (
    id             bigserial primary key,
    username       varchar(100) not null,
    password_hash  varchar(255) not null,
    role           varchar(10)  not null,
    created_at     timestamptz  not null,
    constraint uk_admin_account_username unique (username)
);

create table admin_action_log (
    id              bigserial primary key,
    admin_username  varchar(100) not null,
    action          varchar(20)  not null,
    team_id         bigint,
    quiz_id         bigint,
    detail          varchar(255),
    created_at      timestamptz  not null
);

create index ix_admin_action_log_team on admin_action_log (team_id);
