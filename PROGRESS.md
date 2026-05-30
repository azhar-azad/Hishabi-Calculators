# Calculators — Implementation Progress

Granular checklist tracked **phase by phase**, with each phase broken into **slices**. A slice is the smallest unit that is independently implementable, testable, reviewable, and committable. **One slice = one commit.** Mark each checkbox the moment it's done — don't batch.

Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md).

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

**Per-slice loop (binding — see CLAUDE.md):** implement → tests green → self code-review (medium; high for security/auth/money) → commit on `code` (Conventional Commits, reference slice ID in body) → push. PR `code` → `main` at natural checkpoints (typically end of phase).

---

## Phase 0 — Repo bootstrap

- [-] Initialize git repo — handled by user (creates remote, runs `git init`, creates `code` branch)
- [x] Decide on package/product name — Java package `dev.azhar.hishabi` (originally `dev.azhar.calculators`; renamed in pre-Phase-3 refactor to avoid the `calculators.calculators` redundancy once `calculators/` became a subdomain); product name **Hishabi** (PLAN.md §2)
- [x] Add `.gitignore` covering Java, Node, IDE files (`.idea/`, `.vscode/`), env files (`.env*`), build outputs (`target/`, `node_modules/`, `.next/`)
- [x] Create `backend/` and `frontend/` empty directories (with `.gitkeep` until scaffolded)
- [x] First commit on `code` branch: `chore: bootstrap monorepo (Phase 0)` — includes .gitignore + empty dirs; push to `code` — committed as `bdaae0d "project plan documented"` and pushed to `origin/code`

---

## Phase 1 — Backend scaffold (Spring Boot)

### 1.1 — Spring Boot project skeleton
- [x] Generate via Spring Initializr under `backend/`: Java 21, Maven, base package `dev.azhar.hishabi`; dependencies: Web, Validation, Spring Data JPA, PostgreSQL Driver, Spring Security, Lombok — Initializr returned **Spring Boot 4.0.6** (latest GA); starter names follow Boot 4 convention (`spring-boot-starter-webmvc`, split-out `*-test` starters)
- [x] Verify boots cleanly — full context load proven by passing `@SpringBootTest` below; Tomcat port-bind verification deferred to slice 1.3 (`/api/health`)
- [x] Test: `CalculatorsApplicationTests` (Spring context loads); `./mvnw test` green — H2 added as test-scope dep so JPA context can initialize without a configured DataSource (real DB config arrives in slice 1.2)
- [x] Self code-review (medium) — inline three-angle review, no findings of consequence (diff is ~25KB of unchanged Initializr boilerplate around a 5-line H2 dep addition)
- [x] Commit `chore(backend): generate Spring Boot skeleton`; push to `code` — committed as `d8bfbf0`, pushed to `origin/code`

### 1.2 — Spring profiles (dev + prod)
- [x] `application.yml`: shared defaults + `application-dev.yml` (H2 or local Postgres, decide and record in PLAN.md §2) + `application-prod.yml` (env-driven `DB_URL` / `DB_USER` / `DB_PASSWORD`) — dev DB = H2 in-memory (PostgreSQL mode), recorded in PLAN.md §2. H2 dep promoted from `test` to `runtime` scope so dev runtime sees it. `spring.profiles.active: dev` set as default in `application.yml`
- [x] Test: `@ActiveProfiles("dev")` boot smoke test; `@ActiveProfiles("prod")` boot test with env vars supplied — `DevProfileBootTest` (asserts H2 datasource URL) + `ProdProfileBootTest` (boots with prod profile + property overrides to H2; full Postgres prod-shape coverage deferred to slice 1.8 Testcontainers)
- [x] Self code-review (medium) — three-angle inline review; one PLAUSIBLE finding (default-active dev profile is a deployment footgun if env var missing) deferred to slice 6.4 — see below
- [x] Commit `chore(backend): add dev/prod profiles`; push — committed as `07c5ebe`, pushed to `origin/code`

### 1.3 — Health endpoint
- [x] Implement `GET /api/health` returning `{ "status": "ok" }` — `HealthController` + `HealthResponse` record under `platform.health` package per PLAN.md §4
- [x] Test: `HealthControllerTest` (200 OK + JSON shape) — `@WebMvcTest` slice test with `@AutoConfigureMockMvc(addFilters = false)` to bypass Spring Security default auth (real "permit /api/health" config lands in slice 1.5)
- [x] Self code-review (medium) — three-angle inline review; no actionable findings
- [x] Commit `feat(backend): add /api/health endpoint`; push — committed as `f6429d7`, pushed to `origin/code`

### 1.4 — Global exception handler
- [x] `@RestControllerAdvice` returning a consistent error shape: `{ timestamp, status, code, message, path }` — `ApiError` record with optional `fieldErrors` (hidden via `@JsonInclude(NON_NULL)` when null) under `platform.error`
- [x] Wire validation (`MethodArgumentNotValidException`), generic 500, and a `NotFoundException` placeholder — three `@ExceptionHandler` methods. `handleGenerics` catches `RuntimeException` (narrowed from `Exception`) so Spring's framework exceptions (`NoResourceFoundException`, etc.) fall through to Spring's defaults and get correct 4xx codes
- [x] Test: deliberate-throw path returns expected JSON; validation error returns 400 with field errors — `GlobalExceptionHandlerTest` with inner `TestThrowController` (explicitly `@Import`-ed) covers NotFound→404, generic→500 (without leaking internals), validation→400 with field errors, plus a regression test that unmapped paths produce Spring's default 404
- [x] Self code-review (medium) — three-angle inline review surfaced the broad-catch issue (fixed inline)
- [x] Commit `feat(backend): add global exception handler`; push — committed as `4801792`, pushed to `origin/code`

