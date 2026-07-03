-- V15__create_menu_tables.sql
-- 菜单管理表

CREATE TABLE sys_menu (
    id          BIGSERIAL       PRIMARY KEY,
    parent_id   BIGINT          NOT NULL DEFAULT 0,
    name        VARCHAR(100)    NOT NULL,
    path        VARCHAR(200)    NOT NULL,
    component   VARCHAR(200),
    redirect    VARCHAR(200),
    title       VARCHAR(100)    NOT NULL,
    icon        VARCHAR(100),
    permission  VARCHAR(100),
    menu_type   VARCHAR(20)     NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    visible     BOOLEAN         NOT NULL DEFAULT TRUE,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    is_external BOOLEAN         NOT NULL DEFAULT FALSE,
    is_cache    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_menu_name UNIQUE (name)
);

CREATE INDEX idx_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX idx_menu_sort_order ON sys_menu(sort_order);

-- 种子数据：菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, title, icon, permission, menu_type, sort_order, visible, status, is_external, is_cache) VALUES
-- 首页
(1, 0, 'Home', '/home', NULL, NULL, '首页', 'icon-home', NULL, 'DIRECTORY', 1, true, 'ACTIVE', false, true),
-- 系统管理
(2, 0, 'System', '/system', 'Layout', '/system/department', '系统管理', 'icon-setting', NULL, 'DIRECTORY', 2, true, 'ACTIVE', false, true),
-- 系统管理子菜单
(10, 2, 'Department', 'department', 'system/department/index', NULL, '部门管理', 'icon-common', 'system:department:list', 'MENU', 1, true, 'ACTIVE', false, true),
(3, 2, 'User', 'user', 'system/user/index', NULL, '用户管理', 'icon-user', 'system:user:list', 'MENU', 2, true, 'ACTIVE', false, true),
(4, 2, 'Role', 'role', 'system/role/index', NULL, '角色管理', 'icon-team', 'system:role:list', 'MENU', 3, true, 'ACTIVE', false, true),
(5, 2, 'Permission', 'permission', 'system/permission/index', NULL, '权限管理', 'icon-lock', 'system:permission:list', 'MENU', 4, true, 'ACTIVE', false, true),
(9, 2, 'Menu', 'menu', 'system/menu/index', NULL, '菜单管理', 'icon-menu', 'system:menu:list', 'MENU', 5, true, 'ACTIVE', false, true),
-- 知识库管理
(6, 0, 'Knowledge', '/knowledge', 'Layout', '/knowledge/list', '知识库管理', 'icon-folder', NULL, 'DIRECTORY', 3, true, 'ACTIVE', false, true),
-- 知识库管理子菜单
(7, 6, 'KnowledgeList', 'list', 'knowledge/list/index', NULL, '知识库列表', 'icon-caidan', 'kb:knowledge:list', 'MENU', 1, true, 'ACTIVE', false, true),
(8, 6, 'DocumentList', 'document', 'knowledge/document/index', NULL, '文档管理', 'icon-file', 'kb:document:list', 'MENU', 2, true, 'ACTIVE', false, true);
