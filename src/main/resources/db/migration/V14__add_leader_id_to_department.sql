-- 部门表新增负责人用户ID字段
ALTER TABLE sys_department ADD COLUMN IF NOT EXISTS leader_id BIGINT;
COMMENT ON COLUMN sys_department.leader_id IS '负责人用户ID';

-- 将现有 leader 名字匹配到 user ID，回填 leader_id
UPDATE sys_department d
SET leader_id = u.id
FROM sys_user u
WHERE d.leader = u.realname
  AND d.leader IS NOT NULL
  AND d.deleted = 0
  AND u.deleted = 0;
