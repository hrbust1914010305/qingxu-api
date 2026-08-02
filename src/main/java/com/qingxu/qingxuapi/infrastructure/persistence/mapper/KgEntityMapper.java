package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KgEntityMapper extends BaseMapper<KgEntityEntity> {
}
