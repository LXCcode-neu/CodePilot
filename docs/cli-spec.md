# CLI Spec

English

---

## Purpose

This document defines the development specification for the future CodePilot CLI.

The CLI is a command-line entrypoint for CodePilot platform capabilities. It is intended to let users create tasks, observe execution, inspect patches, confirm fixes, and submit pull requests without requiring direct frontend usage.

This file is a product and engineering specification. It is not the runtime spec for the internal repair agent, and it does not replace repository-wide development rules in `AGENTS.md`.

## Goals

The CLI should:

1. reuse existing CodePilot backend capabilities instead of re-implementing agent logic locally
2. provide a scriptable interface for developers, CI jobs, and operations workflows
3. keep tokens and sensitive integration secrets server-side whenever possible
4. expose task execution, verification, review, and PR actions in a terminal-friendly form
5. support both human-readable output and machine-readable JSON output
6. present human-facing terminal prompts and summaries in Simplified Chinese by default

## Non-Goals

The CLI should not:

- duplicate the backend agent state machine locally
- store plaintext LLM keys, GitHub OAuth tokens, or Sentry secrets on user machines
- access the database directly
- bypass backend verification, AI review, or approval gates
- introduce a separate business flow that diverges from frontend behavior

## Positioning

The CLI is a thin client over the existing CodePilot backend.

Recommended flow:

```text
CLI command
  -> CodePilot backend API
  -> existing task / patch / PR / notification flow
```

This means the CLI is responsible for:

- local configuration
- auth token storage for CodePilot platform login
- argument parsing
- request/response formatting
- polling or streaming task status

The backend remains responsible for:

- agent execution
- repository sync
- patch generation
- deterministic verification
- AI patch review
- patch confirmation rules
- PR submission
- GitHub and Sentry integrations

## Architecture

### Development Constraints

The CLI must follow the repository rules in `AGENTS.md`:

- Java 17
- Maven as the only build tool
- keep Java packages under `com.codepliot`
- avoid introducing new frameworks unless explicitly approved

For the first implementation, prefer:

- Java 17 standard library for process startup and file IO
- Java `HttpClient` for HTTP communication
- Jackson if already available through the existing Maven build

Do not assume a new CLI framework is approved. If a parser library such as Picocli is introduced later, that should be a separate explicit decision.

### Packaging

Recommended package root:

```text
src/main/java/com/codepliot/cli
```

Suggested internal layout:

```text
com.codepliot.cli
|- command
|- client
|- config
|- model
|- output
|- support
```

If the CLI grows substantially, it may later move into a dedicated Maven module. The first version should prefer a simpler in-repo layout unless packaging needs clearly justify a module split.

## Operating Model

### Backend-Backed CLI

The default mode is remote-backed operation:

- the CLI talks to a running CodePilot backend
- the backend owns all execution and state transitions
- CLI commands should map closely to existing backend APIs

### Optional Local Convenience

The CLI may support local convenience behavior such as:

- opening a browser for login
- streaming formatted task events
- exporting patch text to stdout or a file

These are UX helpers only. They must not become alternative execution paths for repair logic.

## Local Configuration

The CLI should store only the minimum local configuration needed to call the backend.

Recommended config file:

```text
~/.codepilot/config.json
```

Required fields:

- `serverUrl`
- `accessToken`
- `defaultProjectId` optional
- `outputMode` optional, for example `text` or `json`

The CLI must not store:

- plaintext project LLM API keys
- GitHub OAuth access tokens owned by the backend
- Sentry auth tokens
- webhook signing secrets

If local auth data is invalid or expired, the CLI should fail clearly and guide the user to log in again.

## Command Surface

The CLI command surface should stay small and task-oriented.

### Required Command Groups

`config`

- `codepilot config set-server <url>`
- `codepilot config show`

`auth`

- `codepilot auth login --token <token>`
- `codepilot auth logout`
- `codepilot auth status`

`project`

- `codepilot project list`
- `codepilot project show <projectId>`

`task`

- `codepilot task create --project <id> --title <title> --desc <description>`
- `codepilot task list`
- `codepilot task show <taskId>`
- `codepilot task watch <taskId>`
- `codepilot task run <taskId>`
- `codepilot task cancel <taskId>`

`patch`

- `codepilot patch show <taskId>`
- `codepilot patch confirm <taskId>`

`pr`

- `codepilot pr submit <taskId>`

### Optional Command Groups

These are useful but not required for the first version:

- `github`
- `sentry`
- `events`
- `doctor`

### Command Behavior Rules

- every mutating command should print a short success summary in text mode
- every command should support a machine-readable `--json` mode
- long-running commands such as `task watch` should return non-zero exit codes on terminal failure states
- output should avoid frontend-only wording and stay concise

## Output Modes

The CLI should support two output modes.

### Text Mode

Human-readable, stable enough for daily terminal usage.

Use cases:

- local developer workflows
- operator inspection
- manual approvals

Text mode must use Simplified Chinese for command help, status names, error summaries, and success messages.

### JSON Mode

Machine-readable output for scripts and CI.

Use cases:

