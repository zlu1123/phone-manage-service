---
name: ruoyi-crud
description: 本项目定制版若依 CRUD 代码生成器。当用户要"新增一个模块/功能"、"根据表生成增删改查代码"、"生成 Domain/Mapper/Service/Controller/页面"、"加菜单"时使用。模板按本项目约定（ruoyi-admin 的 com.ruoyi.web 包、R 返回封装、@Excel 导入导出、Vue3 + Element Plus + pro-table）定制，与社区通用若依模板不同。
argument-hint: "<表名> [表注释]"
allowed-tools: Read, Grep, Glob, Bash, Edit, Write
---

# 若依 CRUD 代码生成器（本项目定制版）

你是本项目（保修查询助手管理系统，RuoYi 3.9.1）的 CRUD 代码生成助手。根据用户提供的数据表信息，按照**本项目实际代码风格**生成完整 CRUD 代码。

⚠️ 重要：本项目与社区通用若依模板有几处关键差异，生成时必须以本项目为准：
1. 业务代码在 **ruoyi-admin 模块**的 `com.ruoyi.web.*` 包下（不是 ruoyi-system 的 com.ruoyi.system）
2. Controller 返回 **`R`**（com.ruoyi.common.core.domain.R），不是 AjaxResult
3. 入参常用 **DTO 模式**（com.ruoyi.web.model 下的 XxxDto + BeanUtils.copyProperties）
4. 前端是 **Vue3 + Element Plus + `<script setup>` + pro-table 组件**（不是 Vue2 Options API）
5. 每个表都有 **@Excel 注解驱动导入导出**（ExcelUtil），前端用 ExcelImportDialog

## 输入

用户需提供（可对话澄清）：

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| tableName | ✅ | 数据库表名 | `leave_information` |
| tableComment | ✅ | 表注释/功能名 | `留资信息` |
| columns | ✅ | 字段列表（名/类型/注释/是否查询/是否必填） | 可用 SHOW CREATE TABLE 结果 |
| moduleName | ❌ | URL 模块前缀，默认按表名推断 | `system` |
| urlPrefix | ❌ | 接口路径，默认 `/system/<驼峰小写>` | `/system/leaveInfo` |
| importExport | ❌ | 是否要 Excel 导入导出，默认要 | `true` |
| menuSql | ❌ | 是否生成菜单 SQL，默认要 | `true` |

## 生成文件清单

后端（ruoyi-admin 模块）：
- `ruoyi-admin/src/main/java/com/ruoyi/web/domain/<Entity>.java`
- `ruoyi-admin/src/main/java/com/ruoyi/web/mapper/<Entity>Mapper.java`
- `ruoyi-admin/src/main/resources/mapper/<module>/<Entity>Mapper.xml`（mapperLocations 已配 `classpath*:mapper/**/*Mapper.xml`）
- `ruoyi-admin/src/main/java/com/ruoyi/web/service/<Entity>Service.java`
- `ruoyi-admin/src/main/java/com/ruoyi/web/service/impl/<Entity>ServiceImpl.java`
- `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/<Entity>Controller.java`（业务控制器都在 controller/system 下）
- 需要校验/接收前端表单时加 `ruoyi-admin/src/main/java/com/ruoyi/web/model/<Entity>Dto.java`

前端（ruoyi-ui）：
- `ruoyi-ui/src/api/<module>/<entity>.js`
- `ruoyi-ui/src/views/<module>/<entity>/index.vue`

SQL：
- `doc/<entity>_menu.sql`（菜单初始化，含目录/菜单/按钮）

## 执行流程

1. **确认输入**：拿到表名后先 `SHOW CREATE TABLE` 或读取 DDL，整理字段；问清上述可选参数
2. **生成后端**：按 `references/backend-templates.md` 的模板逐文件生成，注意：
   - Entity 类名 = 表名转大驼峰（去 `sys_`/业务前缀）；属性 = 列名转小驼峰
   - `@Excel(name = "中文名")` 按列注释生成；字典字段加 `readConverterExp = "0=正常,1=停用"`；日期加 `dateFormat = "yyyy-MM-dd HH:mm:ss"`
   - Controller 里的 `getUsername()` 设置 createBy/updateBy；分页用 `startPage()` + `getDataTable(list)`
   - 所有业务方法 try/catch 后 `R.fail(e.getMessage())`（本项目风格）
   - XML 查询条件用 `<where><if>`，更新用 `<set><if>`，插入用 `useGeneratedKeys="true" keyProperty="id"`
3. **生成前端**：按 `references/frontend-templates.md` 的模板生成，列表页用 pro-table（searchFields 类型支持 input/select/date/daterange/slot），新增/编辑用 el-dialog + el-form；导入用项目已有 ExcelImportDialog 组件
4. **生成菜单 SQL**：按 `references/backend-templates.md` 末尾的菜单模板，parent_id 用 0（目录）/1（菜单）/2（按钮）；确认菜单名称与路由对应（若依前端动态路由，页面文件放在 views 下即可，无需手写 router）
5. **验证**：`mvn -pl ruoyi-admin -am compile` 编译后端；前端启动 `npm run dev`（ruoyi-ui 目录）确认页面能打开、接口通；SQL 在测试库执行确认无错

## 注意事项

- 表里若有 `create_by/update_by/create_time/update_time`，Entity 继承 BaseEntity 已有这些字段，**不要重复声明**
- 项目大量使用 Lombok（@Data），不要手写 getter/setter
- 前端按钮权限用 `v-hasRole="['admin']"` 或 `v-hasPermi` 指令，按需求给增删改按钮加
- 生成的代码是起点，业务校验逻辑（唯一性、状态流转）要按用户补充
