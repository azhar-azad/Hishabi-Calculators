# Calculators — Implementation Progress

Granular checklist tracked **phase by phase**, with each phase broken into **slices**. A slice is the smallest unit that is independently implementable, testable, reviewable, and committable. **One slice = one commit.** Mark each checkbox the moment it's done — don't batch.

Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md).

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

**Per-slice loop (binding — see CLAUDE.md):** implement → tests green → self code-review (medium; high for security/auth/money) → commit on `code` (Conventional Commits, reference slice ID in body) → push. PR `code` → `main` at natural checkpoints (typically end of phase).

---

## Phase 0 — Repo bootstrap

- [-] ~~Initialize git repo~~ — handled by user (creates remote, runs `git init`, creates `code` branch)
- [x] ~~Decide on package/product name~~ — Java package `dev.azhar.calculators`; product name **Hishabi** (PLAN.md §2)
- [x] ~~Add `.gitignore` covering Java, Node, IDE files (`.idea/`, `.vscode/`), env files (`.env*`), build outputs (`target/`, `node_modules/`, `.next/`)~~
- [x] ~~Create `backend/` and `frontend/` empty directories (with `.gitkeep` until scaffolded)~~
- [x] ~~First commit on `code` branch: `chore: bootstrap monorepo (Phase 0)` — includes .gitignore + empty dirs; push to `code`~~ — committed as `bdaae0d "project plan documented"` and pushed to `origin/code`

---

## Phase 1 — Backend scaffold (Spring Boot)

### 1.1 — Spring Boot project skeleton
- [ ] Generate via Spring Initializr under `backend/`: Java 21, Maven, base package `dev.azhar.calculators`; dependencies: Web, Validation, Spring Data JPA, PostgreSQL Driver, Spring Security, Lombok
- [ ] Verify `./mvnw spring-boot:run` boots cleanly
- [ ] Test: `ApplicationContextTest` (Spring context loads); `./mvnw test` green
- [ ] Self code-review (medium)
- [ ] Commit `chore(backend): generate Spring Boot skeleton`; push to `code`

### 1.2 — Spring profiles (dev + prod)
- [ ] `application.yml`: shared defaults + `application-dev.yml` (H2 or local Postgres, decide and record in PLAN.md §2) + `application-prod.yml` (env-driven `DB_URL` / `DB_USER` / `DB_PASSWORD`)
- [ ] Test: `@ActiveProfiles("dev")` boot smoke test; `@ActiveProfiles("prod")` boot test with env vars supplied
- [ ] Self code-review (medium)
- [ ] Commit `chore(backend): add dev/prod profiles`; push

### 1.3 — Health endpoint
- [ ] Implement `GET /api/health` returning `{ "status": "ok" }`
- [ ] Test: `HealthControllerTest` (200 OK + JSON shape)
- [ ] Self code-review (medium)
- [ ] Commit `feat(backend): add /api/health endpoint`; push

### 1.4 — Global exception handler
- [ ] `@RestControllerAdvice` returning a consistent error shape: `{ timestamp, status, code, message, path }`
- [ ] Wire validation (`MethodArgumentNotValidException`), generic 500, and a `NotFoundException` placeholder
- [ ] Test: deliberate-throw path returns expected JSON; validation error returns 400 with field errors
- [ ] Self code-review (medium)
- [ ] Commit `feat(backend): add global exception handler`; push

### 1.5 — CORS config
- [ ] CORS config allowing frontend dev origin (`http://localhost:3000`), reading allowed origins from `app.cors.allowed-origins` property
- [ ] Test: preflight `OPTIONS` returns proper `Access-Control-Allow-*` headers
- [ ] Self code-review (medium)
- [ ] Commit `feat(backend): add CORS config`; push

### 1.6 — Spotless (Google Java Format)
- [ ] Add Spotless Maven plugin, bind `check` to `verify`
- [ ] Run `./mvnw spotless:apply` to format existing code
- [ ] Verify `./mvnw spotless:check` clean
- [ ] Self code-review (medium)
- [ ] Commit `chore(backend): add Spotless (Google Java Format)`; push

### 1.7 — JaCoCo coverage reporting
- [ ] Add JaCoCo Maven plugin with `report` goal bound to `verify`
- [ ] Verify `target/site/jacoco/index.html` is generated after `./mvnw verify`
- [ ] Self code-review (medium)
- [ ] Commit `chore(backend): add JaCoCo coverage reporting`; push

