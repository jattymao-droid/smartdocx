# SmartDocx · 东陆智能教学库

基于 [RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud) 的智慧教学平台：面向教师与教研场景，提供**题库组卷**、**教学文库**、**试卷导出**与**门户浏览**能力。

仓库地址：https://github.com/jattymao-droid/smartdocx

---

## 功能概览

| 模块 | 说明 |
|------|------|
| 门户首页 | 最新文档、推荐/热门、学科导航、热门专题 |
| 智慧题库 | 题目管理、章节教材目录、选题组卷、练习、试卷分享 |
| 教学文库 | 文档上传、在线预览（PDF/Office/压缩包内文件）、收藏下载、VIP/付费专题 |
| 试卷工作台 | Word/PDF 导出、答题卡、我的试卷 |
| 后台管理 | 若依权限体系 + 题库/文库/专题/支付等管理端 |

默认账号：`admin` / `admin123`

---

## 技术栈

**后端**

- Java 17、Spring Boot 4.x、Spring Cloud / Alibaba
- Nacos（注册与配置）、Gateway、Redis
- PostgreSQL（业务库 `ry_cloud`）
- MyBatis、Druid

**前端**

- Vue 2 + Element UI（`ruoyi-ui`）
- 门户布局与管理端分离路由（`/` 门户，`/admin` 后台）

**周边能力**

- kkFileView + LibreOffice：Office / 压缩包预览转换
- 可选：OCR 服务、组卷网采集器（`zujuan_collector`）

---

## 目录结构

```text
SmartDocx/
├── ruoyi-ui/                 # 前端（门户 + 管理端）
├── ruoyi-gateway/            # API 网关 :8080
├── ruoyi-auth/               # 认证中心 :9200
├── ruoyi-modules/
│   ├── ruoyi-system/         # 系统 + 教育业务（题库/文库）:9201
│   ├── ruoyi-file/           # 文件服务 :9300
│   ├── ruoyi-gen/            # 代码生成
│   └── ruoyi-job/            # 定时任务
├── config/                   # 本地 Nacos 配置源（publish 用）
├── scripts/                  # 本地启动、配置发布、kkFileView 等
├── sql/postgresql/           # PostgreSQL 初始化与业务补丁
├── docs/                     # 设计文档（如文库设计）
├── docker/                   # Docker Compose（可选）
└── zujuan_collector/         # 组卷采集工具（可选）
```

---

## 环境要求

| 依赖 | 建议版本 / 说明 |
|------|----------------|
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 16+（推荐 18/20） |
| PostgreSQL | 16（库名 `ry_cloud`，默认端口 `5432`） |
| Redis | 6+（`6379`） |
| Nacos | 3.x（脚本可启动，`8848` / 控制台 `8850`） |
| kkFileView | 压缩包与 Office 预览需要（脚本可启动，`8012`） |

本地配置目录：`config/*.yml`（发布到 Nacos 的 `DEFAULT_GROUP`）。请将其中的数据库密码等改为你的环境值，勿把真实密钥提交到公开仓库。

---

## 快速启动（Windows）

### 1. 准备数据库

创建数据库并导入 SQL（按顺序执行）：

1. `sql/postgresql/ry_cloud.sql`（或项目内官方库脚本）
2. `sql/postgresql/question_bank/install_all.sql`（题库相关）
3. `sql/postgresql/library/` 下 schema / patch（文库、专题、VIP 等）

具体以 `sql/postgresql/` 目录与 `docs/` 说明为准。

### 2. 编译后端

在仓库根目录：

```powershell
# 若使用仓库外 Maven，请改成你的 mvn 路径
mvn clean package "-Dmaven.test.skip=true" -pl ruoyi-auth,ruoyi-gateway,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-file -am
```

### 3. 安装前端依赖

```powershell
cd ruoyi-ui
npm install
```

### 4. 一键启动本地服务

```powershell
.\scripts\start-local.ps1
```

脚本会按需启动：Nacos → 发布 `config/` → kkFileView → auth / system / file / gateway → 前端 `:8081`。

### 5. 访问地址

| 入口 | URL |
|------|-----|
| 用户门户 | http://localhost:8081 |
| 管理后台 | http://localhost:8081/admin |
| API 网关 | http://localhost:8080 |
| Nacos | http://localhost:8850 |
| kkFileView | http://localhost:8012 |

仅启动文档预览服务：

```powershell
.\scripts\start-kkfileview.ps1
```

首次构建 kkFileView：

```powershell
.\scripts\build-kkfileview.ps1
```

手动发布配置到 Nacos：

```powershell
python .\scripts\publish_local_config.py
```

---

## 常用端口

| 服务 | 端口 |
|------|------|
| 前端 | 8081 |
| Gateway | 8080 |
| Auth | 9200 |
| System | 9201 |
| File | 9300 |
| Nacos | 8848 / 8850 |
| Redis | 6379 |
| PostgreSQL | 5432 |
| kkFileView | 8012 |

---

## 开发说明

- 教育业务代码主要在：
  - 后端：`ruoyi-modules/ruoyi-system/.../education/`
  - 前端门户：`ruoyi-ui/src/views/portal/`
  - 前端管理：`ruoyi-ui/src/views/education/`
- 文库设计文档：`docs/文库-设计开发文档.md`
- 压缩包在列表中可关联热门专题（`bundle_document_id`），点击跳转专题页而非单文件预览页
- `tools/`、`packages/`、`logs/` 等大体积本地工具默认不纳入 Git（见 `.gitignore`）

---

## 许可证

本项目基于若依 RuoYi-Cloud 二次开发，遵循 [MIT License](./LICENSE)。
