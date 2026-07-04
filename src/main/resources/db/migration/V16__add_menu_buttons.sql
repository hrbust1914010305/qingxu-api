-- V16__add_menu_buttons.sql
-- 为部门管理、用户管理、菜单管理及角色管理添加按钮权限

-- ========== 部门管理按钮 (parent_id = 10) ==========
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (100, 10, 'addDepartment', '', NULL, NULL, '新增部门', 'icon-plus', 'system:department:add', 'BUTTON', 1, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (101, 10, 'editDepartment', '', NULL, NULL, '编辑部门', 'icon-edit', 'system:department:edit', 'BUTTON', 2, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (102, 10, 'deleteDepartment', '', NULL, NULL, '删除部门', 'icon-delete', 'system:department:delete', 'BUTTON', 3, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (103, 10, 'viewDepartment', '', NULL, NULL, '查看详情', 'icon-eye', 'system:department:view', 'BUTTON', 4, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (104, 10, 'assignLeader', '', NULL, NULL, '分配负责人', 'icon-user-add', 'system:department:assignLeader', 'BUTTON', 5, true, 'ACTIVE', false, true);

-- ========== 用户管理按钮 (parent_id = 3) ==========
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (200, 3, 'addUser', '', NULL, NULL, '新增用户', 'icon-plus', 'system:user:add', 'BUTTON', 1, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (201, 3, 'editUser', '', NULL, NULL, '编辑用户', 'icon-edit', 'system:user:edit', 'BUTTON', 2, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (202, 3, 'deleteUser', '', NULL, NULL, '删除用户', 'icon-delete', 'system:user:delete', 'BUTTON', 3, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (203, 3, 'resetPassword', '', NULL, NULL, '重置密码', 'icon-lock', 'system:user:resetPassword', 'BUTTON', 4, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (204, 3, 'assignRole', '', NULL, NULL, '分配角色', 'icon-team', 'system:user:assignRole', 'BUTTON', 5, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (205, 3, 'exportUser', '', NULL, NULL, '导出用户', 'icon-download', 'system:user:export', 'BUTTON', 6, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (206, 3, 'toggleStatus', '', NULL, NULL, '启用/禁用', 'icon-switch', 'system:user:toggleStatus', 'BUTTON', 7, true, 'ACTIVE', false, true);

-- ========== 菜单管理按钮 (parent_id = 9) ==========
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (300, 9, 'addMenu', '', NULL, NULL, '新增菜单', 'icon-plus', 'system:menu:add', 'BUTTON', 1, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (301, 9, 'editMenu', '', NULL, NULL, '编辑菜单', 'icon-edit', 'system:menu:edit', 'BUTTON', 2, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (302, 9, 'deleteMenu', '', NULL, NULL, '删除菜单', 'icon-delete', 'system:menu:delete', 'BUTTON', 3, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (303, 9, 'sortMenu', '', NULL, NULL, '排序调整', 'icon-drag', 'system:menu:sort', 'BUTTON', 4, true, 'ACTIVE', false, true);

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES (304, 9, 'toggleVisible', '', NULL, NULL, '显示/隐藏', 'icon-eye', 'system:menu:toggleVisible', 'BUTTON', 5, true, 'ACTIVE', false, true);

-- ========== 角色管理按钮 (parent_id = 4) ==========
-- 已在后续迁移 V21 中添加
