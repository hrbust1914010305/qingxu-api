package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserDepartmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserDepartmentMapper extends BaseMapper<SysUserDepartmentEntity> {

    @Select("SELECT * FROM sys_user_department WHERE department_id = #{departmentId}")
    List<SysUserDepartmentEntity> selectByDeptId(@Param("departmentId") Long departmentId);

    @Select("SELECT * FROM sys_user_department WHERE user_id = #{userId}")
    List<SysUserDepartmentEntity> selectByUserId(@Param("userId") Long userId);
}