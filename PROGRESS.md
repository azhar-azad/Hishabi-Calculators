# Calculators — Implementation Progress

Granular checklist tracked **phase by phase**, with each phase broken into **slices**. A slice is the smallest unit that is independently implementable, testable, reviewable, and committable. **One slice = one commit.** Mark each checkbox the moment it's done — don't batch.

Companion to [CLAUDE.md](./CLAUDE.md) and [PLAN.md](./PLAN.md).

Legend: `[ ]` = todo, `[x]` = done, `[~]` = in progress, `[-]` = skipped/deferred (with note).

**Per-slice loop (binding — see CLAUDE.md):** implement → tests green → self code-review (medium; high for security/auth/money) → commit on `code` (Conventional Commits, reference slice ID in body) → push. PR `code` → `main` at natural checkpoints (typically end of phase).

---

## Completed phases (0–6)

All slices done. Full detail (design rationale, gotchas, exact commit hashes) in [PROGRESS_ARCHIVE.md](./PROGRESS_ARCHIVE.md).

| Phase | What was built | Last commit |
|-------|---------------|-------------|
| 0 — Repo bootstrap | Monorepo layout, `.gitignore`, `code` branch | `bdaae0d` |
| 1 — Backend scaffold | Spring Boot 4 skeleton, dev/prod profiles, `/api/health`, global exception handler, CORS, Spotless, JaCoCo, Testcontainers, GitHub Actions CI | `623caa0` |
| 2 — Frontend scaffold | Next.js 16 + React 19, Vitest + RTL, landing page, Prettier + strict TS, `lib/api.ts`, `/dev/health` probe, shadcn/ui theme, GitHub Actions CI | `f5012b4` |
| 3 — Tax backend | Domain enums, rule entities, Flyway V1–V3 migrations, AY 2024-25/2025-26 seed, DTOs, full 6-step calculation service (salary exemption → threshold → slab walk → rebate → floor → AIT), worked-example regression (§10.8 → net 56,820 BDT), `POST /calculate`, `GET /rules/{year}`, dev H2 seeding | `fix(backend): seed dev (H2)` |
| 4 — Tax frontend | Tax route + metadata, rules view (slab table + category thresholds), input form (19 fields, RHF + zod v4), client-side validation, `POST /calculate` integration, full breakdown render (§10.8 worked example), mobile-responsive polish | `2760266` |
| 5 — Auth + history | `users` table, signup (BCrypt), login + JWT (HS256), stateless Spring Security filter chain, `calculations` history table (+ `calculator_type` discriminator), persist-when-logged-in, list endpoint, frontend signup/login/history pages + auth context, save CTA | `feat(frontend-auth): history page` |
| 6 — Deployment | Backend Dockerfile (Render), Neon Postgres, prod env config + fail-fast profile enforcer, Vercel static frontend (`output: 'export'`), CORS lockdown, prod smoke test (net 56,820), UptimeRobot keep-alive | `d35fb7e` |

---

## Phase 7 — Tax assessment year 2026-27

NBR released the AY 2026-27 individual schedule with changed values (General threshold 350k→**375k**, new slab ladder with no 5% band and a 35% top rate, rebate eligible-fraction 15%→**10%**, rebate cap 1,000,000→**750,000**). Because tax rules are data (PLAN.md rule 6), this is a new Flyway migration + tests + a frontend year selector — **no calculation-logic changes**. New §10.8-style worked-example anchor: **net tax = 75,200** (was 56,820). Full rule tables + delta in PLAN.md §12.

### 7.0 — Docs restructure + rule documentation *(chore, docs-only)*
- [x] Archive Phases 5 & 6 detail into `PROGRESS_ARCHIVE.md`; collapse to summary rows here
- [x] PLAN.md §2 decisions-log row for AY 2026-27
- [x] PLAN.md §12 — full AY 2026-27 rule tables + worked example (→ 75,200)
- [x] PLAN.md §11 — mark "multi-year UI" done; remove resolved `rebateLegLabel` tech-debt item
- [x] Commit `chore(docs): archive phases 5-6, document AY 2026-27 plan + rules`; push

