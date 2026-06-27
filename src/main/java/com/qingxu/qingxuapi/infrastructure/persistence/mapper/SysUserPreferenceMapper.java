package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserPreferenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserPreferenceMapper extends BaseMapper<SysUserPreferenceEntity> {

    @Select("SELECT * FROM sys_user_preference WHERE user_id = #{userId}")
    SysUserPreferenceEntity selectByUserId(Long userId);
}