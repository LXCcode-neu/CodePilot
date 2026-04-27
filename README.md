# CodePilot

中文 | English

---

## 中文说明

### 项目简介

CodePilot 是一个基于 Spring Boot 3 的 Issue-to-PR Agent 平台，面向 GitHub Issue 驱动的代码修复场景。

当前仓库同时包含：

- Spring Boot 3 后端服务
- 位于 `frontend/` 的 React + Vite 前端控制台

当前项目已经完成基础工程初始化，并实现了第一阶段的认证能力：

- 用户注册
- 用户登录
- JWT 鉴权
- 当前登录用户信息获取

### 当前已实现

- Java 17 + Spring Boot 3 + Maven 工程
- MyBatis-Plus / MySQL / Redis 基础依赖
- Spring Security + JWT 认证骨架
- 用户注册接口 `POST /api/auth/register`
- 用户登录接口 `POST /api/auth/login`
- 当前用户接口 `GET /api/user/me`
- 基于 JGit 的公开 GitHub 仓库 clone
- Tree-sitter 多语言索引基础模型骨架（Java / Python / JavaScript / TypeScript / Go）
- Tree-sitter 统一 AST 解析层与语言注册表
- 基于 Lucene 的多语言代码检索与重排能力
- PlainTextFallbackExtractor 文本级兜底索引抽象
- `frontend/` 前端项目：React + Vite + TypeScript + TailwindCSS + shadcn/ui
- 统一返回 `Result<T>`
- 错误码枚举 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常处理 `GlobalExceptionHandler`
- 基础实体 `BaseEntity`

### 当前未实现

以下模块当前仍然只保留骨架，尚未实现具体业务逻辑：

- 仓库管理
- Agent 任务创建
- 复杂符号提取规则
- Java 文件扫描执行流程
- 修复建议和 patch 生成
- 执行轨迹展示
- LLM 调用逻辑

### 包结构

```text
src/main/java/com/codepliot
├─ auth
├─ user
├─ project
├─ task
├─ git
├─ index
├─ agent
├─ llm
├─ trace
├─ sse
└─ common

frontend
├─ src
├─ package.json
└─ vite.config.ts
```

### 配置文件

核心配置文件：

- [application.yml](/C:/CodePilot/src/main/resources/application.yml)

启动前请准备：

- MySQL 数据库 `codepilot`
- Redis 服务
- 合法的 JWT 密钥
- 可写的工作目录根路径 `codepilot.workspace.root`

### 启动方式

使用 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

或使用本机 Maven：

```powershell
mvn spring-boot:run
```

### 前端开发

前端项目位于 `frontend/`：

```powershell
cd frontend
npm install
npm run dev
```

- Vite 开发端口：`5173`
- `/api` 会通过 Vite proxy 转发到 `http://localhost:8080`

### 前端构建并接入 Spring Boot

在 `frontend/` 目录执行：

```powershell
npm run build:static
```

执行后会：

- 先构建前端到 `frontend/dist`
- 再复制到 `src/main/resources/static`

随后启动 Spring Boot，即可通过 `http://localhost:8080` 访问前端首页。

此外，`/login`、`/register`、`/projects`、`/tasks`、`/tasks/new`、`/tasks/{id}` 这些前端路由也会由 Spring Boot 转发到 `index.html`。

### 认证接口

注册：

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "demo_user",
  "password": "123456",
  "email": "demo@example.com"
}
```

登录：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "demo_user",
  "password": "123456"
}
```

获取当前用户：

```http
GET /api/user/me
Authorization: Bearer <token>
```

### 下一步建议

建议下一步实现 `project` 模块，包括：

- 仓库接入
- 公开仓库基础信息管理
- 后续 Git 拉取入口

当前更自然的下一步是继续完成 `index` 模块，把文件遍历、Tree-sitter 解析结果适配、语言提取器落库流程真正串起来。

---

## English

### Overview

CodePilot is an Issue-to-PR Agent platform built with Spring Boot 3 for GitHub Issue driven code-repair workflows.

This repository now includes:

