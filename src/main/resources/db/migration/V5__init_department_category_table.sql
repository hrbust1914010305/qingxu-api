-- 部门分类表
CREATE TABLE sys_department_category (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    UNIQUE(tenant_id, code, deleted)
);

COMMENT ON TABLE sys_department_category IS '部门分类表';
COMMENT ON COLUMN sys_department_category.id IS '主键';
COMMENT ON COLUMN sys_department_category.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_department_category.name IS '分类名称';
COMMENT ON COLUMN sys_department_category.code IS '分类编码';
COMMENT ON COLUMN sys_department_category.sort_order IS '排序号';
COMMENT ON COLUMN sys_department_category.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sys_department_category.created_at IS '创建时间';
COMMENT ON COLUMN sys_department_category.updated_at IS '更新时间';
COMMENT ON COLUMN sys_department_category.deleted IS '逻辑删除';

-- 初始化默认数据
INSERT INTO sys_department_category (tenant_id, name, code, sort_order, status) VALUES
('default', '研发类', 'RD', 1, 'ACTIVE'),
('default', '行政类', 'ADMIN', 2, 'ACTIVE'),
('default', '业务类', 'BIZ', 3, 'ACTIVE');