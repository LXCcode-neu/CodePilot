# AGENTS

中文 | English

---

## 中文说明

### 文件定位

本文件是 **CodePilot 仓库开发规范**，用于约束后续 Codex 或其他 AI 编码代理在本仓库中的开发行为。

它的目标是统一：

- 技术栈使用范围
- 目录与模块边界
- 可实现与不可实现内容
- 文档同步要求

### 这份文件不是做什么的

本文件 **不是** CodePilot 平台内部业务 Agent 的运行时规范，不描述：

- Issue 修复 Agent 的执行步骤
- 任务编排策略
- LLM 提示词设计
- patch 生成流程
- 平台内 Agent 的输入输出协议

这些内容应放在独立文档中，例如：

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
8. 模块边界应尽量清晰：
   - `auth`：认证与授权
   - `user`：用户领域
   - `project`：仓库/项目管理
   - `task`：Agent 任务管理
   - `git`：Git 仓库同步能力
   - `index`：基于 Tree-sitter 多语言解析架构的代码扫描与检索基础设施，未支持语言使用文本兜底索引
   - `agent`：任务编排与 Agent 协调
   - `llm`：模型调用抽象
   - `trace`：执行轨迹记录
   - `sse`：实时推送能力
   - `common`：共享基础类
   - `frontend`：基于 React + Vite 的 Web 控制台
9. 在用户没有明确要求前，不要实现登录、仓库管理、任务执行、Git clone、LLM 调用等具体业务逻辑。
10. 如果项目结构、技术选型、开发边界或启动方式发生变化，要同步更新 `README.md` 与本文件。
11. 数据库操作放 Repository 层，业务逻辑放 Service 层。

### 默认理解

除非用户明确覆盖，本文件对后续 Codex 开发持续生效。

---

## English

### Purpose

This file is the **repository development specification** for CodePilot. It constrains how Codex or other AI coding agents should work inside this repository.

It is intended to standardize:

- approved technology choices
- package and module boundaries
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
8. Keep module boundaries clear:
   - `auth`: authentication and authorization
   - `user`: user domain
   - `project`: repository/project management
   - `task`: agent task management
   - `git`: Git synchronization
   - `index`: Tree-sitter based multi-language code scan and retrieval infrastructure, with plain-text fallback for unsupported languages
   - `agent`: orchestration and agent coordination
   - `lock`: Redis-backed runtime locks such as task run guards
   - `llm`: model integration abstractions
   - `patch`: generated patch record storage and retrieval
   - `trace`: execution trace recording
   - `sse`: real-time push
   - `common`: shared foundational classes
   - `frontend`: React + Vite web console
9. Unless explicitly requested, do not implement concrete business logic such as login flow, repository management, task execution, Git clone, or LLM calls.
10. If project structure, tech choices, development boundaries, or startup instructions change, update both `README.md` and this file.
11. Database tables introduced by new backend modules should preferably provide a repeatable startup initialization path, for example Spring Boot SQL init scripts, instead of relying only on manual execution.

### Default Interpretation

Unless explicitly overridden by the user, this file remains in effect for future Codex development in this repository.
