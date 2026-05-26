# Calculators — Implementation Progress

Granular checklist tracked phase by phase. Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md). **Mark each item complete the moment it's done — don't batch.**

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

---

## Phase 0 — Repo bootstrap

- [-] ~~Initialize git repo~~ — handled by user (creates remote, runs `git init`, creates `code` branch)
- [x] ~~Decide on package/product name~~ — Java package `dev.azhar.calculators`; product name **Hishabi** (PLAN.md §2)
- [x] ~~Add `.gitignore` covering Java, Node, IDE files (`.idea/`, `.vscode/`), env files (`.env*`), build outputs (`target/`, `node_modules/`, `.next/`)~~
- [x] ~~Create `backend/` and `frontend/` empty directories (with `.gitkeep` until scaffolded)~~
- [ ] First commit on `code` branch: `chore: bootstrap monorepo (Phase 0)` — includes .gitignore + empty dirs; push to `code`

## Phase 1 — Backend scaffold (Spring Boot)

- [ ] Generate Spring Boot project (Java 21, Maven) under `backend/` — dependencies: Web, Validation, Spring Data JPA, PostgreSQL Driver, Spring Security, Lombok
- [ ] Set base Java package: `dev.azhar.calculators`
- [ ] Configure `application.yml` profiles: `dev` (H2 or local Postgres), `prod` (env-driven)
- [ ] Add a health-check endpoint (`GET /api/health`)
- [ ] Set up global exception handler returning consistent JSON error shape
- [ ] Add CORS config allowing the frontend dev origin
- [ ] Verify `./mvnw spring-boot:run` boots cleanly and `/api/health` responds
- [ ] Add quality plugins: **Spotless** (Google Java Format), **JaCoCo** (coverage report on `verify`)
- [ ] Add test dependencies: **JUnit 5** (default), **AssertJ**, **Mockito**, **Testcontainers** (Postgres module)
- [ ] Write baseline tests: `ApplicationContextTest` (context loads), `HealthControllerTest` (200 OK + JSON shape)
- [ ] Run `./mvnw verify` — all green, coverage report generated
- [ ] Add `.github/workflows/ci.yml` — backend job: setup JDK 21, run `./mvnw verify`, upload JaCoCo report. Triggers on PR to `main` and push to `code`.
- [ ] Per-phase workflow: self code-review on diff (medium effort), then commit `feat(backend): scaffold Spring Boot app (Phase 1)` on `code`, push

## Phase 2 — Frontend scaffold (Next.js)

- [ ] Generate Next.js app under `frontend/` (TypeScript, App Router, ESLint, Tailwind — confirm Tailwind choice)
- [ ] Set app metadata: title `Hishabi`, description "Calculators for Bangladeshi finance & life", favicon placeholder
- [ ] Create base layout + landing page placeholder listing future calculators (header reads "Hishabi")
- [ ] Add API client helper (`lib/api.ts`) pointing to backend URL via env var
- [ ] Wire a test call to `/api/health` from a dev page to confirm end-to-end connectivity
- [ ] Decide and apply a UI component library (shadcn/ui, MUI, or hand-rolled) — track decision in PLAN.md §2
- [ ] Add quality tooling: **Prettier**, TypeScript `strict: true` in `tsconfig.json`, ESLint config tightened (warnings → errors in CI)
- [ ] Add test stack: **Vitest** + **React Testing Library** + **@testing-library/jest-dom**
- [ ] Write baseline tests: `Home.test.tsx` (renders Hishabi header), API client unit test (mocks fetch)
- [ ] Run `npm run lint && npm test` — all green
- [ ] Extend `.github/workflows/ci.yml` with frontend job: setup Node, `npm ci`, `npm run lint`, `npm test`, `npm run build`
- [ ] Per-phase workflow: self code-review on diff (medium effort), then commit `feat(frontend): scaffold Next.js app (Phase 2)` on `code`, push

## Phase 3 — Tax calculator: backend domain

_Rules derived from user's Excel — see PLAN.md §10._

