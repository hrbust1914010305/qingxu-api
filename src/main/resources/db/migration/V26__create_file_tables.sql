create table if not exists sys_file (
    id bigserial primary key,
    original_name varchar(255) not null,
    storage_key varchar(255) not null,
    storage_path varchar(500) not null,
    mime_type varchar(120),
    extension varchar(30),
    size bigint not null,
    checksum varchar(128),
    biz_type varchar(80),
    created_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create unique index if not exists uk_sys_file_storage_key on sys_file(storage_key);
create index if not exists idx_sys_file_biz_type on sys_file(biz_type);
create index if not exists idx_sys_file_created_by on sys_file(created_by);
create index if not exists idx_sys_file_checksum on sys_file(checksum);

create table if not exists sys_file_upload_session (
    id bigserial primary key,
    upload_id varchar(64) not null unique,
    fingerprint varchar(255) not null,
    original_name varchar(255) not null,
    mime_type varchar(120),
    extension varchar(30),
    total_size bigint not null,
    chunk_size bigint not null,
    total_chunks int not null,
    biz_type varchar(80),
    status varchar(30) not null,
    expires_at timestamp not null,
    created_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists idx_file_upload_session_fingerprint
    on sys_file_upload_session(fingerprint, created_by, status);
create index if not exists idx_file_upload_session_expires_at
    on sys_file_upload_session(expires_at);

create table if not exists sys_file_upload_chunk (
    id bigserial primary key,
    upload_id varchar(64) not null,
    chunk_index int not null,
    chunk_size bigint not null,
    checksum varchar(128),
    storage_path varchar(500) not null,
    created_at timestamp not null default current_timestamp,
    constraint uk_file_upload_chunk unique (upload_id, chunk_index)
);

create index if not exists idx_file_upload_chunk_upload_id on sys_file_upload_chunk(upload_id);
