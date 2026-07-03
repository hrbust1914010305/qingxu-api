-- 将所有现有 ACTIVE 用户关联到 admin 角色，确保已有用户不受影响
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, now()
FROM sys_user u
CROSS JOIN sys_role r
WHERE r.code = 'admin'
  AND u.status = 'ACTIVE'
  AND u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
