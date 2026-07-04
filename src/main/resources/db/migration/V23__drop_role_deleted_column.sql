-- V23__drop_role_deleted_column.sql
-- 删除角色表的软删除列，角色已改为硬删除
ALTER TABLE sys_role
    DROP COLUMN IF EXISTS deleted;
