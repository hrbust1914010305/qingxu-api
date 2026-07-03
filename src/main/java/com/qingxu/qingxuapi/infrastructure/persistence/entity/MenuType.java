package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import lombok.Getter;

@Getter
public enum MenuType {
    DIRECTORY("DIRECTORY", "目录"),
    MENU("MENU", "菜单"),
    BUTTON("BUTTON", "按钮");

    private final String code;
    private final String description;

    MenuType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MenuType fromCode(String code) {
        for (MenuType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的菜单类型: " + code);
    }
}
