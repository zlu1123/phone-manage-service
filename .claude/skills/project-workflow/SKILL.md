---
name: project-workflow
description: "本项目的开发工作流与交付约定(保修查询助手管理系统/RuoYi-Vue)。开发或修改任何功能、写迁移 SQL 前必读:Claude 只写代码与 doc/migration_*.sql 迁移脚本,不执行后端编译(mvn)、不执行前端编译(npm build/lint)、不启动服务、不验证;由用户本地自动编译并自行启动开发环境验证,交付时须输出用户验证清单。Project dev workflow: Claude writes code only, user compiles and verifies locally."
license: MIT
metadata:
  version: "1.0.0"
---

# 项目开发工作流

## 何时使用

- 在本仓库开发或修改任何功能(后端 Java、前端 Vue3、迁移 SQL)之前;
- 用户说"帮我加/改一个功能、修一个 bug、写个迁移"等开发类请求时;
- 需要交付代码并告知用户如何验证时。

## 核心分工(硬约束)

### Claude 的职责

1. 编写/修改后端 Java 代码(ruoyi-admin 等模块)与前端 Vue3 代码(ruoyi-ui);
2. 涉及表结构或数据变更时,产出迁移 SQL 到 `doc/migration_*.sql`;
3. 交付时输出「改动摘要 + 用户验证清单」。

### Claude 禁止做的事

- ❌ 运行 `mvn compile` / `mvn package` 等后端编译;
- ❌ 运行 `npm run build` / `npm run lint` / `npm run dev` 等前端编译或启动;
- ❌ 启动前后端服务、用 curl 请求接口做验证;
- ❌ 以任何方式"顺手"验证——编译与验证全部由用户完成。

> 这是用户明确要求的约定:用户本地 IDE 会自动编译后端,并自行启动开发环境验证,Claude 再编译属于重复劳动。交付后停在代码层面,等用户验证反馈。

### 用户的验证流程(Claude 不参与)

1. 本地 IDE 自动编译后端;
2. 用户自行启动开发环境:后端 ruoyi-admin + 前端 `npm run dev`;
3. 用户验证通过 → 结束;不通过 → 反馈问题,Claude 只根据反馈改代码,依然不编译不验证。

## 项目结构速览

| 目录 | 说明 |
|---|---|
| `ruoyi-admin` | 主应用:启动入口、Controller、业务 Service/Mapper,端口 **8090**,Spring profile `druid` |
| `ruoyi-common` / `ruoyi-framework` / `ruoyi-system` / `ruoyi-quartz` / `ruoyi-generator` | 若依框架层,一般不改 |
| `ruoyi-ui` | 前端:Vue 3.5 + Vite 6 + Element Plus 2.13,`npm run dev` 端口 **1024**,`/dev-api` 代理到 `VITE_APP_BASE_URL`,值写在 `ruoyi-ui/.env.development`(当前 `http://www.ybxz.top:8090`;`localhost:8090` 与测试环境 `43.164.129.96:8090` 均已注释,切后端只改这一行) |
| `doc/` | 迁移 SQL(`migration_*.sql`)、项目文档 |
| `sql/` | 初始化/全量 SQL |
| `config-sh/` | 部署配置 |

环境依赖:Java 8 + MySQL + Redis(127.0.0.1:6379)。

### 前端约定(ruoyi-ui)

日常前端改动(改字段展示、加查询条件、调样式、加按钮权限)按这几条走,不要另起一套写法:

- **路由由后端菜单动态生成**:新页面只需放到 `ruoyi-ui/src/views/<模块>/` 下,不要手写 `src/router`;
- 接口按模块放 `ruoyi-ui/src/api/<模块>/`,统一走 `src/utils/request.js`,不要在组件里直接调 `axios`;
- 按钮权限用 `v-hasPermi="['模块:功能:操作']"` / `v-hasRole`;字典展示用 `DictTag` / `useDict`;
- 优先复用已有组件,不要新引 UI 库:列表用 `src/components/ProTable`,导入用 `ExcelImportDialog`,还有 `Pagination`、`RightToolbar`;
- 需要新增依赖前先说明理由(见 ponytail 规则 4),能复用现有组件就不要加包。

## 交付标准(每次改完代码)

1. **代码改动**完成;
2. **DB 变更** → 生成 `doc/migration_*.sql`,沿用现有 header 格式(背景 / 兼容性说明 / 执行方式 / 执行前必读),参考 `doc/migration_add_import_task.sql`;
3. **用户验证清单**:本次改了哪些功能、如何触发、预期行为、需要先执行的迁移 SQL(如有);
4. 明确告知:**未编译、未验证**,请用户启动开发环境验证。

## 数据库约定

- 测试库:`43.164.129.96`(库名 `ry-vue`);生产库:`124.222.38.87`(库名以线上实际为准,`DEPLOY.md` 记为 `ry-vue`);
- 迁移 SQL 必须先于新代码部署执行(新代码依赖新表/新列时);
- 每个迁移脚本仅需执行一次,写清幂等性说明。

## 相关 skill

- [ruoyi-crud](../ruoyi-crud/SKILL.md):生成新的 CRUD 模块(后端 + 前端模板);
- [db-migration-reviewer](../db-migration-reviewer/SKILL.md):迁移 SQL 上线前评审;
- [bug-diagnoser](../bug-diagnoser/SKILL.md):用户反馈验证问题后排查 bug。

## 注意事项

- 不编译、不验证是用户明确要求的约定,不要"顺手"跑 mvn/npm 验证命令;
- 若其他 skill(如 ponytail)要求"自行验证 / 留一个测试",以本条为准:agent 交付「代码 + 清单里的自检说明」(改了哪些分支、哪些边界情况没覆盖),**编译与运行的验证动作全部由用户执行**;
- 用户反馈验证结果后继续迭代,迭代时依然不自行编译;
- 交付清单要具体到用户能直接照着操作(哪个页面、点什么按钮、预期看到什么)。
