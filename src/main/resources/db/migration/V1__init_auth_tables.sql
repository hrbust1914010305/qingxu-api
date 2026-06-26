create table if not exists sys_user (
    id bigserial primary key,
    tenant_id varchar(64),
    username varchar(64) not null,
    realname varchar(64),
    nickname varchar(64) not null,
    avatar varchar(255),
    email varchar(128),
    phone varchar(32),
    password_hash varchar(255) not null,
    user_type varchar(32) not null default 'EXTERNAL',
    status varchar(32) not null default 'PENDING',
    failed_login_count integer not null default 0,
    locked_until timestamp,
    last_login_at timestamp,
    last_login_ip varchar(64),
    created_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_by bigint,
    updated_at timestamp not null default current_timestamp,
    deleted integer not null default 0
);

alter table sys_user
    add constraint uk_sys_user_tenant_username_active unique (tenant_id, username, deleted);

alter table sys_user
    add constraint uk_sys_user_tenant_email_active unique (tenant_id, email, deleted);

alter table sys_user
    add constraint uk_sys_user_tenant_phone_active unique (tenant_id, phone, deleted);
