package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {

    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId}")
    long countByParentId(Long parentId);

    @Select("SELECT * FROM sys_menu WHERE name = #{name}")
    SysMenuEntity findByName(String name);
}
