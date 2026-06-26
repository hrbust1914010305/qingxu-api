create table if not exists sys_role (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(64) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted integer not null default 0
);

create table if not exists sys_permission (
    id bigserial primary key,
    code varchar(128) not null unique,
    name varchar(128) not null,
    permission_type varchar(32),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted integer not null default 0
);

create table if not exists sys_user_role (
    id bigserial primary key,
    user_id bigint not null,
    role_id bigint not null,
    created_at timestamp not null default current_timestamp,
    unique(user_id, role_id)
);

create table if not exists sys_role_permission (
    id bigserial primary key,
    role_id bigint not null,
    permission_id bigint not null,
    created_at timestamp not null default current_timestamp,
    unique(role_id, permission_id)
);
