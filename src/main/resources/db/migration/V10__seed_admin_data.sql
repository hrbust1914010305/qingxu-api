-- 用户管理权限（幂等插入）
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:list',   '用户列表',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:list');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:view',   '用户详情',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:view');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:create', '创建用户',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:create');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:update', '更新用户',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:update');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:delete', '删除用户',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:delete');
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
SELECT 'system:user:export', '导出用户',   'API', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:export');

-- 管理员角色（幂等）
INSERT INTO sys_role (code, name, created_at, updated_at, deleted)
SELECT 'admin', '系统管理员', now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'admin');

-- 管理员角色 -> 所有用户管理权限（幂等）
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' AND p.code LIKE 'system:user:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 默认管理员用户（密码 Admin@123456，首次登录需修改密码，幂等插入）
INSERT INTO sys_user (tenant_id, username, realname, nickname, password_hash, user_type, status, need_password_change, created_at, updated_at, deleted)
SELECT 'default', 'admin', '系统管理员', '管理员', '$2a$10$qSO5/x4WOKVumBevFIZhCujw93ka6tNLMQ0PJmAhPHr8jIzTkF8qq', 'INTERNAL', 'ACTIVE', true, now(), now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE tenant_id = 'default' AND username = 'admin' AND deleted = 0);

-- 关联管理员用户与管理员角色（幂等）
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, now()
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
