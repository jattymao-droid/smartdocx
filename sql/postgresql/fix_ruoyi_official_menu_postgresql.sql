-- Fix default RuoYi official site menu: hide from sidebar (optional external link)
update sys_menu
set visible = '1',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 4 and path = 'http://ruoyi.vip';
