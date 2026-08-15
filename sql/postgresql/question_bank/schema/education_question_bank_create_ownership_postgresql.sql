-- -----------------------------------------------------------------------------
-- education_question_bank_create_ownership_postgresql.sql
-- 导入/审核权限归属「新增试题」(20122)
-- -----------------------------------------------------------------------------

update sys_menu
set parent_id = 20122,
    order_num = 1,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 201207;

update sys_menu
set parent_id = 20122,
    order_num = 2,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 201208;

-- 按钮权限排序：查询/新增/修改等仍挂在题库管理下
update sys_menu
set order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201201;
update sys_menu
set order_num = 4, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201202;
update sys_menu
set order_num = 5, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201203;
update sys_menu
set order_num = 6, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201204;
update sys_menu
set order_num = 7, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201205;
update sys_menu
set order_num = 8, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201206;
