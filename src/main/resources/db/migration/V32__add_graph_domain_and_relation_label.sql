alter table knowledge_base
    add column if not exists graph_domain varchar(60) not null default 'AUTO';

alter table kg_relation
    add column if not exists relation_label_zh varchar(120);

drop index if exists uk_kg_relation_base_source_target_type;

create unique index if not exists uk_kg_relation_base_source_target_type_label
    on kg_relation(knowledge_base_id, source_entity_id, target_entity_id, relation_type, coalesce(relation_label_zh, ''))
    where deleted = 0;
