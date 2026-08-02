create unique index if not exists uk_kg_entity_base_type_name
    on kg_entity(knowledge_base_id, entity_type, normalized_name)
    where deleted = 0;

create unique index if not exists uk_kg_relation_base_source_target_type
    on kg_relation(knowledge_base_id, source_entity_id, target_entity_id, relation_type)
    where deleted = 0;
