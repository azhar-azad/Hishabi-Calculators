import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TaxRulesView } from '@/features/tax/TaxRulesView';
import type { TaxRulesResponse } from '@/features/tax/types';

const rules: TaxRulesResponse = {
  assessmentYear: '2025-26',
  ruleSetName: 'NBR individual schedule',
  salaryExemptionCap: 450000,
  salaryExemptionDivisor: 3,
  disabledChildThresholdBonus: 50000,
  rebateTaxableFraction: 0.03,
  rebateEligibleFraction: 0.15,
  rebateCap: 1000000,
  sanchayPatraCap: 500000,
  dpsCap: 120000,
  slabs: [
    { ordinal: 1, width: 100000, rate: 0.05 },
    { ordinal: 2, width: 400000, rate: 0.1 },
    { ordinal: 3, width: 500000, rate: 0.15 },
    { ordinal: 4, width: 500000, rate: 0.2 },
    { ordinal: 5, width: 2000000, rate: 0.25 },
    { ordinal: 6, width: null, rate: 0.3 },
  ],
  categoryThresholds: [
    { category: 'GENERAL', amount: 350000 },
    { category: 'WOMAN', amount: 400000 },
  ],
  minimumTaxFloors: [{ location: 'DHAKA_CHITTAGONG_CITY_CORP', amount: 5000 }],
};

describe('TaxRulesView', () => {
  it('renders one row per slab from the supplied rules', () => {
    render(<TaxRulesView rules={rules} />);

    const rows = screen.getAllByTestId('slab-row');
    expect(rows).toHaveLength(6);
    expect(screen.getByText('Remaining')).toBeDefined();
    expect(screen.getByText('30%')).toBeDefined();
  });

  it('renders the category thresholds', () => {
    render(<TaxRulesView rules={rules} />);

    expect(screen.getByText('GENERAL')).toBeDefined();
    expect(screen.getByText('350,000 BDT')).toBeDefined();
  });
});
