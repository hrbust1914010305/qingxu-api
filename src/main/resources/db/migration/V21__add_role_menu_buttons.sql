-- 添加角色管理按钮 (parent_id = 4)
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache)
VALUES 
  (400, 4, 'addRole', '', NULL, NULL, '新增角色', 'icon-plus', 'system:role:create', 'BUTTON', 1, true, 'ACTIVE', false, true),
  (401, 4, 'editRole', '', NULL, NULL, '编辑角色', 'icon-edit', 'system:role:update', 'BUTTON', 2, true, 'ACTIVE', false, true),
  (402, 4, 'deleteRole', '', NULL, NULL, '删除角色', 'icon-delete', 'system:role:delete', 'BUTTON', 3, true, 'ACTIVE', false, true);