### 7.1 — Backend: seed AY 2026-27 + years endpoint *(high-risk: tax math + migration)*
- [x] `V7__seed_ay_2026_27.sql`: new `tax_rule_set` id=2 (populate **every** column incl. `sanchay_patra_cap`/`dps_cap` from V3); `ALTER ... RESTART WITH 3`; 6 slabs, 6 thresholds (General 375k), 3 floors for rule_set_id=2; `tax_assessment_year ('2026-27', 2)`
- [x] `GET /api/calculators/tax/years` — available year labels, newest first (facade + controller); add to Spring Security public allow-list
- [x] Test: `TaxRuleEntitiesPersistenceTest` — 2026-27 rule set matches new values; 2026-27 → different RuleSet (id=2) than 2025-26 (id=1); 2024-25 + 2025-26 still share id=1
- [x] Test: `TaxCalculationServiceTest` — worked example for 2026-27 → gross 107,200, rebate 32,000, **net 75,200** + per-slab breakdown
- [x] Test: fix latest-default flip — pin existing `netTax=56820` controller test to explicit `2025-26`; add null-year-resolves-to-2026-27 (→ 75,200) test
- [x] Test: `TaxRulesControllerTest.returnsFullRuleSetForAy2026_27`; years-endpoint test (public, returns `[2026-27, 2025-26, 2024-25]`)
- [x] `./mvnw verify` green — 100/100 tests
- [x] Self code-review (high — tax math, `/code-review`) + independent cold-context reviewer (recomputed net 75,200, all values match §12) — no findings
- [x] Commit `feat(tax): seed AY 2026-27 rules + years endpoint`; push

### 7.2 — Frontend: assessment-year selector + copy + rebate-label fix
- [x] New `features/tax/TaxCalculator.tsx` client wrapper: fetch `/years`, hold `selectedYear` (default newest), shadcn `Select`; fetch rules once and pass to `TaxRulesView` + `TaxCalculatorForm`
- [x] `TaxRulesView` (now presentational, takes `rules`) / `TaxCalculatorForm` (optional `assessmentYear` + `rules` props, omits year → backend latest): dropped hardcoded `ASSESSMENT_YEAR`
- [x] `TaxBreakdown.rebateLegLabel`: takes rebate fractions/cap via `rebateConfig` prop, legacy 2025-26 fallback (fixes PLAN.md §11 tech debt)
- [x] Copy: `app/calculators/tax/page.tsx` + `app/page.tsx` year-neutral
- [x] Tests (`__tests__/`): wrapper defaults to newest year + sends it in payload; breakdown label uses 10%/750k (and 2025-26 fallback); rewrote rules-view/page tests for the new props
- [x] `npm run check` green — 39 tests, lint/type/format clean
- [x] Commit `feat(frontend-tax): assessment-year selector + AY 2026-27`; push

