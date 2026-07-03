package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_department")
public class SysDepartmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String tenantId;
    private Long parentId;
    private String name;
    private String deptType;
    private Long categoryId;
    private String leader;
    private String phone;
    private String email;
    private Integer sortOrder;
    private String status;
    private String description;
    private Long leaderId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}