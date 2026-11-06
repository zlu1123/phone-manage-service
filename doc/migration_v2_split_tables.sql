-- ============================================================
-- phone_active_info 拆表迁移脚本
-- 拆为三张表：主表 + 详情表 + 签约表
-- 执行前请务必备份数据库！
-- ============================================================

-- 1. 创建详情表 phone_active_detail
CREATE TABLE IF NOT EXISTS `phone_active_detail` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT(20) NOT NULL COMMENT '订单ID，关联 phone_active_info.id',
    `activated` TINYINT(1) DEFAULT NULL COMMENT '鸭宝激活状态：1-已激活 0-未激活',
    `activate_date` VARCHAR(50) DEFAULT NULL COMMENT '旧手机激活日期',
    `coverage` VARCHAR(50) DEFAULT NULL COMMENT '旧手机保修到期时间',
    `active_info` TEXT COMMENT '原始查询结果 JSON',
    `sys_time` VARCHAR(50) DEFAULT NULL COMMENT '查询时系统时间',
    `image_path` VARCHAR(500) DEFAULT NULL COMMENT '上传图片相对路径，多个用逗号分隔',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表（06 API 返回的大字段）';

-- 2. 创建签约表 phone_order_contract
CREATE TABLE IF NOT EXISTS `phone_order_contract` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT(20) NOT NULL COMMENT '订单ID，关联 phone_active_info.id',
    `contract_id` BIGINT(20) DEFAULT NULL COMMENT '协议ID',
    `contract_path` VARCHAR(500) DEFAULT NULL COMMENT '协议内容相对路径',
    `contract_content` TEXT COMMENT '协议内容（富文本，历史快照）',
    `signature_model` VARCHAR(100) DEFAULT NULL COMMENT '协议签订设备型号',
    `signature_imei` VARCHAR(50) DEFAULT NULL COMMENT '协议签订设备IMEI',
    `signature_date` VARCHAR(50) DEFAULT NULL COMMENT '协议签订日期',
    `signature_path` VARCHAR(500) DEFAULT NULL COMMENT '手写签名图片相对路径',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单签约表';

-- 3. 数据迁移：从 phone_active_info 迁移到 phone_active_detail
INSERT INTO phone_active_detail (order_id, activated, activate_date, coverage, active_info, sys_time, image_path)
SELECT id, activated, activate_date, coverage, active_info, sys_time, image_path
FROM phone_active_info;

-- 4. 数据迁移：从 phone_active_info 迁移到 phone_order_contract（仅迁移有签约信息的行）
INSERT INTO phone_order_contract (order_id, contract_id, contract_path, contract_content,
                                   signature_model, signature_imei, signature_date, signature_path)
SELECT id, contract_id, contract_path, contract_content,
       signature_model, signature_imei, signature_date, signature_path
FROM phone_active_info
WHERE signature_imei IS NOT NULL OR contract_id IS NOT NULL;

-- 5. 删除 phone_active_info 中已迁移的列
ALTER TABLE phone_active_info
    DROP COLUMN activated,
    DROP COLUMN activate_date,
    DROP COLUMN coverage,
    DROP COLUMN active_info,
    DROP COLUMN sys_time,
    DROP COLUMN image_path,
    DROP COLUMN contract_id,
    DROP COLUMN contract_path,
    DROP COLUMN contract_content,
    DROP COLUMN signature_model,
    DROP COLUMN signature_imei,
    DROP COLUMN signature_date,
    DROP COLUMN signature_path;

-- 6. 回填历史数据的 store_id（根据创建者的部门层级推算所属门店）
-- 递归逻辑：从用户 dept 出发，向上查找 parent_id=201(通信源手机) 的直接子节点
UPDATE phone_active_info pai
JOIN sys_user u ON pai.create_by = u.user_name
JOIN sys_dept d ON u.dept_id = d.dept_id
SET pai.store_id = (
    SELECT d2.dept_id FROM sys_dept d2
    WHERE d2.parent_id = 201
      AND FIND_IN_SET(d2.dept_id, d.ancestors) > 0
    LIMIT 1
)
WHERE pai.store_id IS NULL;

-- ============================================================
-- 验证：检查迁移结果
-- ============================================================
-- SELECT COUNT(*) AS '主表记录数' FROM phone_active_info;
-- SELECT COUNT(*) AS '详情表记录数' FROM phone_active_detail;
-- SELECT COUNT(*) AS '签约表记录数' FROM phone_order_contract;
-- SELECT COUNT(*) AS '详情表匹配数' FROM phone_active_info pai INNER JOIN phone_active_detail pad ON pai.id = pad.order_id;
