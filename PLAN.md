# Calculators — Architecture & Plan

Companion to [CLAUDE.md](./CLAUDE.md) (rules) and [PROGRESS.md](./PROGRESS.md) (granular steps). This file holds the **why** behind decisions and the **direction** of the project. Update it whenever a design choice changes.

---

## 1. Vision

A web-first platform hosting multiple calculators under one brand. Each calculator is self-contained but shares a common shell (auth, accounts, history, UI chrome). Mobile follows later, reusing the same backend.

**First calculator:** Bangladeshi individual income tax.
**Second calculator (planned):** Zakat.
**Beyond:** open — anything financial/civic that benefits from being calculated.

## 2. Stack & decisions log

| Decision                 | Choice                                    | Date       |
| ------------------------ | ----------------------------------------- | ---------- |
| Backend framework        | Spring Boot (Java 21, Maven)              | 2026-05-26 |
| Frontend framework       | Next.js (React, TypeScript)               | 2026-05-26 |
| Database                 | PostgreSQL                                | 2026-05-26 |
| Auth                     | JWT, accounts optional                    | 2026-05-26 |
| Repo layout              | Monorepo (backend/ + frontend/)           | 2026-05-26 |
| Deployment target        | Render / Railway / Fly.io (free tier)     | 2026-05-26 |
| Backend hosting          | Render (free web service — spins down 15 min idle, 750 hrs/month; mitigate with UptimeRobot keep-alive) | 2026-06-12 |
| Frontend hosting         | Vercel (Hobby plan — free forever for Next.js) | 2026-06-12 |
| Database hosting         | Neon (free forever, 0.5 GB — chosen over Render Postgres which expires after 30 days) | 2026-06-12 |
| Mobile strategy          | Deferred; backend kept as clean REST API  | 2026-05-26 |
| Tax-year scope (initial) | Schema multi-year, seed current year only | 2026-05-26 |
| Java base package        | `dev.azhar.hishabi`                       | 2026-05-27 |
| Minimum tax floor        | Included from MVP (location-based)        | 2026-05-26 |
| Wealth surcharge         | Deferred — out of MVP scope               | 2026-05-26 |
| PF Net Interest Income   | Excluded from eligible investments        | 2026-05-26 |
| Brand / product name     | **Hishabi**                               | 2026-05-26 |
| Domain                   | None — use platform default URL           | 2026-05-26 |
| Backend test stack       | JUnit 5 + AssertJ + Mockito + Testcontainers | 2026-05-26 |
| Frontend test stack      | Vitest + React Testing Library            | 2026-05-26 |
| Backend code quality     | Spotless (Google Java Format) + JaCoCo    | 2026-05-26 |
| Frontend code quality    | ESLint + Prettier + TS `strict: true`     | 2026-05-26 |
| CI                       | GitHub Actions (tests + lint on PR to main) | 2026-05-26 |
| Git workflow             | All work on `code` branch; PR → `main`    | 2026-05-26 |
| Dev profile database     | H2 in-memory (PostgreSQL mode)            | 2026-05-26 |
| Frontend styling         | Tailwind CSS                              | 2026-05-27 |
| Frontend UI library      | shadcn/ui (Radix + Tailwind)              | 2026-05-27 |
| DB migration tool        | Flyway (plain-SQL, forward-only)          | 2026-05-29 |
| Monetary rounding        | 2 dp (paisa), HALF_UP, centralized        | 2026-05-29 |
| Dev DB seeding           | Flyway runs on H2 (PG mode); `ddl-auto: validate` — supersedes "Flyway off on H2" | 2026-06-01 |
| Frontend form stack      | react-hook-form + zod (+ shadcn Input/Select/Label/Card) | 2026-06-02 |
| Calc service structure   | Step methods (pure functions, local finals) — not Chain-of-Responsibility. CoR is a poor fit for fixed-order money math; typed returns are clearer than a mutable shared context. Revisit only if wealth surcharge adds year-varying steps. | 2026-05-30 |
| AY 2026-27 rules added    | New `RuleSet` (id=2) — General threshold 350k→375k, new slab ladder (no 5% band, 35% top rate), rebate eligible-fraction 15%→10%, rebate cap 1,000,000→750,000. Frontend gains a year selector + `/years` endpoint. See §12. | 2026-06-20 |
| Zakat calculator added    | Second calculator. Source of truth: **As-Sunnah Foundation** online calculator. Full **28-field** replica; nisab basis **user-selectable** (gold/silver, default silver); rate **calendar-driven** (Hijri 2.5% / English-solar 2.6%); gold/silver valued at **83% of BAJUS price** (live fetch → DB cache, **daily** refresh, stale fallback); helper weight in grams or **Vori/Ana/Roti/Point**. History deferred. See §13. | 2026-06-21 |

