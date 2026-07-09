-- Normalize menu management button permissions to match MenuController authorities.
-- Historical seed data used system:menu:add/edit, while the API and frontend expect
-- system:menu:create/update/delete.

UPDATE sys_menu
SET permission = 'system:menu:create',
    updated_at = now()
WHERE name = 'addMenu'
  AND permission = 'system:menu:add';

UPDATE sys_menu
SET permission = 'system:menu:update',
    updated_at = now()
WHERE name = 'editMenu'
  AND permission = 'system:menu:edit';

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:menu:list', '菜单列表', 'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:menu:list');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:menu:create', '创建菜单', 'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:menu:create');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:menu:update', '更新菜单', 'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:menu:update');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:menu:delete', '删除菜单', 'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:menu:delete');

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r
JOIN sys_permission p ON p.code IN (
    'system:menu:list',
    'system:menu:create',
    'system:menu:update',
    'system:menu:delete'
)
WHERE r.code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, now()
FROM sys_role r
JOIN sys_menu m ON m.name IN ('Menu', 'addMenu', 'editMenu', 'deleteMenu')
WHERE r.code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );
