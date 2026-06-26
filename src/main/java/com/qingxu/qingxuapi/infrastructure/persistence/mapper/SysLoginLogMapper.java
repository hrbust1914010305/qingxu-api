package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysLoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLogEntity> {
}