- the Spring Boot backend
- a React + Vite frontend console under `frontend/`

The project now includes the initial backend scaffold and the first usable authentication layer:

- user registration
- user login
- JWT authentication
- current user info retrieval

### Implemented

- Java 17 + Spring Boot 3 + Maven
- MyBatis-Plus / MySQL / Redis base dependencies
- Spring Security + JWT auth scaffold
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/user/me`
- public GitHub repository clone with JGit
- Tree-sitter based multi-language index scaffolding for Java, Python, JavaScript, TypeScript, and Go
- Tree-sitter parser registry and unified AST parsing layer
- Tree-sitter multi-language symbol extractors for Java, Python, JavaScript, TypeScript, and Go
- code index build service that scans repositories and persists `code_file` / `code_symbol`
- Apache Lucene local index build for `code_symbol` under `workspace/{projectId}/lucene-index`
- multi-language Lucene code search with reranking for `SearchRelevantCodeTool`
- Redis-backed task run lock to prevent duplicate `/run` execution for the same task
- Spring Async background execution for agent tasks, while `/run` returns immediately
- SSE task event subscription via `GET /api/tasks/{taskId}/events`
- configurable LLM abstraction with mock and DeepSeek-backed clients
- issue analysis and patch generation prompt builders
- `GET /api/tasks/{taskId}/patch` for generated patch record retrieval
- plain-text fallback extractor abstraction
- `frontend/` web console with React, Vite, TypeScript, TailwindCSS, and shadcn-style UI components
- unified `Result<T>`
- `ErrorCode`
- `BusinessException`
- `GlobalExceptionHandler`
- `BaseEntity`

### Not Implemented Yet

The following modules are still placeholders only:

- repository management
- agent task creation
- complex symbol extraction rules
- executable file scanning flow
- execution trace display

### Configuration

Main configuration file:

- [application.yml](/C:/CodePilot/src/main/resources/application.yml)

Before starting the project, prepare:

- MySQL database `codepilot`
- Redis instance
- a valid JWT secret
- a writable workspace root via `codepilot.workspace.root`
- an optional task run lock TTL via `codepilot.lock.task-run.ttl` (default `30m`)
- `code_file` and `code_symbol` will now be auto-created on Spring Boot startup if they do not exist
- Lucene index files are rebuilt under `workspace/{projectId}/lucene-index` during the code-index build step

### Async Task Execution Note

- The current MVP uses Spring Async plus Redis task locks instead of MQ.
- This is sufficient because the app is still a monolith and task execution depends on local workspace files and local Lucene indexes.
- If the system later evolves into multi-instance scheduling or dedicated worker nodes, RabbitMQ or Redis Stream would be a more suitable upgrade path.

### Tree-sitter Native Note

- The current `ch.usi.si.seart:java-tree-sitter:1.12.0` integration compiles on Java 17 and is wrapped with graceful fallback.
- If the native library or a language grammar is unavailable, `TreeSitterParserService` returns `success=false` and upper layers should use `PlainTextFallbackExtractor`.
- In the current local verification, this artifact reports `The tree-sitter library was not compiled for this platform: windows 11`, so Windows development machines will currently enter fallback mode unless a Windows-compatible native build is provided.

### Run

Using Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Or with local Maven:

```powershell
mvn spring-boot:run
```

### Frontend Development

The frontend lives in `frontend/`:

```powershell
cd frontend
npm install
npm run dev
```

- Vite dev server runs on `5173`
- `/api` is proxied to `http://localhost:8080`

### Build Frontend for Spring Boot

Inside `frontend/` run:

```powershell
npm run build:static
```

This builds the frontend and copies the generated files into `src/main/resources/static`.

After that, starting Spring Boot lets you access the frontend at `http://localhost:8080`.

The Spring Boot app also forwards `/login`, `/register`, `/projects`, `/tasks`, `/tasks/new`, and `/tasks/{id}` to `index.html` so direct page visits work.

### Recommended Next Step

The natural next step is to continue the `index` module by wiring file traversal, parser adaptation, and persistence for multi-language code indexing.