Add a new row whenever a decision is made or changed.

## 3. Architecture (high level)

```
            ┌──────────────────────────────────────────┐
            │   Next.js (Vercel or same host)          │
            │   - Public pages: landing, calc UIs      │
            │   - Auth flows (login/signup)            │
            │   - Calls backend via REST/JSON          │
            └────────────────┬─────────────────────────┘
                             │  HTTPS, JSON
                             ▼
            ┌──────────────────────────────────────────┐
            │   Spring Boot backend                    │
            │   - REST controllers per calculator      │
            │   - Auth (JWT)                           │
            │   - Calculation history persistence      │
            │   - Tax rules engine (data-driven)       │
            └────────────────┬─────────────────────────┘
                             │  JDBC
                             ▼
                       ┌──────────────┐
                       │  PostgreSQL  │
                       └──────────────┘
```

Frontend is the only first-party client today. A mobile app is a future second client — that's why the backend stays headless and JSON-only.

## 4. Backend module layout (target)

```
backend/src/main/java/dev/azhar/hishabi/
  platform/        cross-cutting: auth, users, history, config, errors
  calculators/
    tax/           Bangladeshi income tax calculator
      model/         entities + DTOs
      rules/         year-keyed rule sets (data)
      service/       calculation logic
      web/           REST controller
    zakat/         (future)
```

Each calculator is its own package. The `platform` package owns anything shared.

## 5. Frontend module layout (target)

```
frontend/src/
  app/                  Next.js App Router
    (marketing)/         landing
    calculators/
      tax/                 tax calculator pages
      zakat/               (future)
    account/             login, signup, history
  components/           shared UI
  lib/                  api client, auth helpers
  features/
    tax/                  tax-specific components, schemas
    zakat/                (future)
```

Same principle: per-calculator feature folders, shared shell.

## 6. Domain model — tax calculator (sketch)

Derived from §10 (tax rules). Initial entities:

- `AssessmentYear` — e.g., `"2025-26"`, references a `RuleSet` (many AYs can share one rule set when NBR doesn't amend the rules — see §10.0)
- `RuleSet` — the actual slabs, thresholds, exemption cap, rebate caps, minimum-tax floors. Versioned independently of `AssessmentYear`.
- `TaxSlabSet` — ordered list of `(width, rate)` for an assessment year (slab widths are constant across categories; only the first slab's width = the category's tax-free threshold)
- `TaxpayerCategory` enum — `GENERAL`, `WOMAN`, `SENIOR_65_PLUS`, `PHYSICALLY_MENTALLY_DISABLED`, `GAZETTED_FREEDOM_FIGHTER`, `THIRD_GENDER`
- `Location` enum — `DHAKA_CHITTAGONG_CITY_CORP`, `OTHER_CITY_CORP`, `OTHER` (for minimum tax)
- `CategoryThreshold` — `(assessmentYear, category) -> taxFreeAmount`
- `MinimumTaxFloor` — `(assessmentYear, location) -> floorAmount`
- `IncomeInput` — basic, house rent, conveyance, medical, leave encashment, performance bonus, yearly bonus, festival bonus, overtime, transportation, AIT, plus category, location, disabled-child count
- `InvestmentInput` — sanchay patra, DPS, mutual fund, treasury bond, PF employee, PF employer, stock (per-category caps applied server-side)
- `CalculationResult` — full breakdown: tax-free salary exemption, taxable income, slab-by-slab tax, rebate computation, AIT credit, minimum-tax bump, final net tax
- `Calculation` — persisted history record (per user, optional)

## 7. Auth & persistence approach

- **Anonymous use** is the default — the calculator works without login.
- **Optional sign-up** unlocks saved calculation history.
- JWT issued by backend; frontend stores it (httpOnly cookie preferred over localStorage).
- Spring Security with stateless filter chain.
- Password hashing: BCrypt.
- Email verification: deferred (not blocking MVP).

## 8. Deployment plan

Target one of: **Render**, **Railway**, **Fly.io**. Final pick deferred until we reach the deployment phase — we'll evaluate based on free-tier limits at that moment.

Constraints to honor regardless of host:

- Backend = single Spring Boot jar in a Docker image.
- Frontend = static + serverless (Vercel) or Node container (Render/Railway).
- Postgres = managed instance from the same provider.
- Secrets via env vars only — never committed.

## 9. Open questions

These need user input before the relevant phase starts:

- [x] ~~Official Bangladesh income tax rules for AY 2025-26~~ — derived from user's Excel, see §10.
- [x] ~~Confirm: +50,000 per disabled child applies universally~~ — yes, all categories.
- [x] ~~Surcharge for wealth > 4 crore BDT~~ — deferred, see §11.
- [x] ~~Analytics/error tracking for MVP~~ — deferred, see §11.
- [x] ~~Brand / product name~~ — **Hishabi**.
- [x] ~~Domain name~~ — none; use platform-default URL (e.g., `hisaabi.onrender.com`). Custom domain revisit only if shared widely.

_All MVP-blocking open questions are now resolved. Add new ones here as they come up._

## 11. Deferred / future scope

Things consciously punted from MVP. Track here so we don't lose them.

| Item | Notes |
| --- | --- |
| Wealth surcharge (>4 crore net wealth) | Adds a "net wealth statement" input flow and progressive surcharge brackets. Revisit after core tax flow ships. |
| Analytics & error tracking | Skip for MVP. Candidates when we revisit: Plausible / Umami (privacy-friendly analytics), Sentry (error tracking, generous free tier). Add an opt-in toggle if we ever go beyond personal use. |
| Business / professional income | MVP models salary only. Other heads of income require their own input flows and (sometimes) different rates. |
| Capital gains & other special-rate incomes | Same as above. |
| Tax return PDF / NBR filing format | Long-tail feature; nice-to-have only if the calculator gets shared widely. |
| ~~Multi-year historical calculation UI~~ | **Done (Phase 7).** With AY 2026-27 added as a genuinely different rule set, the frontend gained an assessment-year selector (fed by `GET /api/calculators/tax/years`). |
| Mobile app | Backend kept REST-clean so any client (React Native / Flutter / native) is viable when we get there. |
| i18n (Bengali + English) | Likely needed if shared beyond colleagues. Skip for personal-use MVP. |
| Bruno: add `API-Collection/README.md` | One-liner: "Bruno collection — open in Bruno, point at a running backend." |
| Bruno: rename folder `API-Collection/` → `api-collection/` | Lowercase, for consistency with `backend/` / `frontend/`. Do as a small standalone commit/PR. |
| **Tech debt — `pct` and `fmt` helpers duplicated** | `TaxBreakdown.tsx` and `TaxRulesView.tsx` each define their own `pct()` (and effectively `fmt()`). Extract to `features/tax/formatters.ts` when a third component needs them. |
| **Tech debt — no `AbortController` on unmount in `TaxRulesView`** | The `useEffect` fetch in `TaxRulesView.tsx` has no abort signal. Harmless in practice (the component rarely unmounts mid-fetch), but will trigger a React dev-mode warning if it does. Low priority. |
| **Tech debt — `/dev/health` page has no prod guard** | `app/dev/health/page.tsx` renders in production. Add a `NODE_ENV !== 'production'` early return (or remove the page) before Phase 6 deploy. |
| **Tech debt — `vite-tsconfig-paths` deprecation** | Vite 7 has native `resolve.tsconfigPaths: true`; `vite-tsconfig-paths` prints a deprecation warning on every test run. Replace in `vitest.config.mts` when convenient. |
| **Display polish — `TaxRulesView` shows raw enum keys** | Category thresholds list renders `GENERAL`, `SENIOR_65_PLUS` etc. instead of human-readable labels. The form dropdowns already have label maps — extract to a shared `features/tax/labels.ts` and reuse in both places. |

## 10. Tax Rules — AY 2025-26 (Bangladesh, individual)

Source: user's Excel sheet `BD_Income_Tax_Calc_2025-26.xlsx`. The calculator's rule engine should treat this entire section as **data**, not code — it must be storable as DB rows / config keyed by `assessmentYear`.

### 10.0 BD tax-year naming convention

- **Income Year (IY)** = the period income is earned, July to June. e.g., IY 2024-25 = Jul 2024 – Jun 2025.
- **Assessment Year (AY)** (aka tax year) = the year the income is filed/assessed, labeled `<start year>-<end suffix>`. Income from IY 2024-25 → AY 2025-26 (filed in calendar 2025).
- We use AY as the canonical key. Format: `"2025-26"` (matches NBR's common usage).
- **Rules can persist across multiple AYs unchanged.** Per the user, AY 2024-25 and AY 2025-26 share the same rule set (NBR did not amend the individual schedule). The data model must allow an `AssessmentYear` row to reference a shared rule set rather than forcing duplicate rows. The user's sheet tab is labeled `24-25` for this reason — it was built for AY 2024-25 and is still valid for AY 2025-26.

### 10.1 Inputs

**Income components (BDT, annual):**
basic, house rent, conveyance, medical allowance, leave encashment, performance bonus, yearly bonus, festival bonus, overtime, transportation. (Future: business income, capital gains, other.)

**Other inputs:**
- Taxpayer category (one of six — see §10.3)
- Location (for minimum tax floor — see §10.6)
- Number of physically/mentally disabled children (integer ≥ 0)
- AIT (Advance Income Tax) already paid
- Investments by category (see §10.5)

### 10.2 Salary exemption (applied before slabs)

```
taxFreeSalary = MIN( totalEarnings / 3 , 450,000 )
taxableIncome = totalEarnings − taxFreeSalary
```

This is the post-2023 reform unified cap; the old per-component exemptions (medical, house rent, conveyance) no longer apply individually.

### 10.3 Taxpayer category — first-slab (tax-free) threshold

| Category | Tax-free threshold (BDT) |
| --- | ---: |
| General | 350,000 |
| Woman | 400,000 |
| 65 years or older | 400,000 |
| Physically/Mentally disabled | 475,000 |
| Gazetted Freedom Fighter | 500,000 |
| Third Gender | 475,000 |

**Disabled-child add:** `effectiveThreshold = categoryThreshold + (50,000 × numDisabledChildren)`

### 10.4 Slab structure (widths constant across categories)

Applied on top of the effective first-slab threshold:

| # | Width (BDT) | Rate |
| ---: | ---: | ---: |
| 1 | (effective first-slab threshold) | 0% |
| 2 | 100,000 | 5% |
| 3 | 400,000 | 10% |
| 4 | 500,000 | 15% |
| 5 | 500,000 | 20% |
| 6 | 2,000,000 | 25% |
| 7 | (rest) | 30% |

Algorithm: walk slabs in order, taxing `MIN(remaining, slabWidth) × rate`, subtract from remaining, continue.

### 10.5 Investment rebate

**Eligible investments** (caps applied per-item before summing):

| Investment | Cap (BDT) |
| --- | ---: |
| Sanchay Patra | 500,000 |
| DPS | 120,000 |
| Mutual Fund | (none) |
| Treasury Bond | (none) |
| PF — Employee's Contribution | (none) |
| PF — Employer's Contribution | (none) |
| Stock | (none) |

**Rebate formula:**

```
eligibleInvestment = sum of (per-item value capped at its limit)
rebate = MIN( 0.03 × taxableIncome , 0.15 × eligibleInvestment , 1,000,000 )
```

Rebate is **zero** if `taxableIncome ≤ 0`.

### 10.6 Minimum tax floor (location-based)

After applying slabs and rebate, if the taxpayer has any taxable income but the computed tax is below the floor, bump it to the floor:

| Location | Floor (BDT) |
| --- | ---: |
| Dhaka or Chittagong City Corporation | 5,000 |
| Other City Corporation | 4,000 |
| Other (municipality / rural) | 3,000 |

```
afterRebate = MAX(0, grossTax − rebate)
withFloor = (taxableIncome > 0) ? MAX(afterRebate, minimumTaxFloor) : 0
```

### 10.7 Final tax

```
netTax = MAX(0, withFloor − AIT)
```

AIT is a withholding credit, not a rebate — modeled separately even though the user's sheet displays them together.

### 10.8 Worked example (matches user's sheet)

Input: basic = `117,000 × 13 + 90,000 = 1,611,000`; investments = Sanchay Patra 200,000 + DPS 120,000 = 320,000; category = General; AIT = 0; location = Dhaka.

| Step | Value |
| --- | ---: |
| Total earnings | 1,611,000 |
| Tax-free salary = MIN(1,611,000/3, 450,000) | 450,000 |
| Taxable income | 1,161,000 |
| Slab 1: 350,000 @ 0% | 0 |
| Slab 2: 100,000 @ 5% | 5,000 |
| Slab 3: 400,000 @ 10% | 40,000 |
| Slab 4: 311,000 @ 15% | 46,650 |
| Gross tax | 91,650 |
| Rebate = MIN(3%×1,161,000, 15%×320,000, 1M) = MIN(34,830, 48,000, 1M) | 34,830 |
| After rebate | 56,820 |
| Minimum tax floor (Dhaka, 5,000) | not binding |
| AIT credit | 0 |
| **Net tax** | **56,820** |

Matches the Excel's `K52` exactly.

### 10.9 Out of scope for MVP (track here so we don't forget)

- **Wealth surcharge** for net wealth > 4 crore BDT
- **Business / professional income** (only salary modeled in MVP)
- **Capital gains** and other special-rate incomes
- **Multi-year historical calculations** (schema supports it; we seed only AY 2025-26)
- **Tax return generation** (PDF / NBR filing format)

### 10.10 Rounding policy

Monetary values are computed at **2 decimal places (paisa), HALF_UP** — the source Excel keeps paisa precision (decided 2026-05-29). The policy is centralized in `Money` (in the `tax.service` package): `SCALE = 2`, `ROUNDING = HALF_UP`. Divisions (e.g. `totalEarnings / 3`) and rate multiplications round to this scale. If the source spreadsheet's rounding is ever found to differ (e.g. whole-taka), change it in that one place. The §10.8 worked example is all whole-taka, so it does not exercise fractional rounding — that confidence comes from the policy being explicit + centralized.

## 12. Tax Rules — AY 2026-27 (Bangladesh, individual)

Source: user (NBR AY 2026-27 individual schedule). Seeded as a **separate `RuleSet` (id=2)** referenced by `tax_assessment_year` label `'2026-27'`. The AY 2024-25 / 2025-26 rule set (id=1, §10) is unchanged and stays seeded. Same engine, same six steps (§10.2–§10.7) — only the data differs.

### 12.1 What changed vs AY 2025-26 (§10)

| Parameter | AY 2025-26 (§10) | AY 2026-27 | Changed? |
| --- | ---: | ---: | :---: |
| Salary exemption divisor / cap | 3 / 450,000 | 3 / 450,000 | no |
| General tax-free threshold | 350,000 | **375,000** | **yes** |
| Woman / Senior 65+ / Disabled / Freedom Fighter / Third Gender | 400k / 400k / 475k / 500k / 475k | same | no |
| Disabled-child bonus | +50,000 / child | +50,000 / child | no |
| Rebate: taxable-income fraction | 3% | 3% | no |
| Rebate: eligible-investment fraction | 15% | **10%** | **yes** |
| Rebate: cap | 1,000,000 | **750,000** | **yes** |
| Investment caps (Sanchay Patra 500k, DPS 120k) | as §10.5 | same | no |
| Minimum-tax floors (5k / 4k / 3k) | as §10.6 | same | no |

### 12.2 Slab structure (AY 2026-27)

The 5% band is gone and the top rate rises to 35%. The 0% band is the per-taxpayer effective first-slab threshold (§10.3, synthesized at calc time — not stored). Stored **paying** slabs (DB ordinals 1–6):

| # | Width (BDT) | Rate |
| ---: | ---: | ---: |
| 1 (0% band) | (effective first-slab threshold) | 0% |
| 2 | 300,000 | 10% |
| 3 | 400,000 | 15% |
| 4 | 500,000 | 20% |
| 5 | 425,000 | 25% |
| 6 | 2,000,000 | 30% |
| 7 | (rest) | 35% |

### 12.3 Worked example (AY 2026-27)

Same inputs as §10.8 (basic 1,611,000; Sanchay Patra 200,000 + DPS 120,000; General; Dhaka; AIT 0), under the AY 2026-27 rule set:

| Step | Value |
| --- | ---: |
| Total earnings | 1,611,000 |
| Tax-free salary = MIN(1,611,000/3, 450,000) | 450,000 |
| Taxable income | 1,161,000 |
| Effective first-slab threshold (General) | 375,000 |
| Slab 1: 375,000 @ 0% | 0 |
| Slab 2: 300,000 @ 10% | 30,000 |
| Slab 3: 400,000 @ 15% | 60,000 |
| Slab 4: 86,000 @ 20% | 17,200 |
| Gross tax | 107,200 |
| Rebate = MIN(3%×1,161,000, 10%×320,000, 750,000) = MIN(34,830, 32,000, 750,000) | 32,000 |
| After rebate | 75,200 |
| Minimum tax floor (Dhaka, 5,000) | not binding |
| AIT credit | 0 |
| **Net tax** | **75,200** |

This is the AY 2026-27 regression anchor (`TaxCalculationServiceTest`).

## 13. Zakat Rules (Bangladesh, individual)

Source of truth: the **As-Sunnah Foundation** online Zakat calculator
(`assunnahfoundation.org/zakat-calculator`), by Shaykh Ahmadullah's organization. Captured via
screenshots (the page is JS-rendered; direct fetch is blocked). Like tax, this section is **data,
not code** — the constants live in DB rows (`zakat_rule_set`) and the calculation service is a pure
function over them. Unlike tax there is **no assessment-year concept**: a single active rule set.

### 13.0 Model overview

```
netWealth     = Σ(21 zakatable asset fields) − Σ(7 deduction fields)   (floored at 0)
aboveNisab    = netWealth ≥ nisab
zakatPayable  = aboveNisab ? netWealth × rate : 0     (else "below nisab" message)
```

The arithmetic is trivial; the substance is the **field set**, the **nisab** (threshold), the
**rate** (calendar-driven), and the **gold/silver valuation** (live BAJUS price). Monetary rounding
reuses the tax policy: 2 dp, HALF_UP (a zakat-local `Money`).

### 13.1 Inputs

**Header:** `nisabBasis` (`GOLD` | `SILVER`, default `SILVER`), `calendarType` (`ENGLISH` |
`HIJRI`), `zakatYearEndDate` (informational). All money fields are BDT, `≥ 0`, default 0 (blank UI
inputs → 0).

**Assets (21)** — `gold`, `silver` (BDT selling values — see §13.4), `cashInHand`,
`foreignCurrency`, `bankDeposits` (principal excl. interest), `savingsInstruments`
(bond/debenture/T-bill at purchase price), `refundableInsurancePremium`, `optionalProvidentFund`
(excl. compulsory), `recoverableLoans` (good debt), `depositsPlacedWithOthers`,
`refundableRentSecurity`, `refundableOtherSecurity`, `businessCash`, `customerReceivables`,
`tradeStock` (ready/manufactured/raw/WIP at wholesale selling price), `tradeAssetsForResale`
(land/flats/cars held to resell), `mudarabaShareNetKnown`, `mudarabaPrincipal`,
`shareMarketCapitalGain`, `shareDividendNetKnown`, `shareDividendMarketValue`.

> The mudaraba (17/18) and share-dividend (20/21) pairs are dual-path: enter the *net zakatable
> share if known*, else the *principal / market value*. They are independent inputs — the user fills
> whichever applies; both are summed if both are provided.

**Deductions (7)** — `personalLoanDueWithinYear`, `businessLoanDueWithinYear`,
`installmentPurchasesDueWithinYear`, `unpaidDowryDueWithinYear`, `unpaidEmployeeWages`,
`payableBillsAndTaxes`, `priorYearUnpaidZakat`. Per As-Sunnah, debts count only for **the portion
payable within the next year**; prior-year unpaid Zakat is itself deductible.

### 13.2 Nisab (threshold)

As-Sunnah uses the **silver** basis only: `612.36 g silver × silver price` (their live figure was
**৳173,250** on 20/06/2026 ⇒ implied silver ≈ ৳282.9/g). **We extend this: the user picks gold or
silver**, defaulting to silver (lower threshold, the cautious/charitable choice).

```
silverNisab = 612.36 g × silverPricePerGram × resaleFactor
goldNisab   =  87.48 g × goldPricePerGram   × resaleFactor
nisab       = nisabBasis == GOLD ? goldNisab : silverNisab
```

Weights and `resaleFactor` are `zakat_rule_set` columns; the per-gram prices come from the live
BAJUS cache (§13.6). Calibrate the silver grade so `silverNisab` tracks As-Sunnah's published value.

### 13.3 Rate (calendar-driven)

The "Zakat year type" selector drives the rate:

| Calendar | Rate | Why |
| --- | ---: | --- |
| Hijri / Arabic (lunar) | **2.5%** | The canonical 1/40 on a lunar year (~354.37 d) |
| English (solar) | **2.6%** | Solar year (365.25 d) is ~3.07% longer ⇒ 2.5% × 1.031 ≈ 2.577% → 2.6% |

Stored as `rate_lunar` (0.0250) / `rate_solar` (0.0260) in `zakat_rule_set`.

### 13.4 Gold / silver valuation

The `gold` and `silver` asset fields hold a **BDT selling value**, entered two ways:

1. **Direct** — type the value (e.g. a jeweller already quoted the resale price).
2. **Price helper** — value holdings at **83% of BAJUS market price** (As-Sunnah's stated resale
   factor; BAJUS publishes retail). Per item: *quantity × carat (22/21/18/traditional)*. Weight may
   be **grams or traditional units**, converted to grams server-side:

   | Unit | Grams | Relation |
   | --- | ---: | --- |
   | Gram | 1 | — |
   | Vori (bhori/tola) | 11.664 | 1 Vori = 16 Ana = 96 Roti = 960 Point |
   | Ana | 0.729 | Vori / 16 |
   | Roti | 0.1215 | Vori / 96 |
   | Point | 0.012150 | Vori / 960 |

   `sellingValue = grams × pricePerGram × resaleFactor`, summed across items.

### 13.5 Formula

See §13.0. `netWealth` is floored at 0 (deductions exceeding assets yield 0, never negative).
`rate` per §13.3; `nisab` per §13.2. Below nisab → no Zakat due (show the "below nisab" message with
the threshold). Persisted to history when authenticated (reuse `Calculation` + `CalculatorType.ZAKAT`).

### 13.6 BAJUS price integration

Gold/silver per-gram prices are fetched from BAJUS (`bajus.org/gold-price`) and cached in
`metal_price` (one row per `metal`+`carat`, raw market price + `fetched_at`).

- BAJUS **403s default HTTP clients** → fetch with browser-like headers (User-Agent, Accept); parse
  HTML with Jsoup.
- **Daily** scheduled refresh + a startup fetch; user requests always serve from the cache (fast, no
  per-request scraping, low ban risk).
- On any fetch failure the last good rows stay; a **stale flag** (cache older than a freshness
  threshold) is surfaced in the UI ("price from {timestamp}"). An initial price snapshot is seeded
  so the app works before the first live fetch. If BAJUS ever blocks server IPs entirely, the table
  is manually updatable as a fallback.

### 13.7 Worked examples (regression anchors)

Silver nisab assumed **173,250** (per the seeded snapshot); gold examples use the gold nisab.

| Case | Inputs | Nisab | Rate | Zakat |
| --- | --- | ---: | ---: | ---: |
| Below nisab | net 150,000, silver | 173,250 | — | **0** (below-nisab message) |
| Above, solar | net 1,000,000, silver, English | 173,250 | 2.6% | **26,000** |
| Above, lunar | net 1,000,000, silver, Hijri | 173,250 | 2.5% | **25,000** |
| With deductions | assets 1,200,000 − deductions 250,000 = 950,000, silver, Hijri | 173,250 | 2.5% | **23,750** |

### 13.8 Data model

- **`zakat_rule_set`** (single row, id=1): `name`, `nisab_basis`, `nisab_silver_grams` (612.36),
  `nisab_gold_grams` (87.48), `resale_factor` (0.8300), `rate_lunar` (0.0250), `rate_solar` (0.0260).
- **`metal_price`** (cache, one row per `metal`+`carat`): `metal` (`GOLD`/`SILVER`), `carat`
  (`22`/`21`/`18`/`TRADITIONAL`), `price_per_gram` (raw BAJUS market price), `source`, `fetched_at`.

### 13.9 Scope notes

- **Divergence from As-Sunnah:** offering a **gold-nisab** option extends beyond the source (which is
  silver-only and even says the tool isn't applicable to gold-only holders). Default stays silver.
- **History deferred** to a later slice — calculation works for anonymous and logged-in users; saving
  + the account/history view come afterward.
- **Disclaimers** from As-Sunnah are reproduced in the UI: "this is an aid; verify with a scholar"
  and privacy ("no data is stored").
