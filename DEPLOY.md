# phone-manage-service 生产环境变更文档

> **基准版本**: commit `3d3c08d5b982e5543f31265900339e3ff4d84bdc` (生产环境当前版本)  
> **目标版本**: HEAD (测试环境当前代码)  
> **变更日期**: 2026-07-09  
> **涉及数据库**: `ry-vue` @ `124.222.38.87` (生产) / `43.164.129.96` (测试)

---

## 目录

- [一、变更总览](#一变更总览)
- [二、数据库变更（DDL）](#二数据库变更ddl)
- [三、后端代码变更](#三后端代码变更)
- [四、前端代码变更](#四前端代码变更)
- [五、索引优化](#五索引优化)
- [六、表结构优化方案](#六表结构优化方案)
- [七、部署步骤](#七部署步骤)
- [八、回滚方案](#八回滚方案)
- [九、验证清单](#九验证清单)

---

## 一、变更总览

| 维度 | 基准 (3d3c08d) | 目标 (HEAD) | 变化 |
|:---|:---|:---|:---|
| 数据库表 | 2 张 (`phone_active_info`, `contract`) | 4 张 (+`leave_information`, `compensation_order`) | +2 张 |
| `phone_active_info` 列 | 23 列 | 27 列 | +4 列 |
| 后端文件 | ~200 个 | ~230 个 | +30 个 |
| 前端框架 | Vue2 + Webpack | Vue3 + Vite | 全面重构 |
| 关键新增功能 | - | 留资人管理 / 赔付订单 / 跳过API / Excel导入 | 4+ 新功能 |

---

## 二、数据库变更（DDL）

### ⚠️ 执行顺序保证：先执行 DDL，再部署代码

```sql
-- ============================================================
-- Step 1: phone_active_info 新增 4 个字段
-- 影响：ALTER TABLE 在线执行，不锁表（MySQL 5.7+）
-- 时间：< 1秒（列允许NULL，无需回填）
-- ============================================================
ALTER TABLE phone_active_info 
    ADD COLUMN info_id              BIGINT      DEFAULT NULL COMMENT '留资人ID'        AFTER update_by,
    ADD COLUMN skip_api_call        TINYINT(1)  DEFAULT 0    COMMENT '跳过API:0否1是' AFTER info_id,
    ADD COLUMN old_phone_status     TINYINT(4)  DEFAULT NULL COMMENT '旧手机状态:0无旧手机1损坏/丢失' AFTER skip_api_call,
    ADD COLUMN old_phone_usage_months INT       DEFAULT NULL COMMENT '旧手机使用月数'  AFTER old_phone_status;

-- ============================================================
-- Step 2: 新建 leave_information（留资人信息表）
-- 时间：< 1秒
-- ============================================================
CREATE TABLE IF NOT EXISTS leave_information (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32) NOT NULL              COMMENT '留资人姓名',
    phone_num   VARCHAR(16) NOT NULL              COMMENT '留资人电话号码',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by   VARCHAR(64) NOT NULL              COMMENT '创建人',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by   VARCHAR(64) NOT NULL              COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留资信息表';

-- ============================================================
-- Step 3: 新建 compensation_order（赔付订单表）
-- 时间：< 1秒
-- ============================================================
CREATE TABLE IF NOT EXISTS compensation_order (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id         BIGINT       NOT NULL              COMMENT '订单ID',
    info_id          BIGINT       NOT NULL              COMMENT '留资人ID',
    amount           DOUBLE       DEFAULT NULL          COMMENT '赔付金额',
    status           INT          DEFAULT 0             COMMENT '状态',
    rejection_reason VARCHAR(500) DEFAULT NULL          COMMENT '驳回原因',
    remark           VARCHAR(500) DEFAULT NULL          COMMENT '备注',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by        VARCHAR(64)  DEFAULT NULL          COMMENT '创建人',
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by        VARCHAR(64)  DEFAULT NULL          COMMENT '更新人',
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赔付订单表';

-- ============================================================
-- Step 4: phone_active_info 新增「渠道」字段（Excel 导入适配）
-- 背景：导入时用户仅能提供渠道（自有/亚丁），无旧手机照片/序列号/保修期
-- 时间：< 1秒（列允许NULL）
-- 历史数据回填脚本见 doc/migration_add_order_channel.sql
-- ============================================================
ALTER TABLE phone_active_info
    ADD COLUMN channel VARCHAR(10) DEFAULT NULL COMMENT '订单渠道：自有/亚丁（导入时直接指定）' AFTER skip_api_call;

-- ============================================================
-- Step 5: phone_active_info 新增「删除标志」字段（测试数据逻辑删除）
-- 背景：admin 可将单条/多条订单标记为测试数据，逻辑删除后列表/导出/统计不再展示
-- 时间：< 1秒（NOT NULL DEFAULT '0'，存量数据不受影响）
-- 执行脚本见 doc/migration_add_order_del_flag.sql
-- ============================================================
ALTER TABLE phone_active_info
    ADD COLUMN del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0=正常 2=测试数据/已逻辑删除）' AFTER channel;
```

### DDL 验证

```sql
-- 执行后确认结构
DESCRIBE phone_active_info;
DESCRIBE leave_information;
DESCRIBE compensation_order;

-- 确认无数据丢失
SELECT COUNT(*) FROM phone_active_info;   -- 应与变更前一致
```

---

## 三、后端代码变更

### 3.1 实体类变更

#### PhoneActiveInfo.java — 新增字段 + Excel注解
**文件**: `ruoyi-admin/src/main/java/com/ruoyi/web/domain/PhoneActiveInfo.java`

| 变更 | 说明 |
|:---|:---|
| 新增 `@Excel` 注解 | 所有数据库字段增加 `@Excel` 注解，支持导入导出 |
| 新增 `infoId` | 留资人关联ID |
| 新增 `skipApiCall` | 是否跳过06 API查询 |
| 新增 `oldPhoneStatus` | 旧手机状态 |
| 新增 `oldPhoneUsageMonths` | 旧手机使用月数 |
| 新增 `phoneNum` | 留资人电话（非DB字段，关联返回） |
| 新增 `name` | 留资人姓名（非DB字段，关联返回） |
| 新增 `isSignature` | 是否已签约（非DB字段，关联返回） |
| `createTime`/`updateTime` | 从 `BaseEntity` 重写到子类以加 `@Excel` 注解 |

#### 新增实体类

| 文件 | 用途 |
|:---|:---|
| `domain/LeaveInformation.java` | 留资人实体 |
| `domain/CompensationOrder.java` | 赔付订单实体 |
| `model/LeaveInformationDto.java` | 留资人DTO |
| `model/CompensationOrderDto.java` | 赔付订单DTO |
| `model/CompensationOrderReturnDto.java` | 赔付订单返回DTO |
| `model/SamsungPhoneObject.java` | 三星设备信息 |
| `model/OppoPhoneObject.java` | OPPO设备信息 |
| `model/VivoPhoneObject.java` | VIVO设备信息 |

### 3.2 Controller 变更

#### PhoneActiveOrderController — 4个主要变更
**文件**: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/PhoneActiveOrderController.java`

| 变更 | 说明 |
|:---|:---|
| 🔧 `resolveCurrentDate()` | **移除** `ISysConfigService` 依赖，改用 `LocalDate.now()` 直接获取服务器时间 |
| ➕ `querySignContractOrderList()` | 新增已签约订单查询接口 |
| ➕ `importTemplate()` | 新增下载Excel导入模板接口 |
| ➕ `importData()` | 新增Excel批量导入订单接口 |
| ➕ `getContractContent()` | 新增根据订单ID获取签约协议内容接口 |

#### WechatApiController — 重大扩展
**文件**: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/wechat/WechatApiController.java`

| 变更 | 说明 |
|:---|:---|
| 🔧 `queryActiveInfo()` | 新增 `infoId`、`skipApiCall`、`oldPhoneStatus`、`oldPhoneUsageMonths` 参数 |
| ➕ `handleSkipApiCall()` | 跳过06 API的新逻辑（无旧手机 / 损坏丢失场景） |
| 🔧 `saveActiveInfo()` | 新增 `infoId`、`skipApiCall` 参数，支持关联留资人和跳过API标记 |
| ➕ `insertLeaveInformation()` | 新增留资人信息接口 |
| ➕ `getLeaveInformationList()` | 查询留资人列表接口 |
| ➕ `insertCompensationOrder()` | 新增赔付订单接口 |
| ➕ `getCompensationOrderList()` | 查询赔付订单列表接口 |
| ➕ `updateCompensationOrder()` | 更新赔付订单接口 |
| ➕ `querySignContractOrderList()` | 已签约订单查询（小程序端） |

### 3.3 Service 变更

#### PhoneActiveInfoServiceImpl — 新增3个功能
**文件**: `ruoyi-admin/src/main/java/com/ruoyi/web/service/impl/PhoneActiveInfoServiceImpl.java`

| 变更 | 说明 |
|:---|:---|
| ➕ `importActiveInfo()` | Excel批量导入核心逻辑：`@Excel` 注解校验 → SN去重 → 逐条插入/更新 → 结果汇总 |
| ➕ `validateRequiredFields()` | 基于 `@Excel(required=true)` 的反射校验方法 |
| ➕ `getContractContent()` | 根据订单ID查询签约协议内容 |

#### 新增 Service 文件

| 文件 | 用途 |
|:---|:---|
| `LeaveInformationService.java` + `impl/LeaveInforationServiceImpl.java` | 留资人管理 |
| `CompensationOrderService.java` + `impl/CompensationOrderServiceImpl.java` | 赔付订单管理 |
| `SamsungPhoneConverter.java` | 三星设备数据转换 |
| `OppoPhoneConverter.java` | OPPO设备数据转换 |
| `VivoPhoneConverter.java` | VIVO设备数据转换 |

### 3.4 Mapper / SQL 变更

#### PhoneActiveInfoMapper.xml — 核心变更

| 变更 | 说明 |
|:---|:---|
| 🔧 ResultMap | 新增 `phone_num`, `info_id`, `skip_api_call`, `old_phone_status`, `old_phone_usage_months` 映射 |
| 🔧 `Base_Column_List` | 新增 4 个字段 |
| 🔧 `Base_Column_List_Q` | 新增 4 个字段，**去掉 `contract_content`**（查询列表不再加载大字段） |
| 🔧 `insert` | 新增 4 个字段插入 |
| 🔧 `updateById` | 新增 4 个字段更新（跳过 `sysTime` 更新因为它不属于 update） |
| 🔧 `selectByExample` | **重大重构**：改用 `pai.` 前缀 + LEFT JOIN `leave_information` + 新增 `skipApiCall`/`oldPhoneStatus`/`name`/`phoneNum`/`isSignature` 过滤条件 |
| 🔧 `selectDashboardStatistics` | `todayOrders`/`signedOrders` 等日期计算 **去掉 `COALESCE(sys_time, create_time)` 逻辑**，统一使用 `create_time` |
| 🔧 `selectUserStatistics` | 同上 |
| 🔧 `selectOrderTrend` | GROUP BY **去掉 `COALESCE(sys_time, ...)` 逻辑**，统一使用 `create_time` |
| 🔧 `selectMonthlyOrderStats` | 同上 |
| ➕ `selectContractContentById` | 新增按ID查询 `contract_content` 接口 |

#### 新增 Mapper 文件

| 文件 | 用途 |
|:---|:---|
| `mapper/phone/LeaveInfomationMapper.xml` | 留资人CRUD SQL |
| `mapper/phone/CompensationOrderMapper.xml` | 赔付订单CRUD SQL |

### 3.5 Mapper 接口变更

#### PhoneActiveInfoMapper.java
**文件**: `ruoyi-admin/src/main/java/com/ruoyi/web/mapper/PhoneActiveInfoMapper.java`

| 变更 | 说明 |
|:---|:---|
| ➕ `selectContractContentById(@Param("id") Long id)` | 新增方法 |

### 3.6 其他变更

| 文件 | 变更 |
|:---|:---|
| `PhoneInfoConverterContext.java` | 新增三星/Oppo/Vivo converter 注册 |
| `RestTemplateConfig.java` | HTTP连接配置微调 |
| `PhoneType.java` | 新增 3 种品牌枚举值 |
| `ExternalApiService.java` | 查询URL格式调整 |
| `DeviceInfoExtractorService.java` | 新增三星/Oppo/Vivo设备信息提取逻辑 |
| `ExcelUtil.java` | 支持 `@Excel` 注解的 `required` 属性 |
| `Excel.java` | 新增 `required` 属性 |

---

## 四、前端代码变更

前端从 **Vue2 + Webpack (`vue.config.js`)** 全面迁移到 **Vue3 + Vite (`vite.config.js`)**。

### 变更规模
- **231 个文件变更**，+24,066 行，-17,672 行
- 废弃 `vue.config.js`，新增 `vite.config.js`
- 废弃 `public/` 目录，`index.html` 移到根目录

### 关键新增页面

| 页面 | 路由 | 说明 |
|:---|:---|:---|
| 订单列表 | `/order/list` | 替代旧的 `/order/list`，新增导出/导入/签约筛选 |
| 赔付订单 | `/info/compensation` | 赔付管理页 |
| 留资人管理 | `/info/user` | 留资人列表页 |
| 锁屏 | `/lock` | 新增密码锁屏功能 |

### 组件升级

| 旧组件 | 新组件 | 说明 |
|:---|:---|:---|
| `element-ui` | `element-plus` | Vue3 适配 |
| - | `ProTable` | 封装表格+搜索+分页 |
| - | `Watermark` | 水印组件（协议查看时用） |
| - | `TreePanel` | 树形面板 |
| - | `CountTo` | 数字滚动 |
| `ExcelImportDialog` | 新增 | Excel导入弹窗 |

---

## 五、索引优化

### 当前索引（生产环境）

```sql
-- 来自 SHOW INDEX FROM phone_active_info:
PRIMARY KEY (id)
INDEX idx_imei1 (imei1)           -- 使用频率低
INDEX idx_model (model)           -- 使用频率低
INDEX idx_sn (sn)                 -- ⚠️ 普通索引，非 UNIQUE
```

### 推荐新增索引

```sql
-- 1. SN 改为唯一索引（防止重复导入）
ALTER TABLE phone_active_info DROP INDEX idx_sn;
ALTER TABLE phone_active_info ADD UNIQUE INDEX uk_sn (sn);

-- 2. 留资人关联查询加速
ALTER TABLE phone_active_info ADD INDEX idx_info_id (info_id);

-- 3. 仪表盘统计加速（复合索引）
ALTER TABLE phone_active_info ADD INDEX idx_create_by_time (create_by, create_time);

-- 4. 品牌筛选加速
ALTER TABLE phone_active_info ADD INDEX idx_phone_type (phone_type);
```

### 索引收益分析

| 查询场景 | 优化前 | 优化后 |
|:---|:---|:---|
| `selectBySn`（导入去重） | 普通索引扫描 | 唯一索引 O(1) |
| `selectByExample`（品牌筛选） | 全表扫描 | phone_type 索引 |
| 仪表盘统计（按用户） | 全表扫描 + CASE WHEN | create_by_time 复合索引 |
| `LEFT JOIN leave_information` | 无 info_id 索引 | info_id 索引加速 JOIN |

---

## 六、表结构优化方案（建议）

### 6.1 当前问题

`phone_active_info` 表 **27 列**，其中 `contract_content`（富文本HTML）平均 **4,748 字节/行**，是最大的性能瓶颈。

### 6.2 优化方案：拆分签约信息

```sql
-- ============================================================
-- 新建签约信息独立表
-- ============================================================
CREATE TABLE phone_order_contract (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT NOT NULL COMMENT '关联phone_active_info.id',
    contract_id       BIGINT DEFAULT NULL COMMENT '合同模板ID',
    contract_path     VARCHAR(255) DEFAULT NULL COMMENT '协议文件路径',
    contract_content  TEXT COMMENT '协议富文本（历史快照）',
    signature_model   VARCHAR(255) DEFAULT NULL COMMENT '签约设备型号',
    signature_imei    VARCHAR(32) DEFAULT NULL COMMENT '签约设备IMEI',
    signature_date    VARCHAR(16) DEFAULT NULL COMMENT '签约日期',
    signature_path    VARCHAR(255) DEFAULT NULL COMMENT '手写签名图片',
    signed_time       DATETIME DEFAULT NULL COMMENT '签约时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_order_id (order_id),
    INDEX idx_contract_id (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单签约信息';

-- 迁移数据
INSERT INTO phone_order_contract 
    (order_id, contract_id, contract_path, contract_content, 
     signature_model, signature_imei, signature_date, signature_path, 
     signed_time, create_time, update_time)
SELECT 
    id, contract_id, contract_path, contract_content,
    signature_model, signature_imei, signature_date, signature_path,
    CASE WHEN signature_date IS NOT NULL AND signature_date != '' 
         THEN STR_TO_DATE(signature_date, '%Y-%m-%d') ELSE create_time END,
    create_time, update_time
FROM phone_active_info
WHERE contract_id IS NOT NULL OR contract_content IS NOT NULL OR signature_imei IS NOT NULL;

-- 数据校验
SELECT 
    (SELECT COUNT(*) FROM phone_active_info 
     WHERE contract_id IS NOT NULL OR contract_content IS NOT NULL OR signature_imei IS NOT NULL) AS source_cnt,
    (SELECT COUNT(*) FROM phone_order_contract) AS target_cnt;
-- 预期：source_cnt = target_cnt

-- 确认无误后（灰度观察7天），删除主表冗余列
-- ⚠️ 此步骤不可逆，务必在确认稳定后执行
-- ALTER TABLE phone_active_info 
--     DROP COLUMN contract_id,
--     DROP COLUMN contract_path,
--     DROP COLUMN contract_content,
--     DROP COLUMN signature_model,
--     DROP COLUMN signature_imei,
--     DROP COLUMN signature_date,
--     DROP COLUMN signature_path;
```

### 6.3 拆分前后对比

| 指标 | 拆分前 | 拆分后 |
|:---|:---|:---|
| 主表列数 | 27 列 | 20 列 |
| 列表查询加载 | 约 8KB/行（含 HTML） | 约 1KB/行 |
| contract_content 存储 | 混在主表 | 独立表，按需加载 |
| 签约字段增删 | ALTER 主表 | 独立 DDL |

---

## 七、部署步骤

```
阶段 1: 数据库 DDL（建议凌晨低峰期，可在线执行）
┌──────────────────────────────────────────────────────┐
│ ☐ 1.1 备份数据库                                     │
│ ☐ 1.2 执行 ALTER TABLE phone_active_info（新增4列）    │
│ ☐ 1.3 执行 CREATE TABLE leave_information             │
│ ☐ 1.4 执行 CREATE TABLE compensation_order            │
│ ☐ 1.5 执行索引优化 SQL                                │
│ ☐ 1.6 验证表结构 DESCRIBE 三张表                      │
└──────────────────────────────────────────────────────┘

阶段 2: 后端部署
┌──────────────────────────────────────────────────────┐
│ ☐ 2.1 打包: mvn clean package -DskipTests            │
│ ☐ 2.2 备份旧 jar 包                                  │
│ ☐ 2.3 部署新 jar 包 (kill 旧进程 → 启动新进程)        │
│ ☐ 2.4 检查启动日志，确认无异常                        │
│ ☐ 2.5 验证新接口:                                     │
│     - GET  /system/order/querySignContractOrderList  │
│     - POST /system/order/importTemplate              │
│     - GET  /system/order/getContractContent?id=1     │
│     - POST /wechat/insertLeaveInformation            │
└──────────────────────────────────────────────────────┘

阶段 3: 前端部署
┌──────────────────────────────────────────────────────┐
│ ☐ 3.1 确认 .env.production 配置正确 (VITE_APP_BASE_API)│
│ ☐ 3.2 npm install && npm run build:prod             │
│ ☐ 3.3 部署 dist/ 到 nginx                            │
│ ☐ 3.4 验证页面可用                                   │
└──────────────────────────────────────────────────────┘

阶段 4: 签约表拆分（可选，建议独立迭代）
┌──────────────────────────────────────────────────────┐
│ ☐ 4.1 创建 phone_order_contract 表                    │
│ ☐ 4.2 迁移数据 + 校验                                 │
│ ☐ 4.3 改造代码（Mapper + Service）                    │
│ ☐ 4.4 灰度验证 7 天                                   │
│ ☐ 4.5 删除主表冗余列                                  │
└──────────────────────────────────────────────────────┘
```

---

## 八、回滚方案

### 数据库回滚

```sql
-- 如果 ALTER TABLE 出问题（概率极低），回滚新增列
ALTER TABLE phone_active_info 
    DROP COLUMN old_phone_usage_months,
    DROP COLUMN old_phone_status,
    DROP COLUMN skip_api_call,
    DROP COLUMN info_id;

-- 回滚新建表
DROP TABLE IF EXISTS leave_information;
DROP TABLE IF EXISTS compensation_order;
```

### 代码回滚

```bash
# 回退到生产版本
git checkout 3d3c08d5b982e5543f31265900339e3ff4d84bdc

# 重新打包部署
mvn clean package -DskipTests
```

---

## 九、验证清单

### 功能验证

| 序号 | 测试场景 | 验收标准 |
|:---:|:---|:---|
| 1 | 小程序查询激活信息（各类品牌） | 正常返回激活/保修信息 |
| 2 | 小程序跳过API（无旧手机场景） | 正常保存订单，skip_api_call=1 |
| 3 | 小程序新增留资人 | 写入 leave_information 表 |
| 4 | 小程序协议签订 | signature_* 字段正常写入 |
| 5 | 后台订单列表查询 | 正常分页 + 多条件筛选 |
| 6 | 后台Excel导入 | 模板下载 + 数据导入 + 重复SN提示 |
| 7 | 后台Excel导出 | 按筛选条件导出 |
| 8 | 后台仪表盘统计 | 管理员/用户数据卡片正确 |
| 9 | 后台赔付订单 | 新增/查询/更新 |
| 10 | 后台查看签约协议 | getContractContent 返回正确HTML |

### 性能验证

| 序号 | 指标 | 基准值 | 目标值 |
|:---:|:---|:---:|:---:|
| 1 | 订单列表查询耗时 | - | < 500ms |
| 2 | Excel导入 1000 条 | - | < 30s |
| 3 | 仪表盘加载 | - | < 1s |

### 数据一致性

```sql
-- 验证行数
SELECT COUNT(*) FROM phone_active_info;          -- 应与变更前一致
SELECT COUNT(*) FROM leave_information;          -- 应为 0（空表新建）

-- 验证约束
SHOW INDEX FROM phone_active_info WHERE Key_name = 'uk_sn';  -- 应为 UNIQUE
```

---

> **文档版本**: v1.0  
> **编写人**: AI Assistant  
> **最后更新**: 2026-07-09
