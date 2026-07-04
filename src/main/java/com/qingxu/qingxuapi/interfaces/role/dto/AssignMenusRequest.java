package com.qingxu.qingxuapi.interfaces.role.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignMenusRequest(
        @NotNull(message = "菜单ID列表不能为空")
        List<Long> menuIds
) {
}