### 1.8 — Testcontainers (Postgres) infrastructure
- [ ] Add `org.testcontainers:postgresql` dependency (test scope)
- [ ] Add a smoke test (`PostgresContainerSmokeTest`) that boots a Postgres container and runs `SELECT 1`
- [ ] Test: smoke test passes under `./mvnw verify`
- [ ] Self code-review (medium)
- [ ] Commit `test(backend): add Testcontainers Postgres support`; push

### 1.9 — CI: backend workflow
- [ ] `.github/workflows/ci.yml` with backend job: checkout, setup JDK 21 (Temurin), cache Maven, `./mvnw verify`, upload JaCoCo report artifact
- [ ] Triggers: PR to `main`, push to `code`
- [ ] Verify green on GitHub after push
- [ ] Self code-review (medium)
- [ ] Commit `ci: backend test + coverage workflow`; push

---

## Phase 2 — Frontend scaffold (Next.js)

### 2.1 — Next.js project skeleton
- [ ] Decide Tailwind yes/no (record in PLAN.md §2)
- [ ] Generate via `create-next-app` under `frontend/`: TypeScript, App Router, ESLint, Tailwind (per decision)
- [ ] Verify `npm run dev` serves the default page
- [ ] Self code-review (medium)
- [ ] Commit `chore(frontend): generate Next.js skeleton`; push

### 2.2 — Hishabi branding (metadata + favicon)
- [ ] Root layout metadata: `title: "Hishabi"`, description "Calculators for Bangladeshi finance & life"
- [ ] Favicon placeholder
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend): set Hishabi metadata`; push

### 2.3 — Test stack (Vitest + RTL)
- [ ] Install Vitest, `@testing-library/react`, `@testing-library/jest-dom`, jsdom env
- [ ] Wire `npm test` script + `vitest.config.ts`
- [ ] Add a trivial smoke test (`smoke.test.ts`) that asserts `1 + 1 === 2` to prove the runner works
- [ ] Self code-review (medium)
- [ ] Commit `chore(frontend): add Vitest + React Testing Library`; push

### 2.4 — Landing page placeholder
- [ ] Replace default `app/page.tsx` with simple landing: header "Hishabi", short tagline, list of future calculators (Income Tax, Zakat coming soon)
- [ ] Test: `Home.test.tsx` (renders "Hishabi" header and "Income Tax" link)
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend): landing page placeholder`; push

### 2.5 — Prettier + strict TypeScript
- [ ] Add Prettier with sensible defaults + `.prettierrc`
- [ ] `tsconfig.json` `strict: true`
- [ ] ESLint config: warnings → errors when `CI=true`
- [ ] Run formatter + lint clean
- [ ] Self code-review (medium)
- [ ] Commit `chore(frontend): Prettier + strict TS + tighter ESLint`; push

