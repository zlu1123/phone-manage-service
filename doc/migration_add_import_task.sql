-- ============================================================
-- 订单导入任务表迁移脚本
-- 背景：导入改为「异步任务 + 全量原子」模式：
--   - 上传后立即返回 taskId，后台线程执行导入，前端轮询本表查询进度；
--   - 任意一行导入失败则整体回滚（含留资自动创建），任务标记为失败并记录错误明细；
--   - 全部数据校验/入库成功才标记任务成功。
--
-- 兼容性说明：
--   - 新表无存量数据，创建即生效；
--   - 必须先执行本脚本再部署新代码：新代码导入时会写入本表，
--     表不存在会报 Unknown table 'phone_import_task' 导致导入失败；
--   - 本脚本仅需执行一次；重复执行时 CREATE 报表已存在（可忽略）。
--
-- 执行方式（数据库名在命令行指定，测试/生产库名可能不同）：
--   测试库：mysql -h 43.164.129.96 -u root -p ry-vue < migration_add_import_task.sql
--   生产库：mysql -h 124.222.38.87 -u root -p <生产库名> < migration_add_import_task.sql
--          （生产库名以线上实际为准：DEPLOY.md 记为 ry-vue，历史脚本记为 phone_manage）
--
-- 执行前请务必：
--   1. 备份数据库！
--   2. 在非业务高峰期执行
--   3. 先在测试库验证，再执行生产
-- ============================================================

-- status 取值：0-进行中 1-成功 2-失败（与页面轮询逻辑一致）
-- message：任务结果摘要，成功时为导入统计，失败时为全部错误明细（含回滚说明）
CREATE TABLE `phone_import_task` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`         VARCHAR(64)  NOT NULL COMMENT '任务ID（UUID，接口查询用）',
    `file_name`       VARCHAR(255) DEFAULT NULL COMMENT '导入文件名',
    `status`          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '任务状态：0-进行中 1-成功 2-失败',
    `total_count`     INT          NOT NULL DEFAULT 0 COMMENT '导入总行数',
    `processed_count` INT          NOT NULL DEFAULT 0 COMMENT '已处理行数（实时进度）',
    `success_count`   INT          NOT NULL DEFAULT 0 COMMENT '新增成功数',
    `update_count`    INT          NOT NULL DEFAULT 0 COMMENT '更新数',
    `fail_count`      INT          NOT NULL DEFAULT 0 COMMENT '失败数（失败时整体回滚）',
    `message`         TEXT         COMMENT '结果信息（成功统计/错误与提示明细）',
    `create_by`       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间（含进度更新）',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status_update` (`status`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单导入任务';

-- ============================================================
-- 校验（可选）
--   SHOW TABLES LIKE 'phone_import_task';   -- 表存在
--   DESCRIBE phone_import_task;             -- 列结构
-- ============================================================
