# Calculators — Implementation Progress

Granular checklist tracked **phase by phase**, with each phase broken into **slices**. A slice is the smallest unit that is independently implementable, testable, reviewable, and committable. **One slice = one commit.** Mark each checkbox the moment it's done — don't batch.

Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md).

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

**Per-slice loop (binding — see CLAUDE.md):** implement → tests green → self code-review (medium; high for security/auth/money) → commit on `code` (Conventional Commits, reference slice ID in body) → push. PR `code` → `main` at natural checkpoints (typically end of phase).

---

## Completed phases (0–3)

All slices done. Full detail (design rationale, gotchas, exact commit hashes) in [PROGRESS_ARCHIVE.md](./PROGRESS_ARCHIVE.md).

| Phase | What was built | Last commit |
|-------|---------------|-------------|
| 0 — Repo bootstrap | Monorepo layout, `.gitignore`, `code` branch | `bdaae0d` |
| 1 — Backend scaffold | Spring Boot 4 skeleton, dev/prod profiles, `/api/health`, global exception handler, CORS, Spotless, JaCoCo, Testcontainers, GitHub Actions CI | `623caa0` |
| 2 — Frontend scaffold | Next.js 16 + React 19, Vitest + RTL, landing page, Prettier + strict TS, `lib/api.ts`, `/dev/health` probe, shadcn/ui theme, GitHub Actions CI | `f5012b4` |
| 3 — Tax backend | Domain enums, rule entities, Flyway V1–V3 migrations, AY 2024-25/2025-26 seed, DTOs, full 6-step calculation service (salary exemption → threshold → slab walk → rebate → floor → AIT), worked-example regression (§10.8 → net 56,820 BDT), `POST /calculate`, `GET /rules/{year}`, dev H2 seeding | `fix(backend): seed dev (H2)` |

---

## Phase 4 — Tax calculator: frontend

### 4.1 — Route + empty page skeleton
- [x] `app/calculators/tax/page.tsx` with header "Bangladeshi Income Tax — AY 2025-26" — static server component + page-level `metadata` title (Next 16 static-metadata export, confirmed unchanged via node_modules docs). Placeholder subtitle ("Rules and form coming next"). Route prerenders as `○ (Static)`
- [x] Linked from landing page — already in place from slice 2.4: landing's Income Tax card is `<Link href="/calculators/tax">`; the route now exists so the link no longer 404s
- [x] Test: page renders header; landing link navigates — `__tests__/TaxCalculatorPage.test.tsx` asserts the h1 via partial regex `/Bangladeshi Income Tax/i` + `2025-26` substring (robust to em-dash vs hyphen). "Landing link navigates" covered by the existing `Home.test` href assertion (real nav is e2e, out of scope). 10/10 frontend tests pass; `npm run check` green; build OK
- [x] Self code-review (medium) — no findings
- [x] Commit `feat(frontend-tax): tax page skeleton`; push — committed as `74e7338`, pushed to `origin/code`

