# Calculators — Implementation Progress

Granular checklist tracked **phase by phase**, with each phase broken into **slices**. A slice is the smallest unit that is independently implementable, testable, reviewable, and committable. **One slice = one commit.** Mark each checkbox the moment it's done — don't batch.

Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md).

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

**Per-slice loop (binding — see CLAUDE.md):** implement → tests green → self code-review (medium; high for security/auth/money) → commit on `code` (Conventional Commits, reference slice ID in body) → push. PR `code` → `main` at natural checkpoints (typically end of phase).

---

## Completed phases (0–4)

All slices done. Full detail (design rationale, gotchas, exact commit hashes) in [PROGRESS_ARCHIVE.md](./PROGRESS_ARCHIVE.md).

| Phase | What was built | Last commit |
|-------|---------------|-------------|
| 0 — Repo bootstrap | Monorepo layout, `.gitignore`, `code` branch | `bdaae0d` |
| 1 — Backend scaffold | Spring Boot 4 skeleton, dev/prod profiles, `/api/health`, global exception handler, CORS, Spotless, JaCoCo, Testcontainers, GitHub Actions CI | `623caa0` |
| 2 — Frontend scaffold | Next.js 16 + React 19, Vitest + RTL, landing page, Prettier + strict TS, `lib/api.ts`, `/dev/health` probe, shadcn/ui theme, GitHub Actions CI | `f5012b4` |
| 3 — Tax backend | Domain enums, rule entities, Flyway V1–V3 migrations, AY 2024-25/2025-26 seed, DTOs, full 6-step calculation service (salary exemption → threshold → slab walk → rebate → floor → AIT), worked-example regression (§10.8 → net 56,820 BDT), `POST /calculate`, `GET /rules/{year}`, dev H2 seeding | `fix(backend): seed dev (H2)` |
| 4 — Tax frontend | Tax route + metadata, rules view (slab table + category thresholds), input form (19 fields, RHF + zod v4), client-side validation, `POST /calculate` integration, full breakdown render (§10.8 worked example), mobile-responsive polish | `2760266` |

---

## Phase 5 — Auth + history persistence

### 5.1 — Users table + entity
- [x] Migration: `users` (id, email unique, password_hash, created_at)
- [x] JPA entity `User` + repository
- [x] Test: persist + lookup by email via Testcontainers
- [x] Self code-review (high — schema)
- [x] Commit `feat(auth): users table + entity`; push

### 5.2 — Signup endpoint (BCrypt)
- [x] `POST /api/auth/signup` — validates email + password strength, BCrypt hash, returns user id (not token yet)
- [x] Test: happy path; duplicate-email rejected with 409
- [x] Self code-review (high — auth)
- [x] Commit `feat(auth): signup endpoint`; push

### 5.3 — Login endpoint + JWT issuance
- [x] `POST /api/auth/login` — validates credentials, issues JWT (HS256, configurable secret + expiry)
- [x] Test: happy path returns token; wrong password → 401
- [x] Self code-review (high — auth)
- [x] Commit `feat(auth): login + JWT issuance`; push

> **Known gap:** Issued tokens are not invalidated on password change or account deletion (no revocation
> mechanism). Any future account-mutation endpoint must revisit this — either shorten token TTL +
> add refresh tokens, or introduce a lightweight denylist (jti blocklist). UUID-based subject and
> `iss`/`aud` claims are deferred to Slice 5.4.

