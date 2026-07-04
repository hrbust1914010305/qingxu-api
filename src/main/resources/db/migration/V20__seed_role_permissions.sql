INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:role:list',   '角色列表',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:list');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:role:create', '创建角色',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:create');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:role:update', '更新角色',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:update');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:role:delete', '删除角色',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:delete');

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' AND p.code LIKE 'system:role:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);