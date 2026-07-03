ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS need_password_change BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN sys_user.need_password_change IS '是否需要修改密码（首次登录或重置密码后为true）';
