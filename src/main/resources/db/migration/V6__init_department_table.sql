-- 部门表
CREATE TABLE sys_department (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    dept_type VARCHAR(32) NOT NULL DEFAULT 'DEPARTMENT',
    leader VARCHAR(64),
    phone VARCHAR(32),
    email VARCHAR(128),
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    description VARCHAR(512),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    UNIQUE(tenant_id, parent_id, name, deleted)
);

COMMENT ON TABLE sys_department IS '部门表';
COMMENT ON COLUMN sys_department.id IS '主键';
COMMENT ON COLUMN sys_department.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_department.parent_id IS '父部门ID，0为顶级';
COMMENT ON COLUMN sys_department.name IS '部门名称';
COMMENT ON COLUMN sys_department.dept_type IS '部门类型：DIRECTORY/DEPARTMENT/TEMPORARY';
COMMENT ON COLUMN sys_department.leader IS '负责人';
COMMENT ON COLUMN sys_department.phone IS '联系电话';
COMMENT ON COLUMN sys_department.email IS '邮箱';
COMMENT ON COLUMN sys_department.sort_order IS '排序号';
COMMENT ON COLUMN sys_department.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sys_department.description IS '描述';
COMMENT ON COLUMN sys_department.created_by IS '创建人ID';
COMMENT ON COLUMN sys_department.created_at IS '创建时间';
COMMENT ON COLUMN sys_department.updated_by IS '更新人ID';
COMMENT ON COLUMN sys_department.updated_at IS '更新时间';
COMMENT ON COLUMN sys_department.deleted IS '逻辑删除';

-- 创建索引（用于树形查询）
CREATE INDEX idx_sys_department_parent_id ON sys_department(parent_id);
CREATE INDEX idx_sys_department_tenant_id ON sys_department(tenant_id);

-- 初始化默认数据：创建根目录和临时部门
INSERT INTO sys_department (tenant_id, parent_id, name, dept_type, sort_order, status, description) VALUES
('default', 0, 'xxx科技有限公司', 'DIRECTORY', 0, 'ACTIVE', '总公司'),
('default', 0, '临时部门', 'TEMPORARY', -1, 'ACTIVE', '系统自动创建的临时部门');