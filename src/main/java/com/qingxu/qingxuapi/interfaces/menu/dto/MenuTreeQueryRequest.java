package com.qingxu.qingxuapi.interfaces.menu.dto;

import lombok.Data;

@Data
public class MenuTreeQueryRequest {
    private String name;
    private String title;
    private String status;
    private String menuType;
    private Long parentId;
    private Boolean visible;
    private Boolean includeButtons;
}
