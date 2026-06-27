package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_preference")
public class SysUserPreferenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 系统配置（10个字段）
    private Boolean collapsed;
    private Boolean isAccordion;
    private Boolean isBreadcrumb;
    private Boolean isTabs;
    private Boolean isFooter;
    private String watermark;
    private String watermarkColor;
    private Integer watermarkFontSize;
    private Integer watermarkRotate;
    private String watermarkGap;

    // 主题配置（7个字段）
    private String layoutType;
    private String themeColor;
    private Boolean colorWeakMode;
    private Boolean grayMode;
    private Boolean asideDark;
    private Boolean darkMode;
    private String transitionPage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}