# AGENTS

中文 | English

---

## 中文说明

### 文件定位

本文件是 **CodePilot 仓库开发规范**，用于约束后续 Codex 或其他 AI 编码代理在本仓库中的开发行为。

它的目标是统一：

- 技术栈使用范围
- 目录与包结构边界
- 可实现与不可实现内容
- 文档同步要求

### 这份文件不负责什么

本文件 **不是** CodePilot 平台内部业务 Agent 的运行时规范，不描述：

- Issue 修复 Agent 的执行步骤
- 任务编排策略
- LLM 提示词设计
- patch 生成流程
- 平台内 Agent 的输入输出协议

这类内容应放在独立文档中，例如：

- `docs/agent-runtime-spec.md`

### 开发约束

1. 必须使用 Java 17 和 Spring Boot 3。
2. 必须使用 Maven 作为唯一构建工具。
3. 仅允许使用当前项目已确认的技术栈：
   - Spring Web
   - Validation
   - MyBatis-Plus
   - MySQL Driver
   - Redis
   - Spring Security
   - JWT
   - Lombok
   - JGit
   - Tree-sitter
   - Apache Lucene
   - React
   - Vite
   - TypeScript
   - TailwindCSS
   - shadcn/ui 风格组件
   - Axios
   - React Router
   - lucide-react
4. 未经用户明确要求，不要引入新的框架、中间件或前端技术栈。
5. 除非用户明确要求或任务明确针对 `frontend/` 模块，否则不要生成前端代码。
6. 不要伪造已完成的业务逻辑。
7. 所有 Java 包必须保持在 `com.codepliot` 命名空间下。
8. 当前后端采用**扁平主包结构**，不要在每个主层级下继续按业务二次分层；优先通过类名表达语义：
   - `config`
   - `controller`
   - `entity`
   - `exception`
   - `handler`
   - `model`
   - `repository`
   - `service`
   - `client`
   - `utils`
   - `frontend`
9. 允许通过类名前缀或完整类名表达业务归属，例如 `AuthController`、`ProjectRepoService`、`LuceneCodeSearchService`、`PatchGenerateResult`。
10. 在用户没有明确要求前，不要额外实现与当前项目范围无关的新业务闭环。
11. 如果项目结构、技术选型、开发边界或启动方式发生变化，要同步更新 `README.md` 与本文件。
12. 数据库操作放 `repository` 层，业务逻辑放 `service` 层。
13. 新增后端表时，优先提供可重复执行的启动初始化路径，例如 Spring Boot SQL init，而不只依赖手工执行。

### 默认理解

除非用户明确覆盖，本文件对后续 Codex 开发持续生效。

---

## English

### Purpose

This file is the **repository development specification** for CodePilot. It constrains how Codex or other AI coding agents should work inside this repository.

It is intended to standardize:

- approved technology choices
- package structure boundaries
- what should and should not be implemented
- documentation sync requirements

### What This File Is Not

This file is **not** the runtime specification for the business agents inside the CodePilot platform. It does not define:

- issue-fixing agent execution flow
- task orchestration strategy
- LLM prompt design
- patch generation workflow
- runtime input/output contract for platform agents

Those rules should live in a separate document, for example:

- `docs/agent-runtime-spec.md`

### Development Rules

1. Use Java 17 and Spring Boot 3.
2. Use Maven as the only build tool.
3. Only use the approved stack in this repository:
   - Spring Web
   - Validation
   - MyBatis-Plus
   - MySQL Driver
   - Redis
   - Spring Security
   - JWT
   - Lombok
   - JGit
   - Tree-sitter
   - Apache Lucene
   - React
   - Vite
   - TypeScript
   - TailwindCSS
   - shadcn/ui style components
   - Axios
   - React Router
   - lucide-react
4. Do not introduce new frameworks, middleware, or frontend stacks unless explicitly requested.
5. Do not generate frontend code unless the user explicitly asks for it or the task clearly targets the `frontend/` module.
6. Do not fake completed business logic.
7. Keep all Java packages under the `com.codepliot` namespace.
8. The backend now uses a **flat top-level package layout**. Do not keep creating domain subpackages under each main layer; prefer semantic class names instead:
   - `config`
   - `controller`
   - `entity`
   - `exception`
   - `handler`
   - `model`
   - `repository`
   - `service`
   - `client`
   - `utils`
   - `frontend`
9. Express business meaning through class names such as `AuthController`, `ProjectRepoService`, `LuceneCodeSearchService`, or `PatchGenerateResult`.
10. Unless explicitly requested, do not introduce unrelated new business flows beyond the current project scope.
11. If project structure, tech choices, development boundaries, or startup instructions change, update both `README.md` and this file.
12. Keep database access in `repository` and business orchestration in `service`.
13. New backend tables should preferably provide a repeatable startup initialization path, such as Spring Boot SQL init scripts, instead of relying only on manual execution.

### Default Interpretation

Unless explicitly overridden by the user, this file remains in effect for future Codex development in this repository.
