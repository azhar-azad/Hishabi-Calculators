import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TaxBreakdown } from '@/features/tax/TaxBreakdown';
import type { TaxCalculationResponse } from '@/features/tax/types';

// AY 2026-27 worked example (PLAN.md §12.3).
const result: TaxCalculationResponse = {
  assessmentYear: '2026-27',
  totalEarnings: 1_611_000,
  taxFreeSalaryExemption: 450_000,
  taxableIncome: 1_161_000,
  effectiveFirstSlabThreshold: 375_000,
  slabs: [
    { ordinal: 0, rate: 0, taxableAmountInSlab: 375_000, tax: 0 },
    { ordinal: 1, rate: 0.1, taxableAmountInSlab: 300_000, tax: 30_000 },
    { ordinal: 2, rate: 0.15, taxableAmountInSlab: 400_000, tax: 60_000 },
    { ordinal: 3, rate: 0.2, taxableAmountInSlab: 86_000, tax: 17_200 },
  ],
  grossTax: 107_200,
  eligibleInvestment: 320_000,
  rebate: 32_000,
  afterRebate: 75_200,
  minimumTaxFloor: 5_000,
  minimumTaxApplied: false,
  taxAfterFloor: 75_200,
  advanceIncomeTaxPaid: 0,
  netTax: 75_200,
};

describe('TaxBreakdown rebate label', () => {
  it('labels the binding leg using the supplied AY 2026-27 rebate config', () => {
    render(
      <TaxBreakdown
        result={result}
        rebateConfig={{
          taxableFraction: 0.03,
          eligibleFraction: 0.1,
          cap: 750_000,
        }}
      />,
    );
    // 10% × 320,000 = 32,000 is the smallest leg → it binds
    expect(
      screen.getByText(
        /Investment rebate \(bound: 10% of eligible investment\)/,
      ),
    ).toBeDefined();
  });

  it('falls back to AY 2025-26 fractions when no config is supplied', () => {
    render(<TaxBreakdown result={result} />);
    // fallback 15%/3%/1M: 3% × 1,161,000 = 34,830 < 15% × 320,000 = 48,000 → 3% leg binds
    expect(
      screen.getByText(/Investment rebate \(bound: 3% of taxable income\)/),
    ).toBeDefined();
  });
});