### 2.6 — API client helper
- [ ] `lib/api.ts` with base URL from `NEXT_PUBLIC_API_URL` (defaults to `http://localhost:8080`); typed `get`/`post` helpers
- [ ] Test: unit test mocking `fetch`
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend): add API client helper`; push

### 2.7 — End-to-end /api/health probe
- [ ] Dev-only page (e.g. `app/_dev/health/page.tsx`) calling backend `/api/health` and rendering result
- [ ] Manual verify: start both servers, page shows backend status
- [ ] Self code-review (medium)
- [ ] Commit `chore(frontend): wire /api/health probe page`; push

### 2.8 — UI component library decision *(decision — no commit)*
- [ ] Decide: shadcn/ui vs MUI vs hand-rolled (discuss with user)
- [ ] Record decision in PLAN.md §2

### 2.9 — UI library install + theme
- [ ] Install + bootstrap chosen library
- [ ] Replace one element on landing (e.g. button) with library component to prove it renders
- [ ] Test: existing landing test still passes
- [ ] Self code-review (medium)
- [ ] Commit `chore(frontend): install <lib> + basic theme`; push

### 2.10 — CI: frontend workflow
- [ ] Extend `.github/workflows/ci.yml` with frontend job: setup Node (LTS), `npm ci`, `npm run lint`, `npm test`, `npm run build`
- [ ] Verify green on GitHub
- [ ] Self code-review (medium)
- [ ] Commit `ci: frontend lint + test + build workflow`; push

---

## Phase 3 — Tax calculator: backend

_Rules derived from user's Excel — see PLAN.md §10. Pure-function service (no DB inside), data-driven rules._

### 3.1 — Tax package + domain enums
- [ ] Create package `dev.azhar.calculators.calculators.tax`
- [ ] Enums: `TaxpayerCategory` (GENERAL, WOMAN, SENIOR_65_PLUS, PHYSICALLY_MENTALLY_DISABLED, GAZETTED_FREEDOM_FIGHTER, THIRD_GENDER), `Location` (DHAKA_CHITTAGONG_CITY_CORP, OTHER_CITY_CORP, OTHER)
- [ ] Test: JSON serialization roundtrip via Jackson
- [ ] Self code-review (medium)
- [ ] Commit `feat(tax): add tax package + domain enums`; push

### 3.2 — Rule entities
- [ ] Entities per PLAN.md §6: `AssessmentYear`, `RuleSet`, `TaxSlab` (ordered, belongs to `RuleSet`), `CategoryThreshold`, `MinimumTaxFloor`
- [ ] JPA mappings + relationships
- [ ] Test: persist + fetch roundtrip via Testcontainers Postgres
- [ ] Self code-review (high — schema is foundational)
- [ ] Commit `feat(tax): add rule entities`; push

### 3.3 — Migration tool *(decision + wire-up)*
- [ ] Decide Flyway vs Liquibase (record in PLAN.md §2)
- [ ] Add dependency + config; V1 migration creates tax rule tables (no seed yet)
- [ ] Test: migration runs cleanly against Testcontainers
- [ ] Self code-review (medium)
- [ ] Commit `feat(backend): wire <tool> migrations + V1 tax schema`; push

### 3.4 — Seed AY 2025-26 + AY 2024-25 rules
- [ ] V2 migration: insert one `RuleSet` row with the AY 2025-26 slabs (PLAN.md §10.4), category thresholds (§10.3), minimum-tax floors (§10.6), exemption cap (§10.2), rebate caps (§10.5)
- [ ] V2 migration: insert `AssessmentYear` rows for `2024-25` and `2025-26` pointing to the same `RuleSet` (per PLAN.md §10.0)
- [ ] Test: query AY 2025-26 → slab structure matches §10.4; AY 2024-25 → same `RuleSet` reference
- [ ] Self code-review (high — tax data correctness)
- [ ] Commit `feat(tax): seed AY 2024-25 + 2025-26 rule set`; push

### 3.5 — DTOs (request + response)
- [ ] `TaxCalculationRequest`: income components, category, location, disabled-child count, investments (by category), AIT — with Bean Validation (`@NotNull`, `@PositiveOrZero`)
- [ ] `TaxCalculationResponse`: full breakdown matching PLAN.md §10.8
- [ ] Test: validation rejects negative values and missing required fields
- [ ] Self code-review (medium)
- [ ] Commit `feat(tax): add request/response DTOs`; push

### 3.6 — Calculation service: salary exemption
- [ ] `TaxCalculationService` skeleton — pure function `(RuleSet, TaxCalculationRequest) → intermediate`, no DB inside
- [ ] Step 1: `taxFreeSalary = min(total/3, 450k)`
- [ ] Test: salary exemption below and above the 450k cap
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — salary exemption`; push

### 3.7 — Calculation service: effective first-slab threshold
- [ ] Step 2: `effectiveThreshold = categoryThreshold + 50,000 × disabledChildren`
- [ ] Test: one case per taxpayer category (6 tests); disabled-child add (0, 1, 3 children)
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — effective threshold`; push

### 3.8 — Calculation service: slab walk
- [ ] Step 3: walk slabs in order, taxing `min(remaining, slabWidth) × rate`; produce slab-by-slab breakdown
- [ ] Test: matches expected per-slab tax for typical incomes (low → all in slab 1, mid → spans 1–3, high → spans all 7)
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — slab walk`; push

### 3.9 — Calculation service: investment rebate
- [ ] Step 4: per-item investment caps (PLAN.md §10.5), then `rebate = min(0.03 × taxable, 0.15 × eligible, 1,000,000)`; rebate = 0 if `taxable ≤ 0`
- [ ] Test: rebate cap binding on each of the three legs (3%, 15%, 1M); zero-taxable case
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — investment rebate`; push

### 3.10 — Calculation service: minimum tax floor
- [ ] Step 5: `withFloor = (taxable > 0) ? max(afterRebate, floorByLocation) : 0`
- [ ] Test: below-floor case per location (Dhaka 5k, other-CC 4k, other 3k); zero taxable income → no floor applied
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — minimum tax floor`; push

### 3.11 — Calculation service: AIT credit
- [ ] Step 6: `netTax = max(0, withFloor − AIT)`; modeled separately from rebate
- [ ] Test: AIT > tax → net = 0 (no refund modeled); AIT < tax → net = tax − AIT
- [ ] Self code-review (high — money math)
- [ ] Commit `feat(tax): calculation — AIT credit`; push

