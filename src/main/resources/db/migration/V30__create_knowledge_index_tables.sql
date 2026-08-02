create table if not exists knowledge_base (
    id bigserial primary key,
    name varchar(120) not null,
    description varchar(500),
    visibility varchar(30) not null default 'PRIVATE',
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create index if not exists idx_knowledge_base_created_by on knowledge_base(created_by);

create table if not exists knowledge_document (
    id bigserial primary key,
    knowledge_base_id bigint not null,
    file_id bigint not null,
    original_name varchar(255) not null,
    mime_type varchar(120),
    extension varchar(30),
    checksum varchar(128),
    status varchar(30) not null,
    chunk_count int not null default 0,
    graph_ready boolean not null default false,
    error_message text,
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create index if not exists idx_knowledge_document_base on knowledge_document(knowledge_base_id);
create index if not exists idx_knowledge_document_file on knowledge_document(file_id);
create index if not exists idx_knowledge_document_status on knowledge_document(status);

create table if not exists knowledge_index_job (
    id bigserial primary key,
    document_id bigint not null,
    file_id bigint not null,
    knowledge_base_id bigint not null,
    status varchar(30) not null,
    stage varchar(50) not null,
    progress int not null default 0,
    estimated_seconds int,
    estimated_remaining_seconds int,
    retry_count int not null default 0,
    max_retry_count int not null default 3,
    error_message text,
    started_at timestamp,
    finished_at timestamp,
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists idx_knowledge_index_job_document on knowledge_index_job(document_id);
create index if not exists idx_knowledge_index_job_file_status on knowledge_index_job(file_id, status);
create index if not exists idx_knowledge_index_job_status on knowledge_index_job(status);

create table if not exists knowledge_element (
    id bigserial primary key,
    document_id bigint not null,
    file_id bigint not null,
    element_index int not null,
    element_type varchar(40) not null,
    text text,
    level int,
    title_path varchar(1000),
    page_number int,
    sheet_name varchar(255),
    slide_number int,
    start_offset int,
    end_offset int,
    metadata_json jsonb,
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_knowledge_element_document on knowledge_element(document_id);
create unique index if not exists uk_knowledge_element_document_index on knowledge_element(document_id, element_index);

create table if not exists knowledge_chunk (
    id bigserial primary key,
    knowledge_base_id bigint not null,
    document_id bigint not null,
    file_id bigint not null,
    chunk_index int not null,
    parent_chunk_id bigint,
    content text not null,
    content_type varchar(40) not null,
    title_path varchar(1000),
    page_number int,
    sheet_name varchar(255),
    slide_number int,
    start_element_index int,
    end_element_index int,
    start_offset int,
    end_offset int,
    token_count int,
    chunk_hash varchar(128) not null,
    metadata_json jsonb,
    embedding_model varchar(120),
    embedding_dimension int,
    embedding_json jsonb,
    embedding_status varchar(30) not null default 'PENDING',
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create index if not exists idx_knowledge_chunk_document on knowledge_chunk(document_id);
create index if not exists idx_knowledge_chunk_file on knowledge_chunk(file_id);
create index if not exists idx_knowledge_chunk_base on knowledge_chunk(knowledge_base_id);
create index if not exists idx_knowledge_chunk_hash on knowledge_chunk(chunk_hash);

create table if not exists kg_entity (
    id bigserial primary key,
    knowledge_base_id bigint not null,
    entity_type varchar(60) not null,
    name varchar(255) not null,
    normalized_name varchar(255) not null,
    description text,
    confidence numeric(5,4),
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create table if not exists kg_relation (
    id bigserial primary key,
    knowledge_base_id bigint not null,
    source_entity_id bigint not null,
    target_entity_id bigint not null,
    relation_type varchar(60) not null,
    description text,
    confidence numeric(5,4),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted int not null default 0
);

create table if not exists kg_entity_mention (
    id bigserial primary key,
    entity_id bigint not null,
    document_id bigint not null,
    chunk_id bigint,
    file_id bigint not null,
    mention_text varchar(500) not null,
    title_path varchar(1000),
    page_number int,
    start_offset int,
    end_offset int,
    confidence numeric(5,4),
    created_at timestamp not null default current_timestamp
);

create table if not exists kg_relation_evidence (
    id bigserial primary key,
    relation_id bigint not null,
    document_id bigint not null,
    chunk_id bigint not null,
    evidence_text text not null,
    confidence numeric(5,4),
    created_at timestamp not null default current_timestamp
);

create table if not exists kg_alias (
    id bigserial primary key,
    entity_id bigint not null,
    alias varchar(255) not null,
    source varchar(60),
    confidence numeric(5,4),
    created_at timestamp not null default current_timestamp
);

create table if not exists kg_extraction_job (
    id bigserial primary key,
    job_id bigint not null,
    document_id bigint not null,
    knowledge_base_id bigint not null,
    status varchar(30) not null,
    error_message text,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists idx_kg_entity_base on kg_entity(knowledge_base_id);
create index if not exists idx_kg_entity_name on kg_entity(normalized_name);
create index if not exists idx_kg_relation_base on kg_relation(knowledge_base_id);
create index if not exists idx_kg_mention_file on kg_entity_mention(file_id);
create index if not exists idx_kg_evidence_document on kg_relation_evidence(document_id);
