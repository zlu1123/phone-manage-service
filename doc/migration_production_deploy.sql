-- ============================================================
-- 生产数据库变更脚本（更新版）
-- 目标数据库：124.222.38.87 / phone_manage
-- 从 commit 3d3c08d 到当前 HEAD (cfd564a) 的所有数据库变更
--
-- 执行方式：
--   mysql -h 124.222.38.87 -u root -p phone_manage < migration_production_deploy.sql
-- 
-- 执行前请务必：
--   1. 备份数据库！备份数据库！备份数据库！
--      mysqldump -h 124.222.38.87 -u root -p phone_manage > phone_manage_backup_$(date +%Y%m%d_%H%M%S).sql
--   2. 在非业务高峰期执行
--   3. 建议先在测试库验证
-- ============================================================

USE `phone_manage`;

-- ============================================================
-- 阶段一：phone_active_info 表新增字段
--   先加列（NULL 允许旧数据兼容），不影响现有业务
-- ============================================================

ALTER TABLE `phone_active_info`
    ADD COLUMN `info_id`            BIGINT(20)   DEFAULT NULL COMMENT '留资人ID，关联 leave_information.id',
    ADD COLUMN `store_id`           BIGINT(20)   DEFAULT NULL COMMENT '所属门店ID，关联 sys_dept.dept_id',
    ADD COLUMN `skip_api_call`      TINYINT(1)   DEFAULT 0    COMMENT '是否跳过06 API查询：0-正常查询 1-无旧手机跳过',
    ADD COLUMN `old_phone_status`   TINYINT(1)   DEFAULT NULL COMMENT '旧手机状态：0-无旧手机 1-损坏/丢失',
    ADD COLUMN `old_phone_usage_months` INT(11)  DEFAULT NULL COMMENT '旧手机已使用月数';

ALTER TABLE `phone_active_info`
    ADD INDEX `idx_info_id` (`info_id`),
    ADD INDEX `idx_store_id` (`store_id`);

-- ============================================================
-- 阶段二：新建 leave_information（留资信息表）
-- ============================================================

