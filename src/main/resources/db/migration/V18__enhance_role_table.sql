ALTER TABLE sys_role
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500),
    ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_role_code ON sys_role(code);
CREATE INDEX IF NOT EXISTS idx_role_status ON sys_role(status);

UPDATE sys_role SET status = 'ACTIVE', sort_order = 1 WHERE code = 'admin';