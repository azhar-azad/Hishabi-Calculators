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
- [ ] New `features/tax/TaxCalculator.tsx` client wrapper: fetch `/years`, hold `selectedYear` (default newest), shadcn `Select`; fetch rules once and pass to `TaxRulesView` + `TaxCalculatorForm`
- [ ] `TaxRulesView` / `TaxCalculatorForm`: drop hardcoded `ASSESSMENT_YEAR`; take year (+ rules) as props
- [ ] `TaxBreakdown.rebateLegLabel`: take rebate fractions/cap from rules instead of hardcoded 0.03/0.15/1M (fixes PLAN.md §11 tech debt)
- [ ] Copy: `app/calculators/tax/page.tsx` + `app/page.tsx` year-neutral
- [ ] Tests (`__tests__/`): year options render + switching changes payload year; form default year 2026-27; breakdown label uses 10%/750k
- [ ] `npm run lint && npm test` green
- [ ] Commit `feat(frontend-tax): assessment-year selector + AY 2026-27`; push

### 7.3 — Verify end-to-end + PR
- [ ] Run backend (dev/H2, runs V1–V7) + frontend; preview-verify: selector lists 3 years; 2026-27 §10.8 inputs → 75,200; 2025-26 → 56,820; rules table shows 35% top slab + General 375k
- [ ] Confirm CI green after push
- [ ] PR `code` → `main` (Render auto-runs V7 on next deploy — no manual DB step)

---

## Future phases

- [ ] **Zakat calculator** — repeat Phases 3–4 under `calculators/zakat/` (will be sliced when started)
- [ ] **Mobile app** — pick stack (React Native vs Flutter vs native) based on user comfort at that point
- [ ] **Analytics / error tracking** — opt-in (see PLAN.md §11)
- [ ] **i18n** — Bengali + English (likely needed for BD audience; confirm with user)
- [ ] **Wealth surcharge** for net wealth > 4 crore BDT (see PLAN.md §11)

---

_When adding new slices mid-phase, append them in place (renumbering is fine — git history is the source of truth, not slice IDs). When a slice is no longer relevant, mark `[-]` with a one-line note rather than deleting — keeps the history of decisions intact._