- [ ] Add `dev.azhar.calculators.calculators.tax` package
- [ ] Define entities: `AssessmentYear` (e.g., `"2025-26"`) → references a `RuleSet`; `RuleSet` owns `TaxSlabSet`, `CategoryThreshold`s, `MinimumTaxFloor`s, exemption cap, rebate caps. Plus enums `TaxpayerCategory` and `Location` (see PLAN.md §6, §10.0)
- [ ] Seed AY 2024-25 and AY 2025-26 pointing to the SAME `RuleSet` row (per §10.0, NBR didn't amend between the two years)
- [ ] DTOs: `TaxCalculationRequest` (income components + category + location + disabled-child count + investments + AIT), `TaxCalculationResponse` (full breakdown matching PLAN.md §10.8)
- [ ] Add Flyway (or Liquibase) for schema migrations — choose one and record in PLAN.md §2
- [ ] Seed AY 2025-26 rules via migration: 7-slab structure, 6 category thresholds, 3 minimum-tax floors
- [ ] Implement `TaxCalculationService` — pure function `(rules, input) -> result`, no DB access inside
  - [ ] Step 1: salary exemption (`min(total/3, 450k)`)
  - [ ] Step 2: effective first-slab threshold (category + disabled-child add)
  - [ ] Step 3: slab walk producing slab-by-slab tax breakdown
  - [ ] Step 4: investment rebate (`min(3% taxable, 15% eligible, 1M)`)
  - [ ] Step 5: minimum-tax floor by location
  - [ ] Step 6: AIT credit (separate from rebate, modeled cleanly)
- [ ] Unit tests:
  - [ ] Reproduce the user's worked example exactly (PLAN.md §10.8) — net tax = 56,820
  - [ ] One case per taxpayer category (6 tests) — verify first-slab threshold shifts correctly
  - [ ] Disabled-child threshold add (1 child, 3 children)
  - [ ] Below-minimum-tax case (taxable income > 0, computed tax < floor) — assert floor applies per location
  - [ ] Zero taxable income (no minimum tax)
  - [ ] Rebate cap binding on each of the three legs (3% leg, 15% leg, 1M leg)
- [ ] Expose `POST /api/calculators/tax/calculate` accepting `TaxCalculationRequest`, returning `TaxCalculationResponse`
- [ ] Expose `GET /api/calculators/tax/rules/{assessmentYear}` so frontend can render slabs / category lists without hardcoding

## Phase 4 — Tax calculator: frontend

- [ ] Build tax calculator page under `app/calculators/tax/`
- [ ] Form: income inputs by category (salary, business, etc. — depends on rules)
- [ ] Submit → call backend → render breakdown (slab-by-slab tax, rebate, net payable)
- [ ] Client-side validation matching backend DTO constraints
- [ ] Mobile-responsive layout

## Phase 5 — Auth + history persistence

- [ ] Add `users` table + JPA entity
- [ ] Signup / login endpoints, BCrypt hashing, JWT issuance
- [ ] Spring Security filter chain, stateless, JWT validation
- [ ] `calculations` table to persist history per user
- [ ] Save calculation when a logged-in user submits
- [ ] Frontend: login/signup pages, auth context, "Save this calculation" CTA when logged in
- [ ] History page listing past calculations

## Phase 6 — Deployment

- [ ] Choose provider (Render / Railway / Fly) — record decision in PLAN.md §2
- [ ] Dockerfile for backend
- [ ] Provision managed Postgres
- [ ] Set env vars (DB URL, JWT secret, allowed origins)
- [ ] Deploy backend, verify `/api/health`
- [ ] Deploy frontend, point at production backend URL
- [ ] Smoke test full tax-calculation flow in production
- [ ] Add a basic uptime check (provider's built-in or external)

## Future phases

- [ ] **Zakat calculator** — repeat Phases 3–4 under `calculators/zakat/`
- [ ] **Mobile app** — pick stack (React Native vs Flutter vs native) based on user comfort at that point
- [ ] **Analytics / error tracking** — opt-in (see PLAN.md §9)
- [ ] **i18n** — Bengali + English (likely needed for BD audience; confirm with user)

---

_When adding new steps mid-phase, append them in place. When a step is no longer relevant, mark `[-]` with a one-line note rather than deleting — keeps the history of decisions intact._
