-- Seed the default role assigned to newly registered users and newly created
-- backend users when no explicit role is supplied.

INSERT INTO sys_role (code, name, status, description, remark, sort_order, created_at, updated_at)
VALUES (
    'default_user',
    '默认用户',
    'ACTIVE',
    '注册或后台创建用户未指定角色时自动分配的默认角色',
    'Seeded by V29__seed_default_user_role.sql',
    900,
    now(),
    now()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    remark = EXCLUDED.remark,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, now()
FROM sys_role r
JOIN sys_menu m ON m.name = 'Home'
WHERE r.code = 'default_user'
ON CONFLICT (role_id, menu_id) DO NOTHING;
