package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysDepartmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartmentEntity> {

    @Update("UPDATE sys_department SET deleted = 1, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}