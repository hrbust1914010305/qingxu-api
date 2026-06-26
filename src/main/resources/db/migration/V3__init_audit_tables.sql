create table if not exists sys_login_log (
    id bigserial primary key,
    event_type varchar(64) not null,
    success boolean not null,
    username varchar(64),
    user_id bigint,
    ip varchar(64),
    user_agent varchar(512),
    trace_id varchar(128),
    created_at timestamp not null default current_timestamp
);

create table if not exists sys_operation_log (
    id bigserial primary key,
    operation_type varchar(64) not null,
    success boolean not null,
    username varchar(64),
    user_id bigint,
    ip varchar(64),
    user_agent varchar(512),
    trace_id varchar(128),
    created_at timestamp not null default current_timestamp
);