### 3.12 — Worked-example regression test
- [ ] End-to-end test exactly reproducing PLAN.md §10.8 (expected net tax = 56,820)
- [ ] Self code-review (medium)
- [ ] Commit `test(tax): worked-example regression`; push

### 3.13 — POST /api/calculators/tax/calculate
- [ ] Controller: `POST /api/calculators/tax/calculate` — accepts `TaxCalculationRequest`, returns `TaxCalculationResponse`; resolves active `RuleSet` by AY (default to latest)
- [ ] Test: MockMvc happy path; validation error returns 400 with global-error JSON shape
- [ ] Self code-review (high — public API surface)
- [ ] Commit `feat(tax): POST /api/calculators/tax/calculate`; push

### 3.14 — GET /api/calculators/tax/rules/{assessmentYear}
- [ ] Controller: returns the rule set for an AY (slabs, category thresholds, floors, caps)
- [ ] Test: 200 with full payload for `2025-26`; 404 for unknown year
- [ ] Self code-review (medium)
- [ ] Commit `feat(tax): GET /api/calculators/tax/rules/{year}`; push

---

## Phase 4 — Tax calculator: frontend

### 4.1 — Route + empty page skeleton
- [ ] `app/calculators/tax/page.tsx` with header "Bangladeshi Income Tax — AY 2025-26"
- [ ] Linked from landing page
- [ ] Test: page renders header; landing link navigates
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-tax): tax page skeleton`; push

### 4.2 — Fetch + render rules
- [ ] On mount, call `GET /api/calculators/tax/rules/2025-26`; render slab table + category list (for confidence, not interactive yet)
- [ ] Loading + error states
- [ ] Test: mocked API → renders slab rows
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-tax): render rules from backend`; push

### 4.3 — Input form (no submit yet)
- [ ] Form: income components, category dropdown, location dropdown, disabled-child count, investments (per type), AIT
- [ ] Use UI library form components
- [ ] Test: renders all fields; default values reasonable
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-tax): input form structure`; push

### 4.4 — Client-side validation
- [ ] Validate: required fields, non-negative numbers; surface inline errors
- [ ] Test: invalid input shows error; valid input clears error
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-tax): client-side validation`; push

### 4.5 — Submit + API call
- [ ] On submit → `POST /api/calculators/tax/calculate`; handle loading + server validation errors
- [ ] Test: mocked API → success path stores response; error path surfaces message
- [ ] Self code-review (high — user-visible API integration)
- [ ] Commit `feat(frontend-tax): submit + calculate API call`; push

### 4.6 — Render breakdown
- [ ] Display: taxable income, slab-by-slab tax rows, gross tax, rebate (with which leg bound), after-rebate, minimum-tax bump (if applied), AIT credit, **net tax**
- [ ] Test: snapshot/structure test for the worked-example response (PLAN.md §10.8)
- [ ] Self code-review (high — user-facing correctness)
- [ ] Commit `feat(frontend-tax): render calculation breakdown`; push

### 4.7 — Mobile-responsive polish
- [ ] Layout works on mobile widths (form stacks, tables scroll)
- [ ] Manual verify in browser at 375px / 768px / desktop
- [ ] Self code-review (medium)
- [ ] Commit `style(frontend-tax): responsive layout`; push

---

## Phase 5 — Auth + history persistence

### 5.1 — Users table + entity
- [ ] Migration: `users` (id, email unique, password_hash, created_at)
- [ ] JPA entity `User` + repository
- [ ] Test: persist + lookup by email via Testcontainers
- [ ] Self code-review (high — schema)
- [ ] Commit `feat(auth): users table + entity`; push

### 5.2 — Signup endpoint (BCrypt)
- [ ] `POST /api/auth/signup` — validates email + password strength, BCrypt hash, returns user id (not token yet)
- [ ] Test: happy path; duplicate-email rejected with 409
- [ ] Self code-review (high — auth)
- [ ] Commit `feat(auth): signup endpoint`; push

### 5.3 — Login endpoint + JWT issuance
- [ ] `POST /api/auth/login` — validates credentials, issues JWT (HS256, configurable secret + expiry)
- [ ] Test: happy path returns token; wrong password → 401
- [ ] Self code-review (high — auth)
- [ ] Commit `feat(auth): login + JWT issuance`; push

### 5.4 — Spring Security stateless filter chain
- [ ] Configure stateless security: JWT validation filter, public allow-list (health, calculate, rules, signup, login)
- [ ] Test: protected endpoint without token → 401; with valid token → 200; with expired token → 401
- [ ] Self code-review (high — auth)
- [ ] Commit `feat(auth): JWT filter + security config`; push

