package com.qingxu.qingxuapi.interfaces.menu.dto;

import lombok.Data;

@Data
public class MenuQueryRequest {
    private Integer page;
    private Integer pageSize;
    private String name;
    private String title;
    private String status;
    private String menuType;
    private Long parentId;

    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 20;
    }
}
