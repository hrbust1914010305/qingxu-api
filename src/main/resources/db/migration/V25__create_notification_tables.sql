create table if not exists sys_notification (
    id bigserial primary key,
    tenant_id varchar(64) not null default 'default',
    type varchar(32) not null,
    level varchar(16) not null default 'INFO',
    title varchar(128) not null,
    content varchar(1000) not null,
    target_url varchar(255),
    biz_type varchar(64),
    biz_id varchar(64),
    sender_id bigint,
    sender_name varchar(64),
    trace_id varchar(128),
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_notification_type on sys_notification(type);
create index if not exists idx_notification_created_at on sys_notification(created_at desc);
create index if not exists idx_notification_biz on sys_notification(biz_type, biz_id);
create index if not exists idx_notification_tenant_created on sys_notification(tenant_id, created_at desc);

create table if not exists sys_notification_recipient (
    id bigserial primary key,
    notification_id bigint not null references sys_notification(id) on delete cascade,
    user_id bigint not null,
    read_status varchar(16) not null default 'UNREAD',
    read_at timestamp,
    deleted boolean not null default false,
    created_at timestamp not null default current_timestamp,
    constraint uk_notification_recipient unique (notification_id, user_id)
);

create index if not exists idx_notification_recipient_user_read
    on sys_notification_recipient(user_id, read_status, deleted, created_at desc);
create index if not exists idx_notification_recipient_user_deleted_created
    on sys_notification_recipient(user_id, deleted, created_at desc);
create index if not exists idx_notification_recipient_notification
    on sys_notification_recipient(notification_id);
