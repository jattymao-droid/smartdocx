-- Portal teacher role + subject read access (idempotent)

DO $$
DECLARE
    role_id_teacher bigint;
BEGIN
    SELECT role_id INTO role_id_teacher
    FROM sys_role
    WHERE role_key = 'edu_teacher' AND del_flag = '0';

    IF role_id_teacher IS NULL THEN
        INSERT INTO sys_role (
            role_name, role_key, role_sort, data_scope,
            menu_check_strictly, dept_check_strictly,
            status, del_flag, create_by, create_time, remark
        )
        VALUES (
            E'\u4efb\u8bfe\u6559\u5e08', 'edu_teacher', 4, '1',
            1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP,
            E'\u95e8\u6237\u6559\u5e08\u89d2\u8272'
        )
        RETURNING role_id INTO role_id_teacher;
    END IF;

    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT role_id_teacher, m.menu_id
    FROM (VALUES
        (201070),
        (20120), (201201), (201202), (201203), (201204), (201205), (201206),
        (20125), (201251), (201252), (201253), (201254), (201255)
    ) AS m(menu_id)
    WHERE NOT EXISTS (
        SELECT 1 FROM sys_role_menu rm
        WHERE rm.role_id = role_id_teacher AND rm.menu_id = m.menu_id
    );
END $$;
