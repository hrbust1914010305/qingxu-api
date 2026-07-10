-- Rebuild backoffice seed data directly in PostgreSQL.
-- Review the transaction first. Run with psql or a database client connected to the target database.
--
-- Default new-user password: Qingxu@123456
-- BCrypt hash generated with Spring Security BCryptPasswordEncoder.

BEGIN;

DO $$
DECLARE
    v_admin2_id BIGINT;
    v_admin_role_id BIGINT;
    v_default_role_id BIGINT;
    v_root_dept_id BIGINT;
    v_root_temp_dept_id BIGINT;
    v_password_hash TEXT := '$2a$10$2EQzEoOaPj1uev3dTp38fOf5ojZo7fn0krAm.yLCHiYxZlpNdgvVy';
    v_product_dept_id BIGINT;
    v_qa_dept_id BIGINT;
    v_ops_dept_id BIGINT;
    v_sales_dept_id BIGINT;
    v_admin_dept_id BIGINT;
    v_role_id BIGINT;
    v_user_id BIGINT;
    v_user_count INT;
    v_role_count INT;
    v_dept_count INT;
    v_missing_user_rel_count INT;
BEGIN
    SELECT id INTO v_admin2_id
    FROM sys_user
    WHERE username = 'admin2'
      AND deleted = 0
    ORDER BY id
    LIMIT 1;

    IF v_admin2_id IS NULL THEN
        RAISE EXCEPTION 'admin2 user was not found. Abort.';
    END IF;

    SELECT id INTO v_admin_role_id
    FROM sys_role
    WHERE code = 'admin'
    ORDER BY id
    LIMIT 1;

    IF v_admin_role_id IS NULL THEN
        RAISE EXCEPTION 'admin role was not found. Abort.';
    END IF;

    INSERT INTO sys_role (code, name, status, description, remark, sort_order, created_at, updated_at)
    VALUES (
        'default_user',
        '默认用户',
        'ACTIVE',
        '注册或后台创建用户未指定角色时自动分配的默认角色',
        'Created by scripts/rebuild-backoffice-data.sql',
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
        updated_at = now()
    RETURNING id INTO v_default_role_id;

    INSERT INTO sys_role_menu (role_id, menu_id, created_at)
    SELECT v_default_role_id, m.id, now()
    FROM sys_menu m
    WHERE m.name = 'Home'
    ON CONFLICT (role_id, menu_id) DO NOTHING;

    SELECT id INTO v_root_dept_id
    FROM sys_department
    WHERE parent_id = 0
      AND dept_type = 'DIRECTORY'
      AND deleted = 0
    ORDER BY sort_order, id
    LIMIT 1;

    IF v_root_dept_id IS NULL THEN
        RAISE EXCEPTION 'root DIRECTORY department was not found. Abort.';
    END IF;

    SELECT id INTO v_root_temp_dept_id
    FROM sys_department
    WHERE parent_id = 0
      AND dept_type = 'TEMPORARY'
      AND deleted = 0
    ORDER BY sort_order, id
    LIMIT 1;

    IF v_root_temp_dept_id IS NULL THEN
        RAISE EXCEPTION 'root TEMPORARY department was not found. Abort.';
    END IF;

    CREATE TEMP TABLE tmp_old_users AS
    SELECT id
    FROM sys_user
    WHERE username <> 'admin2'
      AND deleted = 0;

    CREATE TEMP TABLE tmp_old_roles AS
    SELECT id
    FROM sys_role
    WHERE code NOT IN ('admin', 'default_user');

    CREATE TEMP TABLE tmp_old_departments AS
    SELECT id
    FROM sys_department
    WHERE deleted = 0
      AND NOT (parent_id = 0 AND dept_type IN ('DIRECTORY', 'TEMPORARY'));

    DELETE FROM sys_user_role
    WHERE user_id IN (SELECT id FROM tmp_old_users)
       OR role_id IN (SELECT id FROM tmp_old_roles);

    DELETE FROM sys_user_department
    WHERE user_id IN (SELECT id FROM tmp_old_users)
       OR department_id IN (SELECT id FROM tmp_old_departments);

    DELETE FROM sys_user_preference
    WHERE user_id IN (SELECT id FROM tmp_old_users);

    DELETE FROM sys_role_menu
    WHERE role_id IN (SELECT id FROM tmp_old_roles);

    DELETE FROM sys_role_permission
    WHERE role_id IN (SELECT id FROM tmp_old_roles);

    UPDATE sys_user
    SET deleted = id,
        updated_at = now()
    WHERE id IN (SELECT id FROM tmp_old_users);

    UPDATE sys_department
    SET deleted = id,
        updated_at = now()
    WHERE id IN (SELECT id FROM tmp_old_departments);

    DELETE FROM sys_role
    WHERE id IN (SELECT id FROM tmp_old_roles);

    DELETE FROM sys_user_role WHERE user_id = v_admin2_id;
    INSERT INTO sys_user_role (user_id, role_id, created_at)
    VALUES (v_admin2_id, v_admin_role_id, now())
    ON CONFLICT (user_id, role_id) DO NOTHING;

    DELETE FROM sys_user_department WHERE user_id = v_admin2_id;
    INSERT INTO sys_user_department (user_id, department_id, created_at)
    VALUES (v_admin2_id, v_root_temp_dept_id, now())
    ON CONFLICT (user_id, department_id) DO NOTHING;

    INSERT INTO sys_department (
        tenant_id, parent_id, name, dept_type, leader, phone, email,
        sort_order, status, description, created_at, updated_at, deleted
    )
    VALUES
        ('default', v_root_dept_id, '产品研发部', 'DEPARTMENT', NULL, NULL, NULL, 10, 'ACTIVE', 'Product research and technology delivery team', now(), now(), 0),
        ('default', v_root_dept_id, '测试质量部', 'DEPARTMENT', NULL, NULL, NULL, 20, 'ACTIVE', 'Quality assurance, testing and release gate team', now(), now(), 0),
        ('default', v_root_dept_id, '运营交付部', 'DEPARTMENT', NULL, NULL, NULL, 30, 'ACTIVE', 'Customer operations, launch delivery and support team', now(), now(), 0),
        ('default', v_root_dept_id, '市场销售部', 'DEPARTMENT', NULL, NULL, NULL, 40, 'ACTIVE', 'Marketing, sales and business collaboration team', now(), now(), 0),
        ('default', v_root_dept_id, '综合管理部', 'DEPARTMENT', NULL, NULL, NULL, 50, 'ACTIVE', 'Administration, HR, finance and general support team', now(), now(), 0);

    SELECT id INTO v_product_dept_id FROM sys_department WHERE parent_id = v_root_dept_id AND name = '产品研发部' AND deleted = 0;
    SELECT id INTO v_qa_dept_id FROM sys_department WHERE parent_id = v_root_dept_id AND name = '测试质量部' AND deleted = 0;
    SELECT id INTO v_ops_dept_id FROM sys_department WHERE parent_id = v_root_dept_id AND name = '运营交付部' AND deleted = 0;
    SELECT id INTO v_sales_dept_id FROM sys_department WHERE parent_id = v_root_dept_id AND name = '市场销售部' AND deleted = 0;
    SELECT id INTO v_admin_dept_id FROM sys_department WHERE parent_id = v_root_dept_id AND name = '综合管理部' AND deleted = 0;

    CREATE TEMP TABLE tmp_roles (
        code VARCHAR(64) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(255),
        sort_order INT NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO tmp_roles (code, name, description, sort_order)
    VALUES
        ('rd_manager', '研发负责人', 'R&D team management, technical planning and delivery quality', 10),
        ('backend_engineer', '后端研发工程师', 'Backend services, APIs and data processing', 20),
        ('frontend_engineer', '前端研发工程师', 'Frontend pages, components and interaction experience', 30),
        ('qa_engineer', '测试工程师', 'Test design, quality verification and defect follow-up', 40),
        ('ops_specialist', '运营交付专员', 'Customer operations, project delivery and support response', 50),
        ('sales_admin', '销售/综合管理', 'Sales collaboration, customer follow-up and general administration', 60);

    INSERT INTO sys_role (code, name, status, description, remark, sort_order, created_at, updated_at)
    SELECT code, name, 'ACTIVE', description, 'Created by scripts/rebuild-backoffice-data.sql', sort_order, now(), now()
    FROM tmp_roles
    ON CONFLICT (code) DO UPDATE
    SET name = EXCLUDED.name,
        status = EXCLUDED.status,
        description = EXCLUDED.description,
        remark = EXCLUDED.remark,
        sort_order = EXCLUDED.sort_order,
        updated_at = now();

    -- Default menu assignment for the six business roles.
    -- Change ARRAY[1,2,3] if these roles should see different menus.
    INSERT INTO sys_role_menu (role_id, menu_id, created_at)
    SELECT r.id, m.id, now()
    FROM sys_role r
    JOIN tmp_roles tr ON tr.code = r.code
    JOIN sys_menu m ON m.id = ANY (ARRAY[1,2,3]::BIGINT[])
    ON CONFLICT (role_id, menu_id) DO NOTHING;

    CREATE TEMP TABLE tmp_users (
        username VARCHAR(64) PRIMARY KEY,
        realname VARCHAR(64) NOT NULL,
        department_id BIGINT NOT NULL,
        role_code VARCHAR(64) NOT NULL,
        seq INT NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO tmp_users (username, realname, department_id, role_code, seq)
    VALUES
        ('rd001', 'Lin Zhiyuan', v_product_dept_id, 'rd_manager', 1),
        ('rd002', 'Zhou Mingzhe', v_product_dept_id, 'rd_manager', 2),
        ('be001', 'Chen Jingxing', v_product_dept_id, 'backend_engineer', 3),
        ('be002', 'Xu Wenbo', v_product_dept_id, 'backend_engineer', 4),
        ('be003', 'Gao Yiming', v_product_dept_id, 'backend_engineer', 5),
        ('be004', 'Luo Chengyu', v_product_dept_id, 'backend_engineer', 6),
        ('be005', 'Tang Siyuan', v_product_dept_id, 'backend_engineer', 7),
        ('fe001', 'Shen Ruolin', v_product_dept_id, 'frontend_engineer', 8),
        ('fe002', 'Lu Qingyang', v_product_dept_id, 'frontend_engineer', 9),
        ('fe003', 'Han Yutong', v_product_dept_id, 'frontend_engineer', 10),
        ('fe004', 'Zhao Yanqi', v_product_dept_id, 'frontend_engineer', 11),
        ('fe005', 'Song Zhixia', v_product_dept_id, 'frontend_engineer', 12),
        ('qa001', 'Wang Jianing', v_qa_dept_id, 'qa_engineer', 13),
        ('qa002', 'Li Muyang', v_qa_dept_id, 'qa_engineer', 14),
        ('qa003', 'Zheng Anqi', v_qa_dept_id, 'qa_engineer', 15),
        ('qa004', 'He Siqi', v_qa_dept_id, 'qa_engineer', 16),
        ('qa005', 'Jiang Ruoxi', v_qa_dept_id, 'qa_engineer', 17),
        ('ops001', 'Wu Yihang', v_ops_dept_id, 'ops_specialist', 18),
        ('ops002', 'Feng Xingchen', v_ops_dept_id, 'ops_specialist', 19),
        ('ops003', 'Ma Yutong', v_ops_dept_id, 'ops_specialist', 20),
        ('ops004', 'Zhu Chenxi', v_ops_dept_id, 'ops_specialist', 21),
        ('ops005', 'Hu Jiashu', v_ops_dept_id, 'ops_specialist', 22),
        ('ops006', 'Sun Zhuoran', v_ops_dept_id, 'ops_specialist', 23),
        ('sales001', 'Liu Zihan', v_sales_dept_id, 'sales_admin', 24),
        ('sales002', 'Yuan Kexin', v_sales_dept_id, 'sales_admin', 25),
        ('sales003', 'Peng Haoran', v_sales_dept_id, 'sales_admin', 26),
        ('sales004', 'Cao Yufei', v_sales_dept_id, 'sales_admin', 27),
        ('sales005', 'Deng Qihang', v_sales_dept_id, 'sales_admin', 28),
        ('adm001', 'Ye Anran', v_admin_dept_id, 'sales_admin', 29),
        ('adm002', 'Liang Shuyao', v_admin_dept_id, 'sales_admin', 30),
        ('adm003', 'Jin Zeyu', v_admin_dept_id, 'sales_admin', 31),
        ('adm004', 'Xue Zixuan', v_admin_dept_id, 'sales_admin', 32);

    INSERT INTO sys_user (
        tenant_id, username, realname, nickname, avatar, email, phone,
        password_hash, user_type, status, need_password_change,
        failed_login_count, locked_until, last_login_at, last_login_ip,
        created_by, created_at, updated_by, updated_at, deleted
    )
    SELECT
        'default',
        username,
        realname,
        realname,
        NULL,
        username || '@qingxu.example',
        '1390001' || lpad(seq::TEXT, 4, '0'),
        v_password_hash,
        'INTERNAL',
        'ACTIVE',
        TRUE,
        0,
        NULL,
        NULL,
        NULL,
        v_admin2_id,
        now(),
        v_admin2_id,
        now(),
        0
    FROM tmp_users;

    INSERT INTO sys_user_department (user_id, department_id, created_at)
    SELECT u.id, tu.department_id, now()
    FROM tmp_users tu
    JOIN sys_user u ON u.username = tu.username AND u.deleted = 0
    ON CONFLICT (user_id, department_id) DO NOTHING;

    INSERT INTO sys_user_role (user_id, role_id, created_at)
    SELECT u.id, r.id, now()
    FROM tmp_users tu
    JOIN sys_user u ON u.username = tu.username AND u.deleted = 0
    JOIN sys_role r ON r.code = tu.role_code
    ON CONFLICT (user_id, role_id) DO NOTHING;

    SELECT count(*) INTO v_user_count
    FROM sys_user
    WHERE deleted = 0
      AND username <> 'admin2';

    SELECT count(*) INTO v_role_count
    FROM sys_role
    WHERE code IN (SELECT code FROM tmp_roles);

    SELECT count(*) INTO v_dept_count
    FROM sys_department
    WHERE deleted = 0
      AND name IN ('产品研发部', '测试质量部', '运营交付部', '市场销售部', '综合管理部');

    SELECT count(*) INTO v_missing_user_rel_count
    FROM sys_user u
    WHERE u.deleted = 0
      AND u.username <> 'admin2'
      AND (
          NOT EXISTS (SELECT 1 FROM sys_user_department ud WHERE ud.user_id = u.id)
          OR NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id)
      );

    IF v_user_count <> 32 THEN
        RAISE EXCEPTION 'Expected 32 active business users, got %', v_user_count;
    END IF;

    IF v_role_count <> 6 THEN
        RAISE EXCEPTION 'Expected 6 business roles, got %', v_role_count;
    END IF;

    IF v_dept_count <> 5 THEN
        RAISE EXCEPTION 'Expected 5 business departments, got %', v_dept_count;
    END IF;

    IF v_missing_user_rel_count <> 0 THEN
        RAISE EXCEPTION 'Some active business users do not have department or role relation. Count: %', v_missing_user_rel_count;
    END IF;

    RAISE NOTICE 'Backoffice data rebuild complete. business_users=%, business_roles=%, business_departments=%',
        v_user_count, v_role_count, v_dept_count;
END $$;

COMMIT;
