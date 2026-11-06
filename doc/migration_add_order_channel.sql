-- ============================================================
-- 订单表新增「渠道」字段迁移脚本
-- 背景：Excel 导入时用户只能提供渠道（自有/亚丁），无法提供旧手机
--       照片、序列号、保修期等识别数据，渠道需直接落库供列表展示
--
-- 兼容性说明：
--   - 阶段一（加列）无损：旧数据 channel 为 NULL，列表/导出在 channel
--     为空时仍走原有推导逻辑，展示结果不变
--   - 阶段二（回填）与前端 getChannel 推导规则逐行对齐（含 NULL 边界
--     情况），回填后历史订单展示的渠道与迁移前一致，仅从「实时推导」
--     变为「落库值」，无行为变化
--   - 必须先执行本脚本再部署新代码：新代码插入时会写 channel 列，
--     列不存在会报 Unknown column 'channel' 导致下单/导入失败
--   - 本脚本仅需执行一次；重复执行时 ALTER 报列已存在（可忽略），
--     UPDATE 可重复执行无副作用
--
-- 执行方式（数据库名在命令行指定，测试/生产库名可能不同）：
--   测试库：mysql -h 43.164.129.96 -u root -p ry-vue < migration_add_order_channel.sql
--   生产库：mysql -h 124.222.38.87 -u root -p <生产库名> < migration_add_order_channel.sql
--          （生产库名以线上实际为准：DEPLOY.md 记为 ry-vue，历史脚本记为 phone_manage）
--
-- 执行前请务必：
--   1. 备份数据库！
--   2. 在非业务高峰期执行
--   3. 先在测试库验证，再执行生产
-- ============================================================

-- ============================================================
-- 阶段一：phone_active_info 新增 channel 列
--   允许 NULL，旧数据兼容，不影响现有业务
-- ============================================================

ALTER TABLE `phone_active_info`
    ADD COLUMN `channel` VARCHAR(10) DEFAULT NULL COMMENT '订单渠道：自有/亚丁（导入时直接指定；历史数据为空时前端按规则推导）'
        AFTER `skip_api_call`;

-- ============================================================
-- 阶段二：按前端既有规则回填历史数据渠道
--   与前端 getChannel 推导逻辑完全一致（含 NULL 边界）：
--     skip_api_call=0（API识别）：在保（coverage > sysTime，各取前10位日期）→ 亚丁，否则 → 自有
--     skip_api_call≠0 且 old_phone_status=0（无旧手机）→ 自有
--     skip_api_call≠0 且 old_phone_status≠0（含 NULL）且 使用月数=0（≤24月）→ 亚丁，否则 → 自有
-- ============================================================

UPDATE `phone_active_info` pai
LEFT JOIN `phone_active_detail` pad ON pai.id = pad.order_id
SET pai.channel = CASE
    WHEN pai.skip_api_call = 0
         AND pad.coverage IS NOT NULL AND pad.coverage != ''
         AND pad.sys_time IS NOT NULL AND pad.sys_time != ''
         AND LEFT(pad.coverage, 10) > LEFT(pad.sys_time, 10) THEN '亚丁'
    WHEN COALESCE(pai.skip_api_call, 1) = 1
         AND COALESCE(pai.old_phone_status, 1) = 1
         AND pai.old_phone_usage_months = 0 THEN '亚丁'
    ELSE '自有'
END;

-- ============================================================
-- 校验（可选）：查看回填结果分布
--   SELECT channel, COUNT(*) FROM phone_active_info GROUP BY channel;
--   SELECT * FROM phone_active_info WHERE channel IS NULL;  -- 应为空
-- ============================================================
