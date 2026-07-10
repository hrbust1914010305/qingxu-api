-- V24__create_permission_change_log.sql
-- 权限变更审计日志表
CREATE TABLE IF NOT EXISTS sys_permission_change_log (
    id              BIGSERIAL    PRIMARY KEY,
    change_type     VARCHAR(32)  NOT NULL,
    entity_id       BIGINT       NOT NULL,
    affected_users  JSONB        NOT NULL,
    admin_branch    BOOLEAN      NOT NULL DEFAULT FALSE,
    operator_id     BIGINT,
    operator_name   VARCHAR(64),
    reason          VARCHAR(255),
    trace_id        VARCHAR(128),
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pcl_change_type  ON sys_permission_change_log(change_type);
CREATE INDEX IF NOT EXISTS idx_pcl_occurred_at ON sys_permission_change_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_pcl_operator_id ON sys_permission_change_log(operator_id);
