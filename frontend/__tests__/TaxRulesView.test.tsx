import { describe, it, expect, vi, afterEach } from 'vitest';
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

function stubFetch(payload: unknown, ok = true, status = 200) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok,
      status,
      text: async () => JSON.stringify(payload),
    }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaxRulesView', () => {
  it('renders the six slab rows after a successful fetch', async () => {
    stubFetch(rules);
    render(<TaxRulesView />);

    expect(screen.getByText(/loading tax rules/i)).toBeDefined();

    const rows = await screen.findAllByTestId('slab-row');
    expect(rows).toHaveLength(6);
    expect(screen.getByText('Remaining')).toBeDefined();
    expect(screen.getByText('30%')).toBeDefined();
    expect(screen.getByText('GENERAL')).toBeDefined();
  });

  it('shows an error state when the fetch fails', async () => {
    stubFetch({ message: 'boom' }, false, 500);
    render(<TaxRulesView />);

    expect(await screen.findByText(/error loading tax rules/i)).toBeDefined();
  });
});