CREATE TABLE IF NOT EXISTS `leave_information` (
    `id`          BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(64) DEFAULT '' COMMENT '姓名',
    `phone_num`   VARCHAR(20) DEFAULT '' COMMENT '电话号码',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `create_by`   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    `update_by`   VARCHAR(64) DEFAULT '' COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_num` (`phone_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留资信息表';

-- ============================================================
-- 阶段三：新建 compensation_order（赔付订单表）
-- ============================================================

CREATE TABLE IF NOT EXISTS `compensation_order` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`         BIGINT(20)   DEFAULT NULL COMMENT '订单ID，关联 phone_active_info.id',
    `info_id`          BIGINT(20)   DEFAULT NULL COMMENT '留资人ID，关联 leave_information.id',
    `amount`           DOUBLE       DEFAULT NULL COMMENT '赔付金额',
    `status`           INT(11)      DEFAULT NULL COMMENT '状态',
    `rejection_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    `remark`           VARCHAR(500) DEFAULT NULL COMMENT '备注信息',
    `create_time`      DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`        VARCHAR(64)  DEFAULT '' COMMENT '创建人',
    `update_time`      DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`        VARCHAR(64)  DEFAULT '' COMMENT '修改人',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_info_id` (`info_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赔付订单表';

-- ============================================================
-- 阶段四：新建 contract（协议管理表）
--   管理协议模板，支持版本控制和生效状态
-- ============================================================

CREATE TABLE IF NOT EXISTS `contract` (
    `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(255) DEFAULT '' COMMENT '协议名称',
    `version`     VARCHAR(64)  DEFAULT '' COMMENT '协议版本号',
    `content`     TEXT         DEFAULT NULL COMMENT '协议内容（富文本）',
    `file_path`   VARCHAR(500) DEFAULT '' COMMENT '协议文件地址',
    `status`      TINYINT(1)   DEFAULT 0 COMMENT '是否生效（0：未生效，1：生效中）',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建人',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '修改人',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议管理表';

-- ============================================================
-- 阶段五：拆分 phone_active_info → phone_active_detail + phone_order_contract
--   将主表中的大字段和签约字段拆到两个独立的子表
--   主表保留核心高频查询字段，子表存储大字段和低频字段
-- ============================================================

-- 5.1 创建详情表 phone_active_detail
CREATE TABLE IF NOT EXISTS `phone_active_detail` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      BIGINT(20)   NOT NULL COMMENT '订单ID，关联 phone_active_info.id',
    `activated`     TINYINT(1)   DEFAULT NULL COMMENT '鸭宝激活状态：1-已激活 0-未激活',
    `activate_date` VARCHAR(50)  DEFAULT NULL COMMENT '旧手机激活日期',
    `coverage`      VARCHAR(50)  DEFAULT NULL COMMENT '旧手机保修到期时间',
    `active_info`   TEXT         DEFAULT NULL COMMENT '原始查询结果 JSON',
    `sys_time`      VARCHAR(50)  DEFAULT NULL COMMENT '查询时系统时间',
    `image_path`    VARCHAR(500) DEFAULT NULL COMMENT '上传图片相对路径，多个用逗号分隔',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表（06 API 返回的大字段）';

-- 5.2 创建签约表 phone_order_contract
CREATE TABLE IF NOT EXISTS `phone_order_contract` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`         BIGINT(20)   NOT NULL COMMENT '订单ID，关联 phone_active_info.id',
    `contract_id`      BIGINT(20)   DEFAULT NULL COMMENT '协议ID',
    `contract_path`    VARCHAR(500) DEFAULT NULL COMMENT '协议内容相对路径',
    `contract_content` TEXT         DEFAULT NULL COMMENT '协议内容（富文本，历史快照）',
    `signature_model`  VARCHAR(100) DEFAULT NULL COMMENT '协议签订设备型号',
    `signature_imei`   VARCHAR(50)  DEFAULT NULL COMMENT '协议签订设备IMEI',
    `signature_date`   VARCHAR(50)  DEFAULT NULL COMMENT '协议签订日期',
    `signature_path`   VARCHAR(500) DEFAULT NULL COMMENT '手写签名图片相对路径',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单签约表';

-- 5.3 数据迁移：phone_active_info → phone_active_detail
INSERT INTO `phone_active_detail` (`order_id`, `activated`, `activate_date`, `coverage`, `active_info`, `sys_time`, `image_path`)
SELECT `id`, `activated`, `activate_date`, `coverage`, `active_info`, `sys_time`, `image_path`
FROM `phone_active_info`;

-- 5.4 数据迁移：phone_active_info → phone_order_contract（仅迁移有签约信息的行）
INSERT INTO `phone_order_contract` (`order_id`, `contract_id`, `contract_path`, `contract_content`,
                                     `signature_model`, `signature_imei`, `signature_date`, `signature_path`)
SELECT `id`, `contract_id`, `contract_path`, `contract_content`,
       `signature_model`, `signature_imei`, `signature_date`, `signature_path`
FROM `phone_active_info`
WHERE `signature_imei` IS NOT NULL OR `contract_id` IS NOT NULL;

-- 5.5 删除 phone_active_info 中已迁移到子表的列（⚠️ 不可逆，确认迁移数据无误再执行）
ALTER TABLE `phone_active_info`
    DROP COLUMN `activated`,
    DROP COLUMN `activate_date`,
    DROP COLUMN `coverage`,
    DROP COLUMN `active_info`,
    DROP COLUMN `sys_time`,
    DROP COLUMN `image_path`,
    DROP COLUMN `contract_id`,
    DROP COLUMN `contract_path`,
    DROP COLUMN `contract_content`,
    DROP COLUMN `signature_model`,
    DROP COLUMN `signature_imei`,
    DROP COLUMN `signature_date`,
    DROP COLUMN `signature_path`;

-- ============================================================
-- 阶段六：回填历史数据的 store_id
--   根据创建者的部门层级推算所属门店
--   从用户 dept 出发，向上查找 parent_id=201（通信源手机）的直接子节点
-- ============================================================

UPDATE `phone_active_info` pai
JOIN `sys_user` u  ON pai.create_by = u.user_name
JOIN `sys_dept` d  ON u.dept_id = d.dept_id
SET pai.store_id = (
    SELECT d2.dept_id FROM `sys_dept` d2
    WHERE d2.parent_id = 201
      AND FIND_IN_SET(d2.dept_id, d.ancestors) > 0
    LIMIT 1
)
WHERE pai.store_id IS NULL;

-- ============================================================
-- 验证：检查所有表的数据完整性
-- ============================================================

-- 各表记录数
SELECT 'phone_active_info'    AS `表名`, COUNT(*) AS `记录数` FROM `phone_active_info`    UNION ALL
SELECT 'phone_active_detail'  AS `表名`, COUNT(*) AS `记录数` FROM `phone_active_detail`  UNION ALL
SELECT 'phone_order_contract' AS `表名`, COUNT(*) AS `记录数` FROM `phone_order_contract` UNION ALL
SELECT 'leave_information'    AS `表名`, COUNT(*) AS `记录数` FROM `leave_information`    UNION ALL
SELECT 'compensation_order'   AS `表名`, COUNT(*) AS `记录数` FROM `compensation_order`   UNION ALL
SELECT 'contract'             AS `表名`, COUNT(*) AS `记录数` FROM `contract`;

-- 拆表数据迁移完整性校验
SELECT
    (SELECT COUNT(*) FROM `phone_active_info`)                        AS `主表数`,
    (SELECT COUNT(*) FROM `phone_active_detail`)                      AS `详情表数`,
    (SELECT COUNT(*) FROM `phone_active_info` pai
     INNER JOIN `phone_active_detail` pad ON pai.id = pad.order_id)   AS `详情匹配数`,
    (SELECT COUNT(*) FROM `phone_order_contract`)                     AS `签约表数`,
    (SELECT COUNT(*) FROM `phone_active_info` pai
     INNER JOIN `phone_order_contract` poc ON pai.id = poc.order_id)  AS `签约匹配数`;
