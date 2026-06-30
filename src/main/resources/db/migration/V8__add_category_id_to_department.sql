-- 为部门表添加分类ID字段
ALTER TABLE sys_department ADD COLUMN IF NOT EXISTS category_id BIGINT;

COMMENT ON COLUMN sys_department.category_id IS '部门分类ID';

-- 创建索引（用于按分类查询部门）
CREATE INDEX IF NOT EXISTS idx_sys_department_category_id ON sys_department(category_id);