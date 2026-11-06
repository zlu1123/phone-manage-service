-- ============================================================
-- 订单表新增「逻辑删除」字段迁移脚本
-- 背景：支持 admin 将单条/多条订单标记为「测试数据」，逻辑删除后
--       列表、导出、仪表盘统计均不再展示（数据保留在库中）
--
-- 兼容性说明：
--   - 加列无损：NOT NULL DEFAULT '0'，存量数据全部视为正常订单
--   - 必须先执行本脚本再部署新代码：新代码的列表/统计 SQL 均带
--     del_flag 条件，列不存在会报 Unknown column 'del_flag'
--   - 本脚本仅需执行一次；重复执行时 ALTER 报列已存在（可忽略）
--
-- 执行方式（数据库名在命令行指定）：
--   测试库：mysql -h 43.164.129.96 -u root -p ry-vue < migration_add_order_del_flag.sql
--   生产库：mysql -h 124.222.38.87 -u root -p <生产库名> < migration_add_order_del_flag.sql
--
-- 执行前请务必：
--   1. 备份数据库！
--   2. 在非业务高峰期执行
--   3. 先在测试库验证，再执行生产
-- ============================================================

-- del_flag 取值与 RuoYi 惯例一致：0=正常 2=已删除（测试数据）
ALTER TABLE `phone_active_info`
    ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0=正常 2=测试数据/已逻辑删除）'
        AFTER `channel`;

-- ============================================================
-- 校验（可选）
--   DESCRIBE phone_active_info del_flag;             -- 列存在
--   SELECT del_flag, COUNT(*) FROM phone_active_info GROUP BY del_flag;  -- 应为 0
-- ============================================================
