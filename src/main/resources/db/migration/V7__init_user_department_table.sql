-- 用户部门关联表
CREATE TABLE sys_user_department (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, department_id)
);

COMMENT ON TABLE sys_user_department IS '用户部门关联表';
COMMENT ON COLUMN sys_user_department.id IS '主键';
COMMENT ON COLUMN sys_user_department.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_department.department_id IS '部门ID';
COMMENT ON COLUMN sys_user_department.created_at IS '创建时间';

-- 创建索引
CREATE INDEX idx_sys_user_department_user_id ON sys_user_department(user_id);
CREATE INDEX idx_sys_user_department_department_id ON sys_user_department(department_id);