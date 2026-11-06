-- ============================================================
-- 导入订单类型修正脚本（可重复执行）
-- 背景：生产环境当前运行的旧版导入代码会把 Excel 导入的订单落库为
--   skip_api_call=0 / old_phone_status=NULL，前端显示为「旧手机识别」。
--   业务上所有导入订单（无旧手机信息）应统一显示「无旧手机新手机签约」，
--   即 skip_api_call=1 / old_phone_status=0（对应前端 index.vue 订单类型逻辑）。
--   id >= 443 为导入数据起点（443 之前为 App 端店员创建的订单，勿动）。
--
-- 使用场景：客户每导入一批新数据后，执行一次本脚本。
-- 幂等性：已修正的行不满足 WHERE 条件，不会重复更新；无新导入时影响 0 行。
--
-- 执行方式（生产库）：
--   mysql -h 124.222.38.87 -u root -p phone-manage < migration_fix_imported_contract_flag.sql
--
-- 执行前建议（可选）：先备份本次将修改的行
--   CREATE TABLE phone_active_info_backup_<日期>_fix AS
--     SELECT * FROM phone_active_info
--     WHERE id >= 443 AND NOT (skip_api_call = 1 AND old_phone_status = 0);
-- ============================================================

UPDATE phone_active_info
SET skip_api_call = 1,
    old_phone_status = 0,
    update_time = now()
WHERE id >= 443
  AND NOT (skip_api_call = 1 AND old_phone_status = 0);

-- 校验（期望：still_wrong = 0）
SELECT COUNT(*) AS still_wrong
FROM phone_active_info
WHERE id >= 443
  AND NOT (skip_api_call = 1 AND old_phone_status = 0);
