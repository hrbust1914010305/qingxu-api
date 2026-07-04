-- V17__add_button_permissions.sql
-- 将按钮权限添加到 sys_permission 表，并分配给管理员角色

-- ========== 部门管理按钮权限 ==========
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:department:add', '新增部门', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:department:edit', '编辑部门', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:department:delete', '删除部门', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:department:view', '查看部门详情', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:department:assignLeader', '分配负责人', 'BUTTON', now(), now(), 0);

-- ========== 用户管理按钮权限 ==========
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:user:add', '新增用户', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:user:edit', '编辑用户', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:user:resetPassword', '重置密码', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:user:assignRole', '分配角色', 'BUTTON', now(), now(), 0);

-- ========== 菜单管理按钮权限 ==========
INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:menu:add', '新增菜单', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:menu:edit', '编辑菜单', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:menu:delete', '删除菜单', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:menu:sort', '排序调整', 'BUTTON', now(), now(), 0);

INSERT INTO sys_permission (code, name, permission_type, created_at, updated_at, deleted)
VALUES ('system:menu:toggleVisible', '显示/隐藏菜单', 'BUTTON', now(), now(), 0);


-- ========== 为管理员角色分配所有按钮权限 ==========

-- ========== 角色管理按钮权限 ==========
-- 已在后续迁移 V22 中添加

-- ========== 为管理员角色分配所有按钮权限 ==========
-- 部门管理按钮权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' AND p.code LIKE 'system:department:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 用户管理按钮权限（补充之前未添加的）
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' AND p.code IN ('system:user:add', 'system:user:edit', 'system:user:resetPassword', 'system:user:assignRole')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 菜单管理按钮权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM sys_role r, sys_permission p
WHERE r.code = 'admin' AND p.code LIKE 'system:menu:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);