### 4.2 — Fetch + render rules
- [x] On mount, call `GET /api/calculators/tax/rules/2025-26`; render slab table + category list (for confidence, not interactive yet) — new `features/tax/TaxRulesView.tsx` (`'use client'`) calls `apiGet<TaxRulesResponse>` from `lib/api.ts` (slice 2.6) via `useEffect`/`useCallback`; `features/tax/types.ts` mirrors the backend DTO. `app/calculators/tax/page.tsx` stays a server component (owns the page `metadata` export — Next 16 forbids `'use client'` from exporting metadata) and renders `<TaxRulesView />` as a client child. Slabs rendered as a 3-col table (#, width, rate); null-width top slab shows "Remaining". Category thresholds rendered as a flex list. AY hardcoded to `2025-26` for now; becomes a prop when a year picker lands
- [x] Loading + error states — discriminated-union `ViewState = loading | ok | error` mirroring the `/dev/health` probe pattern. `aria-live="polite"` + `aria-busy` on the section; error path shows backend message + Retry button that re-invokes the fetcher
- [x] Test: mocked API → renders slab rows — `__tests__/TaxRulesView.test.tsx` stubs global `fetch` (`vi.stubGlobal` + `unstubAllGlobals` in `afterEach`), asserts initial "Loading…" then 6 slab rows + the "Remaining" / "30%" / "GENERAL" sentinels after the promise resolves; second test exercises the error path with a 500 response. `__tests__/TaxCalculatorPage.test.tsx` updated to stub fetch so the now-fetching child doesn't blow up the page test. **12/12 frontend tests green** (2 new + 10 prior) via `npm run check` (lint, format, type-check, test). Caught a stray duplicate `src/features/tax/page.tsx` (outside the `app/` tree → not a route, dead code); removed before commit
- [x] Self code-review (medium) — 7-angle inline review on the ~85-line diff. No actionable findings. Two PLAUSIBLE non-blockers (hardcoded AY → becomes prop later; no `AbortController` on unmount → matches `/dev/health` precedent, dev-only warning at worst)
- [x] Commit `feat(frontend-tax): render rules from backend`; push

### 4.3 — Input form (no submit yet)
- [x] Form: income components, category dropdown, location dropdown, disabled-child count, investments (per type), AIT — `features/tax/TaxCalculatorForm.tsx` (`'use client'`). react-hook-form `useForm<TaxFormValues>` with sensible `defaultValues` (all amounts 0, category GENERAL, location DHAKA_CHITTAGONG_CITY_CORP). 19 number fields (10 income + 7 investments + disabledChildren + AIT) via `register(name, { valueAsNumber: true })`, rendered DRY from typed `NumberField[]` arrays. 2 dropdowns (category, location) via `Controller` + shadcn `Select` (base-ui: `value`/`onValueChange`). No submit logic yet — `onSubmit` is a stub wired in 4.5. Wired into the route page above `TaxRulesView`
- [x] Use UI library form components — scaffolded shadcn `input`, `label`, `select`, `card` (only `button` existed before); installed `react-hook-form@7`, `zod@4`, `@hookform/resolvers@5` (recorded in PLAN §2). Note: shadcn `add form` (the RHF wrapper) wouldn't scaffold in 4.10, so RHF is used directly with the primitives — `register` is leaner than the `FormField` render-prop for 19 simple fields anyway
- [x] Test: renders all fields; default values reasonable — `__tests__/TaxCalculatorForm.test.tsx` asserts representative number fields default to "0", both dropdowns + first/last income+investment fields render, and the Calculate button exists. base-ui `Select` renders fine in jsdom (never opened → no ResizeObserver polyfill needed). 13/13 frontend tests green. **Caught a misfile:** File-2's page edit was typed into a stray `features/tax/page.tsx` (not a route — `features/` isn't under `app/`) while the real `app/calculators/tax/page.tsx` went unedited; deleted the stray, applied the edit to the real route, and **strengthened `TaxCalculatorPage.test.tsx` to assert the Calculate button** so a future misfile is caught
- [x] Self code-review (medium) — no findings beyond the misfile (fixed). Build keeps `/calculators/tax` static (form is a client island)
- [x] Commit `feat(frontend-tax): input form structure`; push — committed as `ad4622b`, pushed to `origin/code`

### 4.4 — Client-side validation
- [x] Validate: required fields, non-negative numbers; surface inline errors — new `features/tax/schema.ts`: zod (v4) `taxFormSchema` — every amount `z.number().min(0)` (empty inputs arrive as `NaN` via `valueAsNumber`, which `z.number()` rejects → doubles as the "required" check), `disabledChildren` int `≥ 0`, category/location `z.enum(...)`. `TaxFormValues` is now `z.infer<typeof taxFormSchema>` (single source for form shape + validation). Form wired with `zodResolver` + `mode: 'onTouched'` (validate on blur, revalidate on change); inline error `<p>` under each number field via an `errorAt(errors, dottedPath)` walker; `aria-invalid` on errored inputs; `noValidate` on the `<form>` so RHF/zod own validation. zod 4 note: error param is `{ error: '...' }` (not `message`)
- [x] Test: invalid input shows error; valid input clears error — 2 new tests in `TaxCalculatorForm.test.tsx` (`fireEvent` + `findByText`/`waitFor`): negative Basic → "Basic cannot be negative" then clears when corrected; emptied AIT → "Advance Income Tax is required". 15/15 frontend tests green; build keeps `/calculators/tax` static
- [x] Self code-review (medium) — no findings; schema mirrors backend Bean Validation
- [x] Commit `feat(frontend-tax): client-side validation`; push — committed as `b83749d`, pushed to `origin/code`

