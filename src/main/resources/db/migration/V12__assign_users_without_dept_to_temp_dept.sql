-- 将没有指定部门的用户关联到临时部门
INSERT INTO sys_user_department (user_id, department_id, created_at)
SELECT u.id, d.id, now()
FROM sys_user u
JOIN (SELECT id FROM sys_department WHERE deleted = 0 AND dept_type = 'TEMPORARY' AND parent_id = 0) d ON 1=1
WHERE u.deleted = 0
  AND u.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_department ud WHERE ud.user_id = u.id
  );
