-- 添加角色管理按钮对应的权限
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES
  ('system:role:create', '新增角色', 'BUTTON', now(), now(), 0),
  ('system:role:update', '编辑角色', 'BUTTON', now(), now(), 0),
  ('system:role:delete', '删除角色', 'BUTTON', now(), now(), 0)
ON CONFLICT (code) DO NOTHING;

-- 为管理员角色分配角色管理按钮权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' 
  AND p.code IN ('system:role:create','system:role:update','system:role:delete')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
