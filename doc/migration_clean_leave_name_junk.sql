-- ============================================================
-- 留资信息姓名清洗脚本（去掉导入时混入的“（金卡）”“(金卡)”等注释）
-- 目标数据库：124.222.38.87 / phone-manage（MySQL 5.7.44）
--
-- 背景：
--   Excel 导入时，姓名列被录入了“（金卡）”“(金卡）”“(金卡)”“（金卡1825）”
--   以及“机子非本店购买”“谭总熟人”等注释内容。
--   订单表 phone_active_info 通过 info_id LEFT JOIN 本表的 name 展示姓名，
--   因此只清洗本表即可，订单列表/导出的用户信息自动同步更新，无需改订单表。
--
-- 执行方式：
--   mysql -h 124.222.38.87 -u root -p --default-character-set=utf8mb4 phone-manage < migration_clean_leave_name_junk.sql
--
-- 执行前请务必：
--   1. 先只执行「第一步 预检」，确认影响行数与页面看到的一致
--   2. 备份数据（脚本内置备份表，建议再整体 mysqldump 一次）：
--      mysqldump -h 124.222.38.87 -u root -p phone-manage > phone_manage_backup_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================

USE `phone-manage`;

-- ============================================================
-- 第一步：预检（只读，不修改任何数据）
-- ============================================================

-- 1.1 名字中含任意括号（全角/半角）的行数 —— 预期覆盖所有带注释的姓名
SELECT COUNT(*) AS paren_total
FROM leave_information
WHERE name LIKE '%（%' OR name LIKE '%(%';

-- 1.2 名字中残留“金卡”字样的行数
SELECT COUNT(*) AS jinka_total
FROM leave_information
WHERE name LIKE '%金卡%';

-- 1.3 抽查具体内容，人工确认清洗范围
SELECT id, name, phone_num, create_by, create_time
FROM leave_information
WHERE name LIKE '%（%' OR name LIKE '%(%' OR name LIKE '%金卡%'
ORDER BY id DESC
LIMIT 50;

-- ============================================================
-- 第二步：备份受影响行
-- ============================================================
CREATE TABLE IF NOT EXISTS `leave_information_backup_20260904` LIKE `leave_information`;
INSERT INTO `leave_information_backup_20260904`
SELECT * FROM `leave_information`
WHERE `name` LIKE '%（%' OR `name` LIKE '%(%' OR `name` LIKE '%金卡%';

-- ============================================================
-- 第三步：清洗姓名
--
-- 3.1 截掉第一个“（”或“(”及其之后的所有内容。
--     覆盖： （金卡） (金卡) （金卡) (金卡） （金卡1825）
--           以及 “刘方（金卡）机子非本店购买”“王祉棋（金卡）谭总熟人”等
--     MySQL 5.7 无 REGEXP_REPLACE，用 SUBSTRING_INDEX 实现。
--     注意：括号前无有效文字的行（如名字只有注释）会被排除，保持原样，
--           第四步复核时单独查看处理。
-- ============================================================
UPDATE `leave_information`
SET `name` = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(`name`, '（', 1), '(', 1)),
    `update_time` = NOW()
WHERE (`name` LIKE '%（%' OR `name` LIKE '%(%')
  AND TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(`name`, '（', 1), '(', 1)) <> '';

-- 3.2 去掉截断后残留的结尾逗号（如“绍瑞佳，（金卡）” -> “绍瑞佳”）
UPDATE `leave_information`
SET `name` = TRIM(TRAILING '，' FROM TRIM(TRAILING ',' FROM `name`))
WHERE `name` LIKE '%，' OR `name` LIKE '%,';

-- 3.3 兜底：去掉名字中不带括号残留的“金卡”文字
UPDATE `leave_information`
SET `name` = TRIM(REPLACE(`name`, '金卡', '')),
    `update_time` = NOW()
WHERE `name` LIKE '%金卡%';

-- 3.4 （可选）若预检发现还有其他不带括号的注释词，取消注释并追加
--     从 08-30 备份库见到过的词：机子非本店购买 / 谭总熟人
-- UPDATE `leave_information`
-- SET `name` = TRIM(REPLACE(REPLACE(`name`, '机子非本店购买', ''), '谭总熟人', '')),
--     `update_time` = NOW()
-- WHERE `name` LIKE '%机子非本店购买%' OR `name` LIKE '%谭总熟人%';

-- ============================================================
-- 第四步：复核
-- ============================================================

-- 4.1 清洗后残留的括号/金卡姓名（预期为 0，若有则逐条人工处理）
SELECT COUNT(*) AS remain_paren
FROM leave_information
WHERE name LIKE '%（%' OR name LIKE '%(%';

SELECT COUNT(*) AS remain_jinka
FROM leave_information
WHERE name LIKE '%金卡%';

SELECT id, name, phone_num
FROM leave_information
WHERE name LIKE '%（%' OR name LIKE '%(%' OR name LIKE '%金卡%';

-- 4.2 抽查清洗结果（与备份表对比）
SELECT b.id, b.name AS before_name, a.name AS after_name
FROM leave_information_backup_20260904 b
LEFT JOIN leave_information a ON a.id = b.id
LIMIT 30;
