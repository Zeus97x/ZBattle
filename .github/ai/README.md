# Permanent AI development team
User-approved operating procedure, 2026-10-09. Applies to existing and future GitHub Android apps, websites and software repositories. Initialize this workspace for new repositories.

## Roles and approval
ChatGPT leads planning, UI/UX, roadmap, coordination, reviews, integration and releases. Recommend Claude for architecture, complex Kotlin/debugging, databases, refactoring, performance, independent review and independent features. Recommend Codex for implementation, multi-file edits, tests, Gradle, Actions and automation. Roles are recommendations, not restrictions.
Recommend before transferring work; user approves assignments. Do not automatically switch systems or start a proposed Claude task. If the user says they switched to Claude/Codex, stop that assigned implementation until requested again. User-approved work may proceed without repeated confirmation.

## Begin and recover
Identify repository; read AGENTS.md, this README, PROJECT_STATUS, TASK_BOARD, ROADMAP, root DEVELOPMENT_LOG and relevant legacy handoffs. Check current branch/head, open PRs and unfinished work before coding. Reconcile stale notes with source; unknown facts remain unknown. GitHub is the shared source of truth; never rely on chat history. Preserve existing features and unfinished work.
Use separate ai/<system>/<task> branches, record task IDs in commits/PRs, avoid overlapping files, and integrate through PRs. Never merge without user approval. Recheck branch heads before updates. No forced overwrite of another AI.

## Tasks and phases
Use templates/TASK_TEMPLATE.md for every task; unique IDs, no duplicates. Statuses: BACKLOG, READY, IN PROGRESS, BLOCKED, REVIEW, COMPLETED. Folder mapping: backlog, ready, in-progress (including BLOCKED), review, completed. Move files as status changes; Git history preserves moves. TASK_BOARD links the canonical files.
Plan major features in phases; bugs stay separate. Recommend AI/model/reasoning before major work. Implement two phases per batch unless instructed otherwise; pause at required check-ins. Brainstorming authorizes planning only. Future ideas stay backlog. Completed work is preserved.
Every meaningful change updates logs and task status alongside code. Root DEVELOPMENT_LOG preserves historical entries; this workspace log links it rather than copying history.

## Handoffs and review
Use templates/HANDOFF_TEMPLATE.md. Include context, repository, branch, relevant commits, phase, current implementation, exact objective, files, approach, constraints/unchanged behavior, dependencies/issues, tests, build requirements and acceptance. Handoffs must work without previous chats. Recipient creates a dedicated branch, implements, tests, documents, opens a PR and submits templates/COMPLETION_TEMPLATE.md. Handoffs are prepared proposals until user approves the recipient.
All directions among the three systems use handoffs/<recipient>/. Independent work may run concurrently after approval when files do not overlap. Recommend independent review for complicated/risky changes, not every minor edit. Review correctness, compatibility, performance, security, maintainability, preservation, regressions and meaningful tests.

## Usage and workload
See AI_USAGE_STRATEGY.md. Do not claim exact subscription usage or guess model prices/limits. Use user-reported levels; if unavailable assume no constraint for ordinary work and ask only when needed.
Consider complexity, effort, context, workload, independent division, file overlap, available usage, reviewer benefit and safest integration. Do not assign everything to ChatGPT by default.

## Builds, credentials and releases
Preserve application IDs and permanent signing identities; use existing GitHub Secrets. Never commit keys/passwords/tokens or include values in handoffs; document secret names only. Maintain Actions, verify results, investigate failures. Authorized production Android releases contain APKs. Avoid unnecessary builds during UI work; start release builds only when requested. Preserve project-specific build instructions and update compatibility.

## Shortcut commands
| Command | Action |
|---|---|
| Claude handoff | Prepare self-contained current-task handoff |
| Claude priority | Activate Claude Priority Mode and prepare proposals |
| Low usage | Reduce responses and prepare suitable transfers |
| Usage reset | Review external commits/PRs, reconcile tasks, resume normal coordination |
| What can Claude do? / What can Claude work on? | Review priority, complexity, dependencies, effort, parallel safety and handoff links |
| AI status | Summarize assigned and proposed tasks separately |
| Prepare parallel work | Identify independent file scopes; approval required to assign |
| Review Claude's work / Review Codex's work | Inspect commits/PRs and report findings |
| Update handoffs | Refresh active handoffs against current source |
| Project recovery | Reconstruct state from docs/source/history |
| Next phases | Review roadmap and recommend next two phases and AI |
