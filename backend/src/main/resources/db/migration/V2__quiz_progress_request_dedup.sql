alter table quiz_progress
    add column last_request_id      varchar(64),
    add column last_request_correct boolean;