### 5.4 — Spring Security stateless filter chain
- [x] Configure stateless security: JWT validation filter, public allow-list (health, calculate, rules, signup, login)
- [x] `GET /api/account/me` — protected endpoint (returns authenticated user's email; also used as integration-test anchor for Slice 5.9)
- [x] Test: protected endpoint without token → 401; with valid token → 200; with expired/malformed token → 401; public health endpoint → 200
- [x] Self code-review (high — auth)
- [x] Commit `feat(auth): JWT filter + security config`; push

### 5.5 — Calculations history table
- [x] Migration: `calculations` (id, user_id FK, assessment_year, request_json, response_json, created_at)
- [x] JPA entity + repository (moved to `platform/history/` — history is a platform concern per PLAN.md §4)
- [x] Test: persist + fetch by user via Testcontainers; paginated query tested
- [x] Self code-review (medium)
- [x] Commit `feat(history): calculations table + entity`; push

### 5.5.1 — Add calculator_type discriminator (pre-5.6 schema fix)
- [x] Flyway V6: add `calculator_type VARCHAR(20) NOT NULL`; drop `assessment_year` (retained in request_json)
- [x] `CalculatorType` enum (TAX, ZAKAT) in platform/history
- [x] `Calculation` entity updated; `CalculationRepository` consolidates to two paginated methods
- [x] Tests updated: consolidated history + type-filtered query both covered
- [x] Commit `feat(history): calculator_type discriminator (Slice 5.5.1)`; push

### 5.6 — Persist calculation when logged-in
- [x] In tax calculate controller: if authenticated, save calculation row
- [x] Test: unauthenticated → not saved; authenticated → saved with correct user_id + non-blank JSON
- [x] Self code-review (medium) — fixed stale Javadoc, replaced "anonymousUser" literal with `instanceof AnonymousAuthenticationToken`, added JSON content assertions
- [x] Fix: Surefire was silently skipping all `*IT.java` classes — added explicit `<includes>` in pom.xml (SecurityFilterChainIT also now runs for the first time)
- [x] Commit `feat(history): save calculation for logged-in users`; push

### 5.7 — List calculation history
- [x] `GET /api/calculators/tax/history` — paginated, current user only
- [x] Test: returns own rows only; pagination respected; unauthenticated → 401
- [x] Self code-review (medium) — fixed deleted-user 404→401, degraded-entry on corrupt row, extracted `resolveAuthenticatedUser()` helper, removed dead `findByUserIdOrderByCreatedAtDesc`
- [x] Commit `feat(history): list endpoint`; push

### 5.8 — Frontend signup page
- [x] `app/account/signup/page.tsx` with form, validation, error handling
- [x] Test: form renders; mocked API success redirects to login; 409 duplicate-email error
- [x] Self code-review (medium) — fixed noValidate, aria-live on field errors, mode: 'onTouched', non-409 ApiError body.message surfaced, test payload assertion added
- [x] Commit `feat(frontend-auth): signup page`; push

### 5.9 — Frontend login + auth context
- [x] `app/account/login/page.tsx`; React context (user/token/isRestoring, localStorage persistence, TOKEN_KEY/EMAIL_KEY exported)
- [x] API client attaches token: `apiGetAuth`/`apiPostAuth` with `string | null` guard; rejects with ApiError(401) when null
- [x] Test: login flow stores user; logout clears it; isRestoring transitions false after effect
- [x] Self code-review (high — auth client) — added res.token guard, isRestoring, exported keys, TODO for expiry + shared error handler, token null-guard in api functions
- [x] Commit `feat(frontend-auth): login + auth context`; push

### 5.10 — "Save calculation" CTA when logged in
- [x] Tax page shows save indicator / success toast when authenticated
- [x] Test: logged-out → no save UI; logged-in → CTA visible
- [x] Self code-review (medium) — dropped broken /account/history link (Slice 5.11 not built yet), reset saved on field edit via watch subscription, removed dead setSaved in catch, disabled submit during isRestoring
- [x] Commit `feat(frontend-tax): save indicator for logged-in users`; push

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
- [ ] Set env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, **`SPRING_PROFILES_ACTIVE=prod`**
- [ ] Add a fail-fast `EnvironmentPostProcessor` (or `ApplicationContextInitializer`) that errors at startup if `spring.profiles.active` is missing or contains `dev` — closes the slice 1.2 footgun where a forgotten `SPRING_PROFILES_ACTIVE=prod` would silently fall back to dev profile + H2 in-memory DB in production
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
