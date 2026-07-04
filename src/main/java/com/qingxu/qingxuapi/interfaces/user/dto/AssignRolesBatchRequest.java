package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request payload for batch assigning roles to users.
 *
 * <ul>
 *   <li><code>userIds</code> – 必填且非空的用户 ID 列表。</li>
 *   <li><code>roleIds</code> – 角色 ID 列表，可为空（表示清除所有角色）。若非空，则每个角色必须存在。</li>
 * </ul>
 */
public record AssignRolesBatchRequest(
        @NotEmpty(message = "用户ID列表不能为空")
        List<Long> userIds,

        // 角色列表可以为空，表示删除用户的所有角色；如果非空则每个 roleId 必须存在。
        List<Long> roleIds
) {}