- GitHub Actions
- Jenkins pipelines
- shell automation

Rules:

- JSON output should contain structured fields, not mixed prose
- stderr may still be used for unexpected runtime errors
- exit codes must remain meaningful even when JSON is used

## Exit Codes

Recommended baseline:

- `0` success
- `1` CLI runtime error
- `2` usage or validation error
- `3` backend business rejection, such as invalid task state
- `4` auth failure
- `5` network or backend unavailable

The implementation may refine this list later, but exit codes must stay documented and stable once published.

## Authentication

The CLI authenticates against CodePilot backend APIs, not against GitHub or Sentry directly.

Initial assumption:

- the user obtains a CodePilot platform token through existing login flows
- `codepilot auth login --token ...` stores that token locally

Future optional enhancement:

- browser-based login flow opened from the CLI

Even with a future browser login flow, the CLI should still persist only the CodePilot platform token and never direct third-party secrets.

## API Dependency Rules

The CLI should only rely on backend APIs that are also valid platform contracts for frontend or automation use.

Do not build the CLI around unstable controller internals.

Before implementation, the following backend capabilities must exist as documented APIs:

- project listing
- task creation
- task detail lookup
- task event stream or polling endpoint
- task rerun
- task cancel
- patch view
- patch confirm
- PR submit

If any capability is currently frontend-only or missing, define or stabilize the backend API first before adding the CLI command.

## Task Watching

The CLI should support task progress observation.

Preferred first implementation:

- polling task detail and step data on an interval

Optional later implementation:

- SSE-backed watch mode using the existing task events endpoint

Why polling first:

- easier to implement
- easier to debug
- more portable in CI and terminal environments

Why SSE later:

- lower latency
- cleaner real-time experience

## Patch And PR Rules

The CLI must obey the same backend gates as the frontend.

That means:

- a patch cannot be confirmed before deterministic verification passes
- failed AI Patch Review must still block PR confirmation
- PR submission must go through backend-owned repository sync and patch application logic
- the CLI must not directly push branches to GitHub

The CLI is a transport and UX layer, not an escape hatch around platform policy.

## Security Rules

The CLI must preserve current backend-side trust boundaries.

Rules:

- never print plaintext backend secrets in standard output
- redact bearer tokens in logs and error messages
- avoid writing sensitive data to shell history where possible
- prefer reading tokens from stdin or environment variables for automation-friendly login
- do not persist temporary task outputs that may contain secrets unless the user explicitly requests export

If the CLI adds a debug mode later, debug output must remain redacted by default.

## Error Handling

The CLI should distinguish clearly between:

- local usage errors
- network errors
- authentication failures
- backend business rule failures
- unexpected internal errors

For backend business failures, preserve the backend message when safe to show.

Examples:

- `task is already running`
- `patch must be confirmed before submitting a pull request`
- `patch is outdated and can no longer be applied to the latest code`

## Logging And Observability

The CLI should remain quiet by default.

Recommended flags:

- `--json`
- `--verbose`
- `--quiet`

Rules:

- default mode should show only high-signal output
- verbose mode may include request timing and retry details
- verbose mode must still redact secrets

## Compatibility

The CLI should initially target:

- Windows PowerShell
- macOS Terminal
- Linux shell environments

Behavior must not depend on bash-specific features for core operation.

Local file paths should be handled in a platform-aware way.

## CI Usage

The CLI should be usable inside CI without interactive prompts.

Examples of intended CI usage:

```text
codepilot auth login --token $CODEPILOT_TOKEN
codepilot task create --project 1 --title "Fix prod issue" --desc "..."
codepilot task watch 123 --json
codepilot patch confirm 123
codepilot pr submit 123
```

Requirements:

- every required input must have a flag-based non-interactive form
- JSON output must be stable enough for parsing
- failures must return non-zero exit codes

## Versioning

The CLI should report its own version and, when possible, the backend version it is talking to.

Recommended command:

- `codepilot version`

This helps diagnose compatibility issues between CLI releases and backend deployments.

## Suggested MVP Scope

The first implementation should focus on:

1. config storage
2. token-based login
3. project list
4. task create
5. task show
6. task watch by polling
7. patch show
8. patch confirm
9. PR submit
10. JSON output mode

This is enough to cover the main terminal workflow without overbuilding.

## Explicitly Out Of Scope For MVP

- direct GitHub API usage from the CLI
- direct Sentry API usage from the CLI
- local execution of agent tools
- local patch generation
- interactive TUI
- plugin system
- custom scripting language

## Future Extensions

Possible future extensions include:

- SSE-backed watch mode
- browser-based login
- issue import commands
- Sentry project mapping commands
- richer patch diff rendering
- a dedicated `cli/` Maven module
- shell completion

## Acceptance Criteria

The CLI spec should be considered satisfied when:

1. a user can configure a backend server and log in
2. a user can create and inspect tasks from the terminal
3. a user can wait on task progress without opening the frontend
4. a user can confirm a verified patch and submit a PR through backend APIs
5. CI can call the CLI non-interactively with stable JSON output and meaningful exit codes
6. the CLI does not bypass backend verification, review, or security boundaries
