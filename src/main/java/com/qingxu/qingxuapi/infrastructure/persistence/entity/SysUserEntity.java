package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String username;
    private String realname;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String passwordHash;
    private String userType;
    private String status;
    private Boolean needPasswordChange;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
