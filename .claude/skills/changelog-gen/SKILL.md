---
name: changelog-gen
description: 自动生成更新日志。当用户说"生成更新日志"、"changelog"、"版本记录"、"更新记录"时使用此技能。
argument-hint: "[版本号] [--since=<commit|date>]"
allowed-tools: Read, Grep, Glob, Bash, Edit, Write
---

# 更新日志生成技能

你是更新日志助手，负责从 Git 提交记录中提取有意义的变更，生成格式化的 changelog。

## 参数说明

- `$ARGUMENTS` 支持以下参数：
  - 无参数：从上次 tag 或最近 30 天的提交生成
  - `v1.2.0`：指定版本号
  - `--since=abc1234`：从指定 commit 开始
  - `--since=2026-03-01`：从指定日期开始
  - 可组合：`v1.2.0 --since=v1.1.0`

## 核心配置

- **项目路径**: 仓库根目录（单仓库，后端 `ruoyi-admin` 与前端 `ruoyi-ui` 同仓）
- **远程仓库**: `https://github.com/zlu1123/phone-manage-service`
- **Changelog 文件**: `CHANGELOG.md`（仓库根目录，不存在则创建）

## 执行流程

### 第一步：获取提交记录

```bash
git log <起点>..HEAD --format="%H|%aI|%s|%an" --no-merges
```

无起点时，优先用最近一次 tag（`git describe --tags --abbrev=0`），没有 tag 则取最近 30 天。

### 第二步：分类提交

按 Conventional Commits 规范分类：

| 前缀 | 分类 | 展示标题 |
|------|------|---------|
| `feat:` / `✨` | 新功能 | ✨ 新功能 |
| `fix:` / `🐛` | 修复 | 🐛 Bug 修复 |
| `docs:` / `📝` | 文档 | 📝 文档更新 |
| `perf:` / `⚡` | 性能 | ⚡ 性能优化 |
| `refactor:` / `♻️` | 重构 | ♻️ 代码重构 |
| `style:` / `💄` | 样式 | 💄 样式调整 |
| `chore:` / `🔧` | 杂务 | 🔧 其他 |
| `BREAKING CHANGE` | 破坏性变更 | ⚠️ 破坏性变更 |

无前缀的提交根据内容智能分类。

按提交涉及路径标注所属端：`ruoyi-admin/**` 为后端，`ruoyi-ui/**` 为前端，其余为其他（部署脚本、SQL 等）。在条目描述后标注，如 `（后端）`、`（前端）`。

### 第三步：生成 Changelog

```markdown
# 更新日志

## [版本号] - 日期

### ⚠️ 破坏性变更

- 变更描述

### ✨ 新功能

- 功能描述 ([commit](https://github.com/zlu1123/phone-manage-service/commit/<hash>))

### 🐛 Bug 修复

- 修复描述

### 📝 文档更新

- 文档描述

### ⚡ 性能优化

- 优化描述

### ♻️ 代码重构

- 重构描述
```

### 第四步：更新 CHANGELOG.md

将新版本内容插入到 `CHANGELOG.md` 的顶部（在已有内容之前），保留历史记录。

### 第五步：输出结果

```markdown
## Changelog 已生成

- **版本**: <版本号>
- **日期范围**: <起始日期> → <结束日期>
- **提交数**: N 个
- **分类统计**:
  - 新功能: N
  - 修复: N
  - 文档: N
  - 其他: N
- **文件**: `CHANGELOG.md`
```

## 注意事项

1. **过滤无意义提交** — 跳过 merge commit、CI/CD 相关、格式化提交
2. **合并相关提交** — 同一功能的多次提交合并为一条记录
3. **保留已有内容** — 只在文件顶部插入，不覆盖历史记录
4. **破坏性变更置顶** — BREAKING CHANGE 始终放在最前面
