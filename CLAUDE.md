# Calculators — Project Rules for Claude

This file is the entry point for any Claude session working on this repo. It captures **what this project is**, **how it's built**, and **how to work in it**. Architecture/design lives in [PLAN.md](./PLAN.md). Step-by-step implementation tracking lives in [PROGRESS.md](./PROGRESS.md). Always check both before starting work.

---

## What this is

A web (and later mobile) platform hosting **multiple financial/civic calculators**. The first is a **Bangladeshi individual income tax calculator**. The next planned is a **Zakat calculator**, with more to follow. Calculator-specific logic must be modular — never bake one calculator's assumptions into shared/platform code.

## Stack (locked)

| Layer        | Choice                                                |
| ------------ | ----------------------------------------------------- |
| Backend      | Spring Boot, **Java 21 (LTS)**, **Maven**             |
| Frontend     | **Next.js** (React, TypeScript)                       |
| Database     | **PostgreSQL**                                        |
| Auth         | JWT — accounts optional (anonymous use works)         |
| Deployment   | Free-tier cloud (Render / Railway / Fly.io)           |
| Mobile       | Deferred — backend must stay a clean REST/JSON API    |

Don't propose stack changes without explicitly raising the trade-off.

## Repo layout (monorepo)

```
Hishabi-Calculators/
  backend/      Spring Boot app
  frontend/     Next.js app
  CLAUDE.md     This file
  PLAN.md       Architecture + design decisions
  PROGRESS.md   Granular implementation checklist
```

## Working rules

1. **Always read `PLAN.md` and `PROGRESS.md` before starting work.** They are the source of truth for direction and status.
2. **Update `PROGRESS.md` as you go.** Mark steps complete the moment they're done — don't batch. If you add a new step or sub-step mid-task, append it there.
3. **Update `PLAN.md` when a design decision changes.** Don't let it drift from reality.
4. **Backend = stateless REST API.** No server-rendered HTML from Spring. The frontend is the only client we maintain; assume there will be others (mobile).
5. **Calculator code is modular.** Each calculator (tax, zakat, …) is its own package/module on both sides. Shared concerns (auth, users, history, UI shell) live in a platform layer.
6. **Tax rules are data, not code.** Bangladesh tax rules change yearly. Store slabs, thresholds, and rebates in config/DB rows keyed by assessment year — not in hardcoded `if` chains.
7. **No premature features.** Ship the smallest thing that works, then iterate. Don't add abstractions for hypothetical future calculators beyond the modular boundary already required.
8. **Confirm before destructive actions** — dropping DB tables, force-pushing, deleting branches, rewriting migrations that have been applied.
9. **Windows/PowerShell environment.** Use PowerShell syntax in shell examples (`$env:VAR`, not `export VAR=`). Maven and Node both work fine cross-platform.

## Authoring convention (current)

Until the user says otherwise:

- **User types** production source code (Java, TypeScript), test code, and configuration files (`application.yml`, `pom.xml` edits, `vitest.config.ts`, etc.). Claude proposes the code blocks with full file paths; user signals completion with **"done"**.
- **Claude drives** scaffolding generators (Spring Initializr, `create-next-app`), CI/YAML files, auto-formatter output (`./mvnw spotless:apply`), `npm install` commands, and doc files (`CLAUDE.md`, `PLAN.md`, `PROGRESS.md`).
- **Fixes follow original ownership.** If the user typed the code, the user also types fixes surfaced by tests or code-review — even trivial typos. If Claude wrote/generated the code, Claude applies fixes. No "Claude edits trivial typos in user code" carve-out.
- After user signals "done", Claude reads the typed files to confirm they match, runs tests, performs the slice's code-review, updates PROGRESS, commits, and pushes.

This convention can be revoked at any time with an explicit "I'm done typing code".

## Per-slice workflow (binding)

[PROGRESS.md](./PROGRESS.md) is organized into **phases** (coarse groupings) and **slices** (the smallest commit-unit). **One slice = one commit.** Every slice ends with this loop. No exceptions.

1. **Implement** the slice's items.
2. **Tests** — write/run tests for what was built. Red → green before moving on.
3. **Self code-review** — run the `/code-review` skill on the diff at `medium` effort. Escalate to `high` for security, auth, or money/calculation logic.
4. **Independent review (selective)** — for high-risk slices (auth flow, tax math, persistence migrations), spawn a fresh `code-reviewer` subagent on the diff. Cold context catches what self-review misses.
5. **Fix** findings; re-run tests.
6. **Update PROGRESS.md** checkboxes the moment each item is done.
7. **Commit on `code` branch** using Conventional Commits format (`feat(backend): ...`, `test(tax): ...`, `chore: ...`). Reference the slice ID (e.g., "Slice 3.6") in the commit body.
8. **Push to `code`** after each slice. PR `code` → `main` at natural checkpoints (typically end of phase, or whenever the user opens one). User reviews and merges.

**Branching:** all work lands on `code`. Never commit directly to `main`. Never force-push.

**Quality gates baked into the build:**

| Layer    | Tools                                                                          |
| -------- | ------------------------------------------------------------------------------ |
| Backend  | Spotless (Google Java Format), JaCoCo (coverage), JUnit 5 + AssertJ + Mockito + Testcontainers |
| Frontend | ESLint, Prettier, TypeScript `strict: true`, Vitest + React Testing Library    |
| CI       | GitHub Actions runs tests + lint on every PR to `main`                         |

**Never push red builds.** Run `./mvnw verify` (backend) and `npm run lint && npm test` (frontend) locally before pushing.

## Dev commands

_Filled in as the project is scaffolded — see [PROGRESS.md](./PROGRESS.md) for current status._

```powershell
# Backend (from backend/)
./mvnw spring-boot:run         # run dev server
./mvnw test                    # run tests
./mvnw package                 # build jar

# Frontend (from frontend/)
npm run dev                    # run dev server
npm run build                  # production build
npm test                       # run tests
```

## Where to look

- **What's the next step?** → [PROGRESS.md](./PROGRESS.md)
- **Why is it built this way?** → [PLAN.md](./PLAN.md)
- **How do I run things?** → this file, "Dev commands"