### 7.3 — Verify end-to-end + PR
- [x] Live backend (dev/H2, ran V1–V7) verified via HTTP: `/years` → `[2026-27,2025-26,2024-25]`; default calc → 2026-27 net 75,200; `2025-26` → 56,820; `/rules/2026-27` → 10%/750k rebate, 35% top slab, General 375k
- [x] Browser preview: filled §12.3 inputs → "Tax Breakdown — AY 2026-27", gross 107,200, net 75,200, rebate bound to "10% of eligible investment"; rules table shows 35% top slab + General 375k
- [x] Fix found in preview: year-selector trigger showed placeholder (async-mounted items) — render Select only after years load (`fix(frontend-tax): show selected year in the selector trigger`)
- [x] CI green after push — Backend (Maven verify) + Frontend (check + build) + Vercel all pass
- [x] PR `code` → `main` opened (#16); Render auto-runs V7 on next deploy — no manual DB step

---

## Phase 8 — Zakat calculator

Second platform calculator. Source of truth: the **As-Sunnah Foundation** online Zakat calculator (full **28-field** replica). Mirrors the tax module's data-driven pattern (Flyway-seeded constants → pure calc service → facade → Next.js feature). Key decisions: nisab basis **user-selectable** (gold/silver, default silver); rate **calendar-driven** (Hijri 2.5% / English-solar 2.6%); gold/silver valued at **83% of BAJUS price** (live fetch → DB cache, **daily** refresh, stale fallback) with weight in grams or **Vori/Ana/Roti/Point**; history **deferred**. Full design + worked examples in PLAN.md §13.

### 8.0 — Docs *(chore, docs-only)*
- [x] PLAN.md §2 decisions-log row for Zakat
- [x] PLAN.md §13 — full Zakat rules (model, 28 fields, nisab, calendar rate, BAJUS, weight units, worked examples, data model)
- [x] PROGRESS.md Phase 8 plan (this section)
- [x] Commit `chore(docs): document Zakat calculator plan + As-Sunnah rules`; push

### 8.1 — Backend: schema + calc service + config/calculate *(high-risk: money + migration)*
- [x] `V8__create_zakat_tables.sql`: `zakat_rule_set` + `metal_price`
- [x] `V9__seed_zakat_rules.sql`: rule set row (612.36 / 87.48 / 0.83 / 0.025 / 0.026) + initial `metal_price` snapshot (silver 340.87 BDT/g → nisab ≈ 173,250; `TIMESTAMP WITH TIME ZONE` for H2 compat)
- [x] Entities (`ZakatRuleSet`, `MetalPrice`) + repos; DTOs (`ZakatCalculationRequest` w/ `nisabBasis`+`calendarType`+21 assets+7 deductions; `ZakatCalculationResponse`; `ZakatConfigResponse`)
- [x] `ZakatCalculationService` (pure): Σ assets − Σ deductions, floor at 0, nisab compare by basis, rate by calendar
- [x] `ZakatCalculationFacade`: resolve config + nisab from cache; persist if authenticated (`CalculatorType.ZAKAT`)
- [x] Controller `GET /config` + `POST /calculate`; add both to `SecurityConfig` public allow-list
- [x] Tests: 13 service tests (below/at/above nisab; English 2.6% & Hijri 2.5%; gold vs silver nisab; deductions; net floored at 0; stale passthrough); config + calculate controller; Testcontainers persistence of seeded rule set
- [x] `./mvnw verify`: 89 tests, 0 failures, 11 Docker errors (all pre-existing Testcontainers pattern, CI green); self code-review (high — 8 angles); independent subagent review — 2 low-sev findings (stale seed cliff noted for Slice 8.2; at-nisab boundary test added)
- [x] Commit `feat(zakat): rule schema + calculation service + config/calculate endpoints`; push

### 8.2 — Backend: BAJUS live price + cache/fallback + metal-value *(high-risk: external I/O + money)*
- [ ] `pom.xml`: add Jsoup
- [ ] `BajusPriceService`: browser-header `RestClient` fetch, Jsoup parse, upsert `metal_price`, daily `@Scheduled` + `ApplicationReadyEvent` fetch, stale detection
- [ ] `WeightUnit` enum + grams conversion (Gram/Vori/Ana/Roti/Point)
- [ ] `POST /metal-value` endpoint (items `{metal,carat,quantity,unit}` → per-item + total selling value + `{fetchedAt,stale}`); add to allow-list
- [ ] Thread `priceFetchedAt`/`priceStale` into config + calculate responses; nisab calibration vs live As-Sunnah figure
- [ ] Tests: parser against saved BAJUS HTML fixture; fallback-to-cache (fetch fails → stale); weight-unit conversion; metal-value endpoint
- [ ] `./mvnw verify` green; self code-review (high); independent `code-reviewer` subagent
- [ ] Commit `feat(zakat): BAJUS live price with DB cache + stale fallback`; push

### 8.3 — Frontend: form + breakdown + config wrapper
- [ ] Read `node_modules/next/dist/docs/` (non-standard Next.js per `frontend/AGENTS.md`)
- [ ] `features/zakat/`: `schema.ts` (zod, 28 fields + `calendarType` + `nisabBasis`), `types.ts`
- [ ] `ZakatCalculator` (fetch `/config`, nisab-basis selector + nisab display/stale note), `ZakatCalculatorForm` (2-step wizard, all 28 fields, calendar select + date), `ZakatBreakdown`
- [ ] `app/calculators/zakat/page.tsx` route; flip home-card from "Coming soon" to a live link
- [ ] Tests (`__tests__/`): all fields render; calculate payload shape; breakdown shows nisab / below-nisab / zakat + rate
- [ ] `npm run lint && npm test` green; self code-review
- [ ] Commit `feat(frontend-zakat): calculator form + breakdown`; push

### 8.4 — Frontend: price helper modal + stale indicators + E2E + PR
- [ ] `MetalPriceModal` (items: name, quantity, unit selector Gram/Vori/Ana/Roti/Point, carat, Add More) → `POST /metal-value` → total + stale indicator → Confirm writes value into gold/silver field (field stays directly editable)
- [ ] Stale indicators on nisab + prices
- [ ] Tests (modal + stale states)
- [ ] End-to-end via preview tools: both calendar types (2.5% vs 2.6%), below/above nisab, price helper total, simulated stale fallback
- [ ] CI green after push; open PR `code` → `main`
- [ ] Commit `feat(frontend-zakat): live price helper + stale indicators`; push

---

## Future phases

- [ ] **Zakat history** — wire `CalculatorType.ZAKAT` into save-on-calc + the account/history view (deferred from Phase 8)
- [ ] **Mobile app** — pick stack (React Native vs Flutter vs native) based on user comfort at that point
- [ ] **Analytics / error tracking** — opt-in (see PLAN.md §11)
- [ ] **i18n** — Bengali + English (likely needed for BD audience; confirm with user)
- [ ] **Wealth surcharge** for net wealth > 4 crore BDT (see PLAN.md §11)

---

_When adding new slices mid-phase, append them in place (renumbering is fine — git history is the source of truth, not slice IDs). When a slice is no longer relevant, mark `[-]` with a one-line note rather than deleting — keeps the history of decisions intact._
