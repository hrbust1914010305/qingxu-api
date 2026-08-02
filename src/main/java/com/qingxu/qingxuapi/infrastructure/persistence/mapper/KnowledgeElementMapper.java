package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeElementEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeElementMapper extends BaseMapper<KnowledgeElementEntity> {
}