### 1.5 — CORS config
- [x] CORS config allowing frontend dev origin (`http://localhost:3000`), reading allowed origins from `app.cors.allowed-origins` property — `CorsProperties` record (with `@DefaultValue` empty-list fallback) bound from `app.cors.allowed-origins`; `dev` profile sets `http://localhost:3000`; prod CORS deferred to slice 6.4 (env-var driven)
- [x] Test: preflight `OPTIONS` returns proper `Access-Control-Allow-*` headers — `CorsConfigTest` covers allowed origin (200 + headers) and disallowed origin (403). Also necessitated a minimal `SecurityConfig` (CSRF disabled, STATELESS sessions, `anyRequest().permitAll()` — slice 5.4 will tighten with JWT + real allowlist)
- [x] Self code-review (medium) — three-angle inline review; no actionable findings
- [x] Commit `feat(backend): add CORS config`; push — committed as `7c8824c`, pushed to `origin/code`

### 1.6 — Spotless (Google Java Format)
- [x] Add Spotless Maven plugin, bind `check` to `verify` — `com.diffplug.spotless:spotless-maven-plugin:2.46.1` with googleJavaFormat + removeUnusedImports + importOrder + trimTrailingWhitespace + endWithNewline
- [x] Run `./mvnw spotless:apply` to format existing code — 13 files reformatted to Google Java Format (2-space indent + Google import order); 1 file was already clean
- [x] Verify `./mvnw spotless:check` clean — `./mvnw verify` green: spotless:check passes + all 10 tests green
- [x] Self code-review (medium) — inline review; cosmetic-only diff to 13 files (no semantic changes); test suite all green confirms no behavior regression
- [x] Commit `chore(backend): add Spotless (Google Java Format)`; push — committed as `6534df3`, pushed to `origin/code`

### 1.7 — JaCoCo coverage reporting
- [x] Add JaCoCo Maven plugin with `report` goal bound to `verify` — `org.jacoco:jacoco-maven-plugin:0.8.13`, two executions: `prepare-agent` (default phase, injects the JVM agent into Surefire's `argLine`) + `report` (bound to `verify`)
- [x] Verify `target/site/jacoco/index.html` is generated after `./mvnw verify` — confirmed; report includes per-package HTML drilldown + `jacoco.csv` + `jacoco.xml` (for future CI/SonarCloud integration). No coverage thresholds enforced yet; can add as a follow-up slice when we have meaningful tax-calculation code to gate
- [x] Self code-review (medium) — inline review; one-plugin addition, no logic changes, all tests green
- [x] Commit `chore(backend): add JaCoCo coverage reporting`; push — committed as `908d25c`, pushed to `origin/code`

### 1.8 — Testcontainers (Postgres) infrastructure
- [x] Add `org.testcontainers:postgresql` dependency (test scope) — added `org.testcontainers:junit-jupiter` + `org.testcontainers:postgresql`, both `test` scope, both at `1.20.4` (explicit version — Spring Boot 4 BOM doesn't manage Testcontainers, unlike Boot 3)
- [x] Add a smoke test (`PostgresContainerSmokeTest`) that boots a Postgres container and runs `SELECT 1` — uses `postgres:16-alpine` image, static `@Container` field, JDBC `SELECT 1` via DriverManager
- [x] Test: smoke test passes under `./mvnw verify` — 11 tests total now green (10 previous + this one). Smoke test takes ~5-11s depending on container warm-start
- [x] Self code-review (medium) — three-angle inline review. One heads-up logged: CI workflow (slice 1.9) needs a Docker-enabled runner (default `ubuntu-latest` has Docker, so OK by default)
- [x] Commit `test(backend): add Testcontainers Postgres support`; push — committed as `146139a`, pushed to `origin/code`

### 1.9 — CI: backend workflow
- [x] `.github/workflows/ci.yml` with backend job: checkout, setup JDK 21 (Temurin), cache Maven, `./mvnw verify`, upload JaCoCo report artifact
- [x] Triggers: PR to `main`, push to `code`
- [x] Verify green on GitHub after push — first run failed (`backend/mvnw` stored as non-executable from Windows checkout); fixed via `git update-index --chmod=+x` in commit `623caa0`. Re-run (run id 26470825402) green
- [x] Self code-review (medium) — inline three-angle review; the mvnw chmod issue surfaced by CI itself (real-world feedback loop > local review)
- [x] Commit `ci: backend test + coverage workflow`; push — committed as `35f8f52` (workflow) + `623caa0` (mvnw chmod fix), pushed to `origin/code`

---

## Phase 2 — Frontend scaffold (Next.js)

### 2.1 — Next.js project skeleton
- [x] Decide Tailwind yes/no (record in PLAN.md §2) — **Yes**, recorded in PLAN.md §2 (2026-05-27)
- [x] Generate via `create-next-app` under `frontend/`: TypeScript, App Router, ESLint, Tailwind (per decision) — scaffold pulled **Next.js 16.2.6 + React 19 + Tailwind v4 + ESLint 9** (Next 16 has breaking changes vs older training data — see `frontend/AGENTS.md` and memory `project-nextjs-16-caveat`). Used `--src-dir` to match PLAN.md §5 layout; `--turbopack` (Next 16 default); `--use-npm`
- [x] Verify `npm run dev` serves the default page — boots in 3.2s; `GET http://localhost:3000` returns 200 + 16.8KB HTML with title "Create Next App"
- [x] Self code-review (medium) — no actionable findings; 2 npm-audit warnings are false-positives on transitive `postcss<8.5.10` inside Next's own `node_modules` ("fix" downgrades Next to 9.3.3 — unacceptable; build-time PostCSS XSS doesn't apply to standard Next builds; will clear when Next bumps its pinned postcss)
- [x] Commit `chore(frontend): generate Next.js skeleton`; push — committed as `578f1dc`, pushed to `origin/code`

### 2.2 — Hishabi branding (metadata + favicon)
- [x] Root layout metadata: `title: "Hishabi"`, description "Calculators for Bangladeshi finance & life" — typed by user in `frontend/src/app/layout.tsx` lines 15-18
- [x] Favicon placeholder — keeping scaffold's default `frontend/src/app/favicon.ico` (Next's logo) as the placeholder; replace when real Hishabi branding lands
- [x] Self code-review (medium) — two-string-literal change; `npm run lint` clean; `npm run build` green (TypeScript OK, `/` prerendered as static)
- [x] Commit `feat(frontend): set Hishabi metadata`; push — committed as `e9b6573`, pushed to `origin/code`

