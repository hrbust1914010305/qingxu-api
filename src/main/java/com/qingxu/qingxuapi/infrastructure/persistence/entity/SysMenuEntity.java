package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenuEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String redirect;
    private String title;
    private String icon;
    private String permission;
    private String menuType;
    private Integer sortOrder;
    private Boolean visible;
    private String status;
    private Boolean isExternal;
    private Boolean isCache;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
