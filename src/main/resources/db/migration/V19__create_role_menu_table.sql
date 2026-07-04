CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGSERIAL       PRIMARY KEY,
    role_id     BIGINT          NOT NULL,
    menu_id     BIGINT          NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

CREATE INDEX IF NOT EXISTS idx_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_menu_id ON sys_role_menu(menu_id);