### 5.5 — Calculations history table
- [ ] Migration: `calculations` (id, user_id FK, assessment_year, request_json, response_json, created_at)
- [ ] JPA entity + repository
- [ ] Test: persist + fetch by user via Testcontainers
- [ ] Self code-review (medium)
- [ ] Commit `feat(history): calculations table + entity`; push

### 5.6 — Persist calculation when logged-in
- [ ] In tax calculate controller: if authenticated, save calculation row
- [ ] Test: unauthenticated → not saved; authenticated → saved with correct user_id
- [ ] Self code-review (medium)
- [ ] Commit `feat(history): save calculation for logged-in users`; push

### 5.7 — List calculation history
- [ ] `GET /api/calculators/tax/history` — paginated, current user only
- [ ] Test: returns own rows only; pagination respected
- [ ] Self code-review (medium)
- [ ] Commit `feat(history): list endpoint`; push

### 5.8 — Frontend signup page
- [ ] `app/account/signup/page.tsx` with form, validation, error handling
- [ ] Test: form renders; mocked API success redirects to login
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-auth): signup page`; push

### 5.9 — Frontend login + auth context
- [ ] `app/account/login/page.tsx`; React context for current user + token (httpOnly cookie preferred)
- [ ] API client attaches token to requests
- [ ] Test: login flow stores user; logout clears it
- [ ] Self code-review (high — auth client)
- [ ] Commit `feat(frontend-auth): login + auth context`; push

### 5.10 — "Save calculation" CTA when logged in
- [ ] Tax page shows save indicator / success toast when authenticated
- [ ] Test: logged-out → no save UI; logged-in → CTA visible
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-tax): save indicator for logged-in users`; push

### 5.11 — History page
- [ ] `app/account/history/page.tsx` — list of past calculations, click to re-open with inputs prefilled
- [ ] Test: renders list from mocked API; click navigates to tax page with state
- [ ] Self code-review (medium)
- [ ] Commit `feat(frontend-auth): history page`; push

---

## Phase 6 — Deployment

### 6.1 — Choose hosting provider *(decision — no commit)*
- [ ] Compare Render / Railway / Fly.io free tiers at this moment; pick one
- [ ] Record decision in PLAN.md §2

### 6.2 — Backend Dockerfile
- [ ] Multi-stage Dockerfile (Maven build → slim JRE runtime), exposes 8080
- [ ] Verify `docker build` + `docker run` works locally; `/api/health` reachable
- [ ] Self code-review (medium)
- [ ] Commit `chore(deploy): backend Dockerfile`; push

### 6.3 — Provision managed Postgres
- [ ] Create Postgres instance on chosen provider
- [ ] Capture connection details into a secrets store (provider's env-var UI)
- [ ] (no commit — infra setup)

### 6.4 — Backend env config + deploy
- [ ] Set env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`
- [ ] Deploy backend image; verify `/api/health` and Flyway/Liquibase migrations ran
- [ ] (no commit — infra deploy; capture deploy notes in PLAN.md if useful)

### 6.5 — Frontend deploy
- [ ] Deploy Next.js (Vercel or same provider); set `NEXT_PUBLIC_API_URL` to production backend
- [ ] Verify landing page loads
- [ ] (no commit — infra deploy)

### 6.6 — Production smoke test
- [ ] Hit `POST /api/calculators/tax/calculate` from prod frontend with PLAN.md §10.8 inputs; confirm net tax = 56,820
- [ ] Sign up + log in + save calculation + view history
- [ ] (no commit — verification)

### 6.7 — Uptime check
- [ ] Configure provider's built-in uptime check or a free external one (e.g., UptimeRobot) on `/api/health`
- [ ] (no commit — infra config)

---

## Future phases

- [ ] **Zakat calculator** — repeat Phases 3–4 under `calculators/zakat/` (will be sliced when started)
- [ ] **Mobile app** — pick stack (React Native vs Flutter vs native) based on user comfort at that point
- [ ] **Analytics / error tracking** — opt-in (see PLAN.md §11)
- [ ] **i18n** — Bengali + English (likely needed for BD audience; confirm with user)
- [ ] **Wealth surcharge** for net wealth > 4 crore BDT (see PLAN.md §11)

---

_When adding new slices mid-phase, append them in place (renumbering is fine — git history is the source of truth, not slice IDs). When a slice is no longer relevant, mark `[-]` with a one-line note rather than deleting — keeps the history of decisions intact._