### 2.3 — Test stack (Vitest + RTL)
- [x] Install Vitest, `@testing-library/react`, `@testing-library/jest-dom`, jsdom env — installed: `vitest@4.1.7`, `@vitejs/plugin-react@6.0.2`, `jsdom@29.1.1`, `@testing-library/react@16.3.2` (React 19 compatible), `@testing-library/dom@10.4.1`, `@testing-library/jest-dom@6.9.1`, `vite-tsconfig-paths@6.1.1`
- [x] Wire `npm test` script + `vitest.config.ts` — typed by user in `frontend/package.json` (`"test": "vitest run"` — single-pass per CLAUDE.md pre-push rule) and `frontend/vitest.config.mts` (`.mts` per Next 16 docs, not `.ts`, since `package.json` has no `"type": "module"`). Config: `plugins: [tsconfigPaths(), react()]`, `environment: 'jsdom'`. Caught a typo on first try (`from "eslint/config"` instead of `"vitest/config"`) — user re-typed
- [x] Add a trivial smoke test (`smoke.test.ts`) that asserts `1 + 1 === 2` to prove the runner works — typed by user at `frontend/__tests__/smoke.test.ts` (Next docs' "common `__tests__` convention"). `npm test` → 1/1 passed in 3.33s
- [x] Self code-review (medium) — three-angle inline review: correct (test passes; lint clean; build green), secure (deps are mainstream Vitest/RTL ecosystem; no new audit findings), maintainable (4-space indent will normalize under Prettier in slice 2.5). Follow-up: drop `vite-tsconfig-paths` — Vite 7 has native `resolve.tsconfigPaths: true` support; the plugin prints a deprecation hint on every test run. Small cleanup for a later slice
- [x] Commit `chore(frontend): add Vitest + React Testing Library`; push — committed as `eea8056`, pushed to `origin/code`

### 2.4 — Landing page placeholder
- [x] Replace default `app/page.tsx` with simple landing: header "Hishabi", short tagline, list of future calculators (Income Tax, Zakat coming soon) — typed by user in `frontend/src/app/page.tsx`. Income Tax card is a `<Link href="/calculators/tax">` (route arrives in slice 4.1; until then it 404s — intentional). Zakat shown as opacity-60 disabled card with "Coming soon"
- [x] Test: `Home.test.tsx` (renders "Hishabi" header and "Income Tax" link) — typed by user at `frontend/__tests__/Home.test.tsx`; expanded to 4 tests covering h1, tagline, Income Tax link href, and Zakat coming-soon text. **First run failed** with "Found multiple elements" — RTL needs explicit cleanup between tests in Vitest. Patched with `frontend/vitest.setup.ts` (calls `cleanup()` in `afterEach`) + added `setupFiles` to `vitest.config.mts`. Slice 2.3 oversight that this slice fixes
- [x] Self code-review (medium) — three-angle inline; all 5 tests pass (1 smoke + 4 Home), lint clean, build green (/ still prerendered static). Code-review surfaced two typos in typed page.tsx (`fond-medium` → `font-medium`, `Bangladesh` → `Bangladeshi`) — fixed by user
- [x] Commit `feat(frontend): landing page placeholder`; push — committed as `45aeb69`, pushed to `origin/code`

### 2.5 — Prettier + strict TypeScript
- [x] Add Prettier with sensible defaults + `.prettierrc` — installed `prettier@3.8.3`, `eslint-config-prettier@10.1.8`, `prettier-plugin-tailwindcss@0.8.0`. Typed by user: `frontend/.prettierrc` (minimal — only `singleQuote: true` + tailwind plugin; rest inherit Prettier 3 defaults: 2-space, semi, trailing-commas-all, 80-char) and `frontend/.prettierignore` (only `package-lock.json`; `.gitignore` already covers `node_modules`/`.next` and Prettier reads it natively)
- [x] `tsconfig.json` `strict: true` — already on from `create-next-app` scaffold (line 7); no edit needed. Recorded here for traceability
- [x] ESLint config: warnings → errors when `CI=true` — implemented as **always strict** via `"lint": "eslint --max-warnings=0"` (tighter than spec — simpler, encourages zero-warning baseline everywhere; loosen later if friction). `eslint.config.mjs` now imports `eslint-config-prettier/flat` and places it AFTER `nextVitals` + `nextTs` in the flat-config array so it overrides any formatting rules they ship
- [x] Run formatter + lint clean — `npm run format` rewrote 9 files (single quotes, 2-space, sorted Tailwind classes via `prettier-plugin-tailwindcss`; e.g. page.tsx Link className collapsed from 2 lines to 1 and re-ordered). New scripts: `format`, `format:check`, plus convenience `check` = `lint && format:check && test` (npm scripts use cmd.exe on Windows — `&&` works). `npm run check` green; `npm run build` still prerenders `/` as static
- [x] Self code-review (medium) — three-angle inline: prettier ordering correct (last in array → wins formatting overrides), `--max-warnings=0` typo caught + fixed (`eslint max-warnings=0` → `eslint --max-warnings=0`), no behavioral diffs (5/5 tests still pass). Follow-up: CLAUDE.md "Quality gates" section still references `npm run lint && npm test` — should be `npm run check`. Doc-only cleanup for a later slice
- [x] Commit `chore(frontend): Prettier + strict TS + tighter ESLint`; push — committed as `cecfb88`, pushed to `origin/code`

### 2.6 — API client helper
- [x] `lib/api.ts` with base URL from `NEXT_PUBLIC_API_URL` (defaults to `http://localhost:8080`); typed `get`/`post` helpers — typed by user at `frontend/src/lib/api.ts`. Exports `apiGet<T>`, `apiPost<T, B>`, and `ApiError` (extends Error, carries `status` + parsed `body`). Private `request()` reads response as text first, then `JSON.parse` with text fallback — robust for both JSON and plain-text error responses. Throws `ApiError` on non-2xx. Two typos caught and fixed by user: `https://localhost:8080` → `http://localhost:8080` (dev backend has no TLS) and filename `api.text.ts` → `api.test.ts` (was being silently skipped by Vitest's `*.test.*` glob)
- [x] Test: unit test mocking `fetch` — `frontend/__tests__/api.test.ts` covers 4 scenarios: GET success with parsed JSON, GET 404 with JSON error body, POST sends correct headers+body and returns parsed JSON, POST 500 with plain-text body. Uses `vi.stubGlobal('fetch', ...)` + `vi.unstubAllGlobals()` in `afterEach` — clean auto-restore
- [x] Self code-review (medium) — three-angle inline; no actionable findings. `npm run check` 9/9 tests pass (1 smoke + 4 Home + 4 api); `npm run build` still prerenders `/` as static
- [x] Commit `feat(frontend): add API client helper`; push — committed as `78e79f6`, pushed to `origin/code`
- [x] **Post-commit fix:** validation step 2 (TypeScript catches misuse) exposed that `npm run build` only type-checks files reachable from the Next page graph — so `__tests__/` is silently skipped. Two type errors in the typed apiPost-error test (`error.status` / `error.body` on `unknown`) were hiding. **Fix:** added `"type-check": "tsc --noEmit"` to `package.json` scripts and wired it into the `check` chain (`lint && format:check && type-check && test`); rewrote the apiPost-error test using `rejects.toMatchObject({ name: 'ApiError', status, body })` for consistency with the apiGet-error test and to avoid the `unknown`-narrowing problem; removed now-unused `ApiError` import. All 4 gate stages green. Closes a slice-2.5 design gap (the `check` gate wasn't actually verifying TS correctness everywhere) — committed separately as a `fix(frontend)` follow-up

### 2.7 — End-to-end /api/health probe
- [x] Dev-only page (e.g. `app/_dev/health/page.tsx`) calling backend `/api/health` and rendering result — **path corrected** to `frontend/src/app/dev/health/page.tsx` (URL `/dev/health`); the original `_dev` would have been a Next App Router "private folder" (opts out of routing entirely → page unreachable). Client component with discriminated-union `ProbeState` (loading | ok | error), uses `apiGet<HealthResponse>` from slice 2.6, distinguishes `ApiError` (HTTP error from a reachable backend) vs other errors (network/CORS/fetch failure). Re-probe button + `aria-live="polite"` region. Bumped against a Next 16/React 19 lint rule `react-hooks/set-state-in-effect` — disabled inline with comment explaining the rule's static analysis can't see that our setState calls happen *after* an async resolve (no render cascade). Considered server-component alternative but rejected: server-to-server doesn't exercise CORS, which is half the point of this probe
- [x] Manual verify: start both servers, page shows backend status — validation steps documented in commit body / chat
- [x] Self code-review (medium) — three-angle inline; `npm run check` green (lint clean with documented disable, format clean, type-check clean, 9/9 tests pass), `npm run build` green (`/dev/health` listed as `○ (Static)` — loading HTML shell prerenders, fetch runs client-side after hydration). Follow-up: no prod-safety guard yet — if this page ships to prod, anyone can visit it. Add `NODE_ENV !== 'production'` check or remove the page before prod deploy
- [x] Commit `chore(frontend): wire /api/health probe page`; push — committed as `43e9a2a`, pushed to `origin/code`

### 2.8 — UI component library decision *(decision — no commit)*
- [x] Decide: shadcn/ui vs MUI vs hand-rolled (discuss with user) — **shadcn/ui** chosen. Reasons: Tailwind-native (we locked Tailwind in 2.1; MUI would fight it); components live in our repo so they double as a React patterns reference while learning Next; pairs with `react-hook-form` for the tax form coming in slice 4.x; Radix primitives give keyboard nav + screen reader support for free
- [x] Record decision in PLAN.md §2 — added row `Frontend UI library | shadcn/ui (Radix + Tailwind) | 2026-05-27`

### 2.9 — UI library install + theme
- [x] Install + bootstrap chosen library — ran `npx shadcn@latest init --defaults --yes` (next template + `base-nova` preset + neutral base color + CSS variables). Generated `frontend/components.json`, `src/components/ui/button.tsx`, `src/lib/utils.ts` (the `cn()` helper combining `clsx` + `tailwind-merge`); patched `src/app/globals.css` with theme CSS variables (background, foreground, primary, destructive, sidebar, chart palette). New runtime deps: `@base-ui/react@^1.5` (the Radix-team successor that shadcn 4.x is built on), `class-variance-authority`, `clsx`, `lucide-react`, `tailwind-merge`, `tw-animate-css`. `shadcn` CLI is also in runtime deps (slightly odd, but it's a CLI not imported into client bundles)
- [x] Replace one element on landing (e.g. button) with library component to prove it renders — landing had no existing button, so **added** rather than replaced: a dev-only `<Link>` to `/dev/health` styled via `buttonVariants({ variant: 'link', size: 'sm' })` (canonical shadcn pattern for "Link styled as button"; cleaner than base-ui's `render` prop). Wrapped in `process.env.NODE_ENV === 'development'` so Next dead-code-eliminates it in prod. Also gives `/dev/health` a real discoverable entry point in dev (partially addresses slice 2.7 follow-up)
- [x] Test: existing landing test still passes — Vitest runs with `NODE_ENV=test`, so the dev-only link doesn't render in tests. All 4 Home assertions unchanged + green; full 9/9 gate
- [x] Self code-review (medium) — three-angle inline; `npm run check` 4/4 green; `npm run build` prerenders `/`, `/_not-found`, `/dev/health` all as `○ (Static)`. Tracked the base-ui render-prop vs Radix asChild distinction (shadcn 4.x switched primitive libraries)
- [x] Commit `chore(frontend): install <lib> + basic theme`; push — committed as `9fde85a`, pushed to `origin/code`

### 2.10 — CI: frontend workflow
- [x] Extend `.github/workflows/ci.yml` with frontend job: setup Node (LTS), `npm ci`, `npm run lint`, `npm test`, `npm run build` — added `frontend` job parallel to `backend`. Node 22 LTS, `npm ci` (lockfile-strict), `working-directory: frontend` at job level, `NEXT_TELEMETRY_DISABLED=1`, cache keyed on `frontend/package-lock.json`. **Used `npm run check` instead of separate `lint`/`test` steps** — it's our composite gate (lint + format:check + type-check + test) from the slice-2.6 fix, a strict superset of the original spec; cleaner failure attribution. Separate `npm run build` step preserved
- [x] Verify green on GitHub — run id 26520864170 on commit `f5012b4`: both jobs success (Backend Maven verify + Frontend check + build)
- [x] Self code-review (medium) — three-angle inline; jobs run in parallel (no inter-dependence); cache invalidates cleanly on dep changes; no secrets needed for frontend job
- [x] Commit `ci: frontend lint + test + build workflow`; push — committed as `f5012b4`, pushed to `origin/code`; CI run 26520864170 green

---

## Phase 3 — Tax calculator: backend

_Rules derived from user's Excel — see PLAN.md §10. Pure-function service (no DB inside), data-driven rules._

### 3.1 — Tax package + domain enums
- [x] Create package `dev.azhar.hishabi.calculators.tax` — created `tax/model/` sub-package per PLAN.md §4 (enums are domain primitives alongside future entities + DTOs)
- [x] Enums: `TaxpayerCategory` (GENERAL, WOMAN, SENIOR_65_PLUS, PHYSICALLY_MENTALLY_DISABLED, GAZETTED_FREEDOM_FIGHTER, THIRD_GENDER), `Location` (DHAKA_CHITTAGONG_CITY_CORP, OTHER_CITY_CORP, OTHER) — typed by user in `TaxpayerCategory.java` + `Location.java`. Both with Javadoc grounding to PLAN.md §10.3 / §10.6. Caught a typo on first try (`GAZETTED_FREDOM_FIGHTER` missing the second E — Jackson's error message surfaced it, since the test serialized all enum values and the typo printed in the "accepted values" list)
- [x] Test: JSON serialization roundtrip via Jackson — `TaxEnumsJsonTest.java` covers (1) explicit uppercase serialization format, (2) full roundtrip across all enum values for both enums, (3) unknown value fails to deserialise with `InvalidFormatException`. **Discovered Spring Boot 4 ships Jackson 3 at `tools.jackson.*`** (not `com.fasterxml.jackson.*`) — first proposed test used Jackson 2 imports; fix used `tools.jackson.databind.ObjectMapper`, `new JsonMapper()`, `tools.jackson.databind.exc.InvalidFormatException`. Saved as memory `project-jackson-3-caveat` since this will recur in every future JSON-handling slice (DTOs, controllers, custom serializers)
- [x] Self code-review (medium) — three-angle inline; 15/15 unit tests green (10 prior + 4 new + 1 Application boot test that turned up after the d1e99f0 rename — wait that's 15 including the duplicate. Verified: `./mvnw '-Dtest=!PostgresContainerSmokeTest' test` runs 15/15 clean; Spotless clean
- [x] Commit `feat(tax): add tax package + domain enums`; push — committed as `e6e64e3` (preceded by `d1e99f0` follow-up refactor renaming `CalculatorsApplication` → `Application`), pushed to `origin/code`

### 3.2 — Rule entities
- [x] Entities per PLAN.md §6: `AssessmentYear`, `RuleSet`, `TaxSlab` (ordered, belongs to `RuleSet`), `CategoryThreshold`, `MinimumTaxFloor` — typed by user. `RuleSet` is the aggregate root holding scalar config (exemption cap+divisor, disabled-child bonus, rebate fractions+cap) plus three `@OneToMany` child lists (cascade=ALL, orphanRemoval). `TaxSlab` stores only the 6 *paying* slabs (rows 2-7 of §10.4); the 0% threshold band is per-taxpayer and computed at calc time, not stored. `width` is nullable (null = the open-ended "(rest)" top slab). Money = `numeric(15,2)`, rates = `numeric(5,4)`. Enum columns use `@Enumerated(STRING)`
- [x] JPA mappings + relationships — IDENTITY ids; `@OrderBy("ordinal ASC")` on slabs; unique constraints on `(rule_set_id, ordinal)`, `(rule_set_id, category)`, `(rule_set_id, location)`, and global unique on `AssessmentYear.label`. Lombok `@Getter/@Setter/@NoArgsConstructor` (no `@Data` — avoids dodgy entity equals/hashCode). **Hibernate 7 enum-DDL surprise**: `@Enumerated(STRING)` made Hibernate emit MySQL-style `enum(...)` column DDL that Postgres rejects (`type "enum" does not exist`) → table silently not created → INSERT failed with `relation does not exist`. `@Column(length=...)` and the `hibernate.type.preferred_enum_jdbc_type` YAML property both failed to fix it; **`columnDefinition = "varchar(N)"`** on the enum fields works. Saved as memory `project-hibernate7-enum-caveat`
- [x] Test: persist + fetch roundtrip via Testcontainers Postgres — `TaxRuleEntitiesPersistenceTest` (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace=NONE)` + Testcontainers `postgres:16-alpine`, `ddl-auto=create-drop`). Needed explicit `spring.datasource.driver-class-name=org.postgresql.Driver` via `@DynamicPropertySource` because H2 (runtime-scoped since slice 1.2) was on the classpath and Boot's auto-config grabbed H2's driver despite the injected Postgres URL. Two tests: (1) full RuleSet+6 slabs+2 thresholds+2 floors roundtrip with `em.flush()/clear()` to force a real DB read, asserting slab order + null top-slab width; (2) two AssessmentYears sharing one RuleSet (§10.0). 18/18 backend tests green via `./mvnw verify`
- [x] Self code-review (high — schema is foundational) — high-effort three-angle review. No blocking findings. Noted follow-ups: (a) no bidirectional-sync helper methods (`addSlab` etc.) — risk of half-set relationships, but mitigated since seeding (3.4) is via Flyway SQL not JPA; (b) `salaryExemptionDivisor` has no DB-level divide-by-zero guard — calc service (3.6) must handle; (c) entity↔Flyway schema-drift seam to reconcile in 3.3; (d) trivial Javadoc typo in `TaxSlab` line 30 (missing close paren). Independent `code-reviewer` subagent offered to user as optional
- [x] Commit `feat(tax): add rule entities`; push — committed as `68a1e92`, pushed to `origin/code`

### 3.3 — Migration tool *(decision + wire-up)*
- [x] Decide Flyway vs Liquibase (record in PLAN.md §2) — **Flyway** (plain-SQL, forward-only). Recorded in PLAN.md §2. Rationale: PLAN §10 treats tax rules as *data* — seeds (3.4) read far cleaner as raw `INSERT` SQL than Liquibase changesets; solo dev already knows SQL; no cross-DB abstraction needed (Postgres is the only real target; H2 is just fast-boot). Forward-only is fine — CLAUDE.md already forbids rewriting applied migrations
- [x] Add dependency + config; V1 migration creates tax rule tables (no seed yet) — `V1__create_tax_rule_tables.sql` creates all 5 `tax_*` tables with named FK/unique constraints, column types mirroring the entities so `ddl-auto: validate` passes. **Three deps needed** (Boot 4 split autoconfig into per-tech modules): `flyway-core` + `flyway-database-postgresql` (the library, for PG16) **and `org.springframework.boot:spring-boot-flyway`** (the autoconfig module). The last one was initially missed — without it Flyway silently never runs, which would have **broken prod boot** (prod uses `validate` against an empty schema). Config: Flyway OFF by default in `application.yml` (H2 dev/tests can't run PG SQL), ON in `application-prod.yml`, force-OFF in `ProdProfileBootTest` (overrides prod→H2)
- [x] Test: migration runs cleanly against Testcontainers — switched `TaxRuleEntitiesPersistenceTest` from `ddl-auto: create-drop` to **Flyway + `validate`** (added `@ImportAutoConfiguration(FlywayAutoConfiguration.class)` — Boot 4's `@DataJpaTest` slice imports only the 2 JPA autoconfigs, not Flyway). Logs confirm `Successfully applied 1 migration ... now at version v1`, then `validate` passes → **proves V1 SQL ⟷ entity mappings agree** (closes the 3.2 drift seam). 17/17 backend tests green under `./mvnw verify`
- [x] Self code-review (medium) — three-angle inline. No blocking findings. Caught the missing `spring-boot-flyway` prod-boot bug. Documented seam: dev/H2 schema via Hibernate `create-drop` vs prod/Postgres via Flyway — both checked against the same entities (validate test is the guard), so transitively consistent. V1 now immutable; seeds → V2 (3.4)
- [x] Commit `feat(backend): wire Flyway migrations + V1 tax schema`; push — committed as `f7979de`, pushed to `origin/code`

### 3.4 — Seed AY 2025-26 + AY 2024-25 rules
- [x] V2 migration: insert one `RuleSet` row with the AY 2025-26 slabs (PLAN.md §10.4), category thresholds (§10.3), minimum-tax floors (§10.6), exemption cap (§10.2), rebate caps (§10.5) — `V2__seed_ay_2024_25_and_2025_26.sql`. Rule set seeded with explicit `id=1` (deterministic/auditable reference data) followed by `ALTER TABLE tax_rule_set ALTER COLUMN id RESTART WITH 2` so later IDENTITY inserts don't collide with id=1 (Postgres `GENERATED BY DEFAULT` doesn't advance the sequence on explicit insert). Every value carries a §-ref comment. 6 paying slabs (ordinal 6 = NULL-width "(rest)"), 6 category thresholds, 3 floors
- [x] V2 migration: insert `AssessmentYear` rows for `2024-25` and `2025-26` pointing to the same `RuleSet` (per PLAN.md §10.0) — both reference `rule_set_id=1`
- [x] Test: query AY 2025-26 → slab structure matches §10.4; AY 2024-25 → same `RuleSet` reference — added 2 read-only seed-verification tests to `TaxRuleEntitiesPersistenceTest` (reuses its Flyway+Testcontainers context): one asserts every §10 value on the AY 2025-26 rule set (scalars, all 6 slabs in order incl. null top width, all 6 thresholds via lookup, all 3 floors), the other asserts AY 2024-25 shares the same rule-set id. Also fixed a collision the seed introduced: the 2 pre-existing roundtrip tests inserted AY label `2025-26`/`2024-25` which now clash with seeded rows → relabeled `TEST-*`. 19/19 backend tests green; Flyway log shows `applied 2 migrations, now at version v2`
- [x] Self code-review (high — tax data correctness) — high-effort self-review (manually cross-checked all values vs §10 + spot-checked against the §10.8 worked example) **plus an independent cold-context reviewer subagent** that re-read §10 against the V2 SQL value-by-value and confirmed all 23 values + enum-name matches + FK consistency with zero discrepancies. Definitive oracle remains the §10.8 worked-example regression in slice 3.12 (net tax = 56,820 vs the user's Excel)
- [x] Commit `feat(tax): seed AY 2024-25 + 2025-26 rule set`; push — committed as `af01450`, pushed to `origin/code`

### 3.5 — DTOs (request + response)
- [x] `TaxCalculationRequest`: income components, category, location, disabled-child count, investments (by category), AIT — with Bean Validation (`@NotNull`, `@PositiveOrZero`) — Java `record` with nested `IncomeComponents` (10 fields) + `Investments` (7 fields) records. Money/count fields `@NotNull @PositiveOrZero`; `category`/`location` `@NotNull`; nested objects `@NotNull @Valid` (cascades). `assessmentYear` optional (null → controller resolves latest in 3.13)
- [x] `TaxCalculationResponse`: full breakdown matching PLAN.md §10.8 — `record` with nested `SlabTax` (ordinal/rate/taxableAmountInSlab/tax). Fields: totalEarnings, taxFreeSalaryExemption, taxableIncome, effectiveFirstSlabThreshold, slabs list, grossTax, eligibleInvestment, rebate, afterRebate, minimumTaxFloor, minimumTaxApplied, taxAfterFloor, advanceIncomeTaxPaid, netTax. `SlabTax` can carry the synthesized 0% band (ordinal 0) so the UI shows the full ladder
- [x] Test: validation rejects negative values and missing required fields — `TaxCalculationRequestValidationTest` (plain `Validation.buildDefaultValidatorFactory()`, no Spring context): valid→no violations, null category, negative nested income (`income.basic` path proves `@Valid` cascade), negative disabledChildren, null AIT money field. 24/24 backend tests green
- [x] Self code-review (medium) — three-angle inline; no findings. Records map via Jackson 3 by component name (no annotations). Note: `@Valid` on the request only fires once the controller annotates the body (slice 3.13); DTOs define the contract now
- [x] Commit `feat(tax): add request/response DTOs`; push — committed as `d91389a`, pushed to `origin/code`

### 3.6 — Calculation service: salary exemption
- [x] `TaxCalculationService` skeleton — pure function `(RuleSet, TaxCalculationRequest) → intermediate`, no DB inside — `@Service` in `tax.service` package (PLAN §4). No repository injected (pure). Step methods are package-private so the same-package test calls them directly; the public orchestrating `calculate(...)` is assembled across later slices as pieces land. Added `Money` helper (package-private) centralizing the rounding policy (PLAN §10.10: scale 2, HALF_UP) with `scale()` + `divide()`
- [x] Step 1: `taxFreeSalary = min(total/3, 450k)` — `salaryExemption(RuleSet, totalEarnings)` = `MIN(Money.divide(total, divisor), cap)`. Also `totalEarnings(IncomeComponents)` sums the 10 components at monetary scale
- [x] Test: salary exemption below and above the 450k cap — 4 tests: totalEarnings sum, below-cap (900k/3=300k), at/above-cap (worked-example 1,611k/3=537k→450k), and a fractional rounding case (1,000k/3=333,333.33 HALF_UP). 28/28 backend tests green
- [x] Self code-review (high — money math) — high-effort three-angle. No blocking findings; rounding centralized; divide-by-zero on divisor still the only standing flag (seeded data safe). Cumulative correctness oracle is the §10.8 worked-example regression (3.12)
- [x] Commit `feat(tax): calculation — salary exemption`; push — committed as `306d5b2`, pushed to `origin/code`

### 3.7 — Calculation service: effective first-slab threshold
- [x] Step 2: `effectiveThreshold = categoryThreshold + 50,000 × disabledChildren` — `effectiveFirstSlabThreshold(RuleSet, category, disabledChildren)`; private `categoryThreshold(...)` looks up the category's amount in `RuleSet.categoryThresholds`, throwing `IllegalStateException` if absent (data-integrity guard; seed has all 6). `Money.scale` applied
- [x] Test: one case per taxpayer category (6 tests); disabled-child add (0, 1, 3 children) — two `@ParameterizedTest` `@CsvSource` methods (junit-jupiter-params 6.0.3 confirmed on classpath): 6 categories vs §10.3 bases, and GENERAL + {0,1,3} children → 350k/400k/500k. 37/37 backend tests green
- [x] Self code-review (high — money math) — high-effort three-angle; no blocking findings. Cumulative oracle = §10.8 regression (3.12)
- [x] **Architecture note:** discussed Chain-of-Responsibility/Pipeline with user. Decided to KEEP isolated step-methods (CoR misfits fixed-order math; a mutable shared context is a downgrade vs typed returns for money math; respects CLAUDE.md rule 7). Revisit only at wealth surcharge / year-varying step. When assembling `calculate(...)` (3.8-3.11), thread an immutable accumulator record. Recorded as memory `project-calc-structure-decision`
- [x] Commit `feat(tax): calculation — effective threshold`; push — committed as `2679691`, pushed to `origin/code`

### 3.8 — Calculation service: slab walk
- [x] Step 3: walk slabs in order, taxing `min(remaining, slabWidth) × rate`; produce slab-by-slab breakdown — `walkSlabs(RuleSet, effectiveThreshold, taxableIncome)` → `SlabWalkResult(grossTax, List<SlabTax>)`. `remaining = max(taxable, 0)`; **band 0** = synthesized 0% band (width = effective threshold); then stored paying slabs in ordinal order; `null` width = open-ended top slab absorbs the rest. `Money.scale` on every amount + tax. `SlabWalkResult` is the first immutable accumulator piece (per the calc-structure decision)
- [x] Test: matches expected per-slab tax for typical incomes (low → all in slab 1, mid → spans 1–3, high → spans all 7) — 4 tests: low (200k → band 0 only, gross 0), worked-example mid (1,161,000 → **91,650**, per-slab amounts 350k/100k/400k/311k asserted incl. the partial 15% slab), high (5,000,000 → 1,065,000, top slab absorbs 1,150,000@30%), zero taxable → 0. 41/41 backend tests green
- [x] Self code-review (high — money math) — high-effort three-angle; no blocking findings. Worked example reproduces §10.8 gross tax exactly. Cumulative oracle = §10.8 regression (3.12)
- [x] Commit `feat(tax): calculation — slab walk`; push — committed as `f5cdcd3`, pushed to `origin/code`

### 3.9 — Calculation service: investment rebate
- [x] Step 4: per-item investment caps (PLAN.md §10.5), then `rebate = min(0.03 × taxable, 0.15 × eligible, 1,000,000)`; rebate = 0 if `taxable ≤ 0` — **per-item caps made data-driven** (user decision, rule 6): added `sanchay_patra_cap` + `dps_cap` to `RuleSet` via **V3 migration** (add nullable → backfill id=1 to 500k/120k → `SET NOT NULL`) + entity fields. `eligibleInvestment(RuleSet, Investments)` caps Sanchay/DPS, sums the other 5 uncapped. `investmentRebate(...)` = `MIN(taxableFraction×taxable, eligibleFraction×eligible, rebateCap)`, returns 0 when `taxable.signum() ≤ 0`
- [x] Test: rebate cap binding on each of the three legs (3%, 15%, 1M); zero-taxable case — 5 tests: per-item cap application (770k), 3% leg binds (§10.8 → 34,830), 15% leg binds (15,000), 1M cap binds (1,000,000), zero + negative taxable → 0. Also updated `TaxRuleEntitiesPersistenceTest`: `newRuleSet()` sets the two new NOT-NULL caps; seed test asserts them. 46/46 backend tests green; Flyway applies V1+V2+V3
- [x] Self code-review (high — money math) — high-effort three-angle; no blocking findings. V3 migration order correct + forward-only; dev/H2 unaffected (Flyway off, create-drop from entity); validate confirms V3 schema ↔ entity. Cumulative oracle = §10.8 regression (3.12)
- [x] Commit `feat(tax): calculation — investment rebate`; push — committed as `44ed601`, pushed to `origin/code`

### 3.10 — Calculation service: minimum tax floor
- [x] Step 5: `withFloor = (taxable > 0) ? max(afterRebate, floorByLocation) : 0` — `afterRebate(grossTax, rebate)` = MAX(0, gross−rebate); `applyMinimumTaxFloor(RuleSet, Location, taxableIncome, afterRebate)` → `MinimumTaxResult(taxAfterFloor, floor, applied)`: returns 0 when `taxable.signum() ≤ 0`, else MAX(afterRebate, floor) with `applied = floor > afterRebate`. Private `minimumTaxFloor(RuleSet, Location)` lookup throws `IllegalStateException` if absent (mirrors category-threshold lookup)
- [x] Test: below-floor case per location (Dhaka 5k, other-CC 4k, other 3k); zero taxable income → no floor applied — 6 tests: afterRebate (worked-example 56,820 + clamps at 0), `@ParameterizedTest` 3 locations below-floor (bumped + applied=true), above-floor unchanged (applied=false), zero-taxable → 0. 52/52 backend tests green
- [x] Self code-review (high — money math) — high-effort three-angle; no blocking findings. Worked-example after-rebate = 56,820 confirmed; Dhaka floor not binding. `MinimumTaxResult` immutable record carries the response's floor fields. Cumulative oracle = §10.8 regression (3.12)
- [x] Commit `feat(tax): calculation — minimum tax floor`; push — committed as `4364047`, pushed to `origin/code`

### 3.11 — Calculation service: AIT credit
- [x] Step 6: `netTax = max(0, withFloor − AIT)`; modeled separately from rebate — `netTax(withFloor, advanceIncomeTaxPaid)` = MAX(0, withFloor − AIT). **Also assembled the public `calculate(RuleSet, assessmentYear, TaxCalculationRequest) → TaxCalculationResponse`** threading all six steps via local finals (chose locals over an accumulator record — clearer for a single straight-line method; the `SlabWalkResult`/`MinimumTaxResult` records already bundle multi-value outputs). Response constructor order matches the record component order
- [x] Test: AIT > tax → net = 0 (no refund modeled); AIT < tax → net = tax − AIT — 2 netTax tests + a `calculate()` assembly test on a simple hand-computed scenario (basic 900k → net 20,000, full breakdown wired, 7 slab rows). Reusable `fullRuleSet()`/`basicOnly()`/`noInvestments()` fixtures added for 3.12. Exact §10.8 → 56,820 regression is slice 3.12. 55/55 backend tests green
- [x] Self code-review (high — money math) — high-effort three-angle; no blocking findings. calc math now complete end-to-end. Cumulative oracle = §10.8 regression (3.12, next)
- [x] Commit `feat(tax): calculation — AIT credit`; push — committed as `61a9fd8` (incl. `calculate()` assembly), pushed to `origin/code`

### 3.12 — Worked-example regression test
- [x] End-to-end test exactly reproducing PLAN.md §10.8 (expected net tax = 56,820) — `workedExampleFromPlanSection10_8ProducesNetTax56820` in `TaxCalculationServiceTest` feeds the exact §10.8 inputs (basic 1,611,000; Sanchay 200,000 + DPS 120,000; GENERAL; Dhaka; AIT 0) through `calculate(fullRuleSet, "2025-26", request)` and asserts the full breakdown: total 1,611,000 → exemption 450,000 → taxable 1,161,000 → gross 91,650 → eligible 320,000 → rebate 34,830 → afterRebate 56,820 → Dhaka floor 5,000 (not binding) → **netTax 56,820**, plus the per-slab rows (band0/5%/10%/15%-partial). Reuses existing fixtures; no production change. 56/56 backend tests green. Note carried to 3.13: `calculate()` reads the rule set's lazy `@OneToMany` collections, so the controller must load it with collections initialized (entity graph / transaction)
- [x] Self code-review (medium) — test-only; encodes every §10.8 row + headline 56,820 as a permanent regression guard. No findings
- [x] Commit `test(tax): worked-example regression`; push — committed as `bc5569e`, pushed to `origin/code`

### 3.13 — POST /api/calculators/tax/calculate
- [x] Controller: `POST /api/calculators/tax/calculate` — accepts `TaxCalculationRequest`, returns `TaxCalculationResponse`; resolves active `RuleSet` by AY (default to latest) — `TaxCalculationController` (`tax.web`) delegates to a new `TaxCalculationFacade` (`tax.service`, `@Transactional(readOnly=true)`) which resolves the AY via `AssessmentYearRepository` (`findByLabel`, or `findTopByOrderByLabelDesc` when the request omits it) and calls the pure `TaxCalculationService.calculate(...)`. The read-only transaction keeps the session open so the rule set's lazy `@OneToMany` collections initialize before the calc walks them — kept the pure service repo-free; transaction lives in the service layer, not the controller
- [x] Test: MockMvc happy path; validation error returns 400 with global-error JSON shape — `TaxCalculationControllerTest` (`@SpringBootTest` + MockMvc + Testcontainers + Flyway, real HTTP→facade→DB→calc): worked-example POST → 200 + **netTax 56,820** + assessmentYear resolves to latest "2025-26" (proves lazy-loading works end-to-end — the open risk from 3.12); unknown AY → 404 `NOT_FOUND`; invalid (null category) → 400 `VALIDATION_ERROR` with `fieldErrors`. **Gotcha:** first named it `...IT`, which Surefire silently skips (no Failsafe configured) — renamed to `...Test` to match the project's existing Testcontainers-as-Surefire convention. 59/59 backend tests green
- [x] Self code-review (high — public API surface) — high-effort three-angle; no blocking findings. Boot 4 package reorg again: `AutoConfigureMockMvc` is at `org.springframework.boot.webmvc.test.autoconfigure`. Follow-up (non-blocking): no Failsafe `*IT` split — all integration tests run under Surefire by `*Test` naming; could formalize later
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
