-- Normalize menu-driven permission sources so runtime authorities can be resolved
-- directly from sys_role_menu -> sys_menu.permission.

-- Align historical department and user menu button codes with controller authorities.
UPDATE sys_menu
SET permission = 'system:department:create',
    updated_at = now()
WHERE name = 'addDepartment'
  AND permission = 'system:department:add';

UPDATE sys_menu
SET permission = 'system:department:update',
    updated_at = now()
WHERE name = 'editDepartment'
  AND permission = 'system:department:edit';

UPDATE sys_menu
SET permission = 'system:user:create',
    updated_at = now()
WHERE name = 'addUser'
  AND permission = 'system:user:add';

UPDATE sys_menu
SET permission = 'system:user:update',
    updated_at = now()
WHERE name = 'editUser'
  AND permission = 'system:user:edit';

-- Ensure the role permission-assignment button exists in fresh environments.
INSERT INTO sys_menu (
    id, parent_id, name, path, component, redirect, title, icon, permission,
    menu_type, sort_order, visible, status, is_external, is_cache, created_at, updated_at
)
SELECT
    403, 4, 'assignPermissions', '', NULL, NULL, '分配权限', 'icon-safety-certificate',
    'system:role:assignPermissions', 'BUTTON', 1, true, 'ACTIVE', false, true, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission = 'system:role:assignPermissions'
);

-- Ensure permission dictionary keeps the normalized codes for compatibility.
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:department:list', '部门列表', 'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:department:list');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:department:create', '创建部门', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:department:create');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:department:update', '更新部门', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:department:update');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:create', '创建用户', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:create');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:update', '更新用户', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:update');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:toggleStatus', '启用/禁用用户', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:toggleStatus');

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:role:assignPermissions', '分配权限', 'BUTTON', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:assignPermissions');

-- Migrate historical role-permission references to normalized codes for compatibility.
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, rp.created_at
FROM sys_role_permission rp
JOIN sys_permission p_old ON p_old.id = rp.permission_id
JOIN sys_permission p_new ON p_new.code = 'system:department:create'
WHERE p_old.code = 'system:department:add'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp2
      WHERE rp2.role_id = rp.role_id
        AND rp2.permission_id = p_new.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, rp.created_at
FROM sys_role_permission rp
JOIN sys_permission p_old ON p_old.id = rp.permission_id
JOIN sys_permission p_new ON p_new.code = 'system:department:update'
WHERE p_old.code = 'system:department:edit'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp2
      WHERE rp2.role_id = rp.role_id
        AND rp2.permission_id = p_new.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, rp.created_at
FROM sys_role_permission rp
JOIN sys_permission p_old ON p_old.id = rp.permission_id
JOIN sys_permission p_new ON p_new.code = 'system:user:create'
WHERE p_old.code = 'system:user:add'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp2
      WHERE rp2.role_id = rp.role_id
        AND rp2.permission_id = p_new.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, rp.created_at
FROM sys_role_permission rp
JOIN sys_permission p_old ON p_old.id = rp.permission_id
JOIN sys_permission p_new ON p_new.code = 'system:user:update'
WHERE p_old.code = 'system:user:edit'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp2
      WHERE rp2.role_id = rp.role_id
        AND rp2.permission_id = p_new.id
  );

DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id
    FROM sys_permission
    WHERE code IN (
        'system:department:add',
        'system:department:edit',
        'system:user:add',
        'system:user:edit'
    )
);

DELETE FROM sys_permission
WHERE code IN (
    'system:department:add',
    'system:department:edit',
    'system:user:add',
    'system:user:edit'
);

-- Ensure the admin role sees the permission-assignment button in fresh environments.
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, now()
FROM sys_role r
JOIN sys_menu m ON m.permission = 'system:role:assignPermissions'
WHERE r.code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );
