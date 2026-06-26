package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionEntity> {

    @Select("""
            select distinct p.id, p.code, p.name, p.permission_type
            from sys_permission p
            inner join sys_role_permission rp on rp.permission_id = p.id
            inner join sys_user_role ur on ur.role_id = rp.role_id
            where ur.user_id = #{userId}
              and p.deleted = 0
            order by p.id
            """)
    List<SysPermissionEntity> selectByUserId(Long userId);
}