### 4.5 — Submit + API call
- [x] On submit → `POST /api/calculators/tax/calculate`; handle loading + server validation errors — `TaxCalculatorForm` async `onSubmit` maps `TaxFormValues` → request by adding `assessmentYear: '2025-26'` (form fields already mirror the backend `TaxCalculationRequest` DTO 1:1), `POST`s via `apiPost<TaxCalculationResponse>`. Local state: `submitting` (button disables + "Calculating…"), `serverError` (`role="alert"`; `serverErrorMessage()` extracts backend `body.message` for 400/404, falls back to "Could not reach the server" for network/CORS), `result` (stored + a minimal "Net tax: … BDT" panel — full breakdown is 4.6). `TaxCalculationResponse`/`SlabTax` types added to `features/tax/types.ts`. Submit only fires on a zod-valid form
- [x] Test: mocked API → success path stores response; error path surfaces message — 2 new tests (`vi.stubGlobal('fetch')` + `unstubAllGlobals`): success → "56,820" rendered + POST to `/api/calculators/tax/calculate` with `method: POST`; 404 → `role="alert"` with the backend message. 17/17 frontend tests green; build keeps `/calculators/tax` static
- [x] Self code-review (high — user-visible API integration) — request shape cross-checked field-for-field against the slice-3.5 DTO (exact match); enum strings + AY match backend; money-as-JSON-number exact for tax-range inputs. No blocking findings. Tests mock `fetch`, so the real request-shape proof is the live round-trip (validation step)
- [x] Commit `feat(frontend-tax): submit + calculate API call`; push — committed as `8882b0a`, pushed to `origin/code`

### 4.6 — Render breakdown
- [x] Display: taxable income, slab-by-slab tax rows, gross tax, rebate (with which leg bound), after-rebate, minimum-tax bump (if applied), AIT credit, **net tax** — new `features/tax/TaxBreakdown.tsx`; card with 4 section dividers. Slab filter hides zero-amount slabs; `ordinal + 1` gives correct 1-indexed display (ordinal 0 = zero-rate band, confirmed by backend). Rebate binding leg inferred client-side via `rebateLegLabel` reduce (hardcoded 3%/15%/1M for AY 2025-26 — flagged in review as plausible future risk when second year lands). `TaxCalculatorForm` swaps the minimal result panel for `<TaxBreakdown result={result} />`
- [x] Test: structure test for the worked-example response (PLAN.md §10.8) — `WORKED_EXAMPLE` fixture in `TaxCalculatorForm.test.tsx` mirrors the exact §10.8 numbers; updated success mock to use full response (partial mock would crash `slabs.filter`); new test asserts taxable income, slab rows (2/3/4), gross tax, rebate label "3% of taxable income", "not binding" floor, two occurrences of "56,820 BDT". **18/18 tests green**
- [x] Self code-review (high — user-facing correctness) — 7-angle review. 3 PLAUSIBLE findings, none blocking: (1) hardcoded 0.03/0.15/1M in `rebateLegLabel` will mislabel binding leg for future AYs — fix by passing rules props when second year added; (2) `pct` diverges from `TaxRulesView.pct` (Math.round vs toLocaleString) — harmless for whole-% rates; (3) `fmt`/`bdt` duplicated across components. No correctness bugs for AY 2025-26
- [x] Commit `feat(frontend-tax): render calculation breakdown`; push

### 4.7 — Mobile-responsive polish
- [x] Layout works on mobile widths (form stacks, tables scroll) — `p-4 sm:p-8` on page, `overflow-x-auto` wrapping slab table, `min-w-0` on breakdown Row label span. Form grids already stacked correctly from slice 4.3 (`sm:grid-cols-2`). Also fixed a regression in this slice: missing leading space in `Row` className template literal was producing `text-smfont-semibold` (invalid), silently dropping bold weight from all summary rows.
- [x] Manual verify in browser at 375px / 768px / desktop — all three viewports confirmed via dev-server preview screenshots
- [x] Self code-review (medium) — 7-angle review, 1 confirmed bug (space regression, fixed), 1 non-blocking cleanup (use `cn()` for Row className — deferred). No other findings.
- [x] Commit `style(frontend-tax): responsive layout polish (Slice 4.7)`; push — committed as `2760266`, pushed to `origin/code`

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
