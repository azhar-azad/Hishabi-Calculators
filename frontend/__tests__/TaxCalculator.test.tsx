import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TaxCalculator } from '@/features/tax/TaxCalculator';
import type {
  TaxCalculationResponse,
  TaxRulesResponse,
} from '@/features/tax/types';

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    token: null,
    user: null,
    isRestoring: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

const RULES_2026: TaxRulesResponse = {
  assessmentYear: '2026-27',
  ruleSetName: 'NBR individual schedule — AY 2026-27',
  salaryExemptionCap: 450000,
  salaryExemptionDivisor: 3,
  disabledChildThresholdBonus: 50000,
  rebateTaxableFraction: 0.03,
  rebateEligibleFraction: 0.1,
  rebateCap: 750000,
  sanchayPatraCap: 500000,
  dpsCap: 120000,
  slabs: [
    { ordinal: 1, width: 300000, rate: 0.1 },
    { ordinal: 2, width: 400000, rate: 0.15 },
    { ordinal: 3, width: 500000, rate: 0.2 },
    { ordinal: 4, width: 425000, rate: 0.25 },
    { ordinal: 5, width: 2000000, rate: 0.3 },
    { ordinal: 6, width: null, rate: 0.35 },
  ],
  categoryThresholds: [{ category: 'GENERAL', amount: 375000 }],
  minimumTaxFloors: [{ location: 'DHAKA_CHITTAGONG_CITY_CORP', amount: 5000 }],
};

const CALC_RESPONSE: TaxCalculationResponse = {
  assessmentYear: '2026-27',
  totalEarnings: 0,
  taxFreeSalaryExemption: 0,
  taxableIncome: 0,
  effectiveFirstSlabThreshold: 375000,
  slabs: [],
  grossTax: 0,
  eligibleInvestment: 0,
  rebate: 0,
  afterRebate: 0,
  minimumTaxFloor: 5000,
  minimumTaxApplied: false,
  taxAfterFloor: 0,
  advanceIncomeTaxPaid: 0,
  netTax: 0,
};

function jsonResponse(body: unknown) {
  return Promise.resolve({
    ok: true,
    status: 200,
    text: async () => JSON.stringify(body),
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaxCalculator (year selector shell)', () => {
  it('defaults to the newest year, loads its rules, and sends it in the calculate payload', async () => {
    const fetchMock = vi.fn((url: string) => {
      if (url.includes('/years')) {
        return jsonResponse(['2026-27', '2025-26', '2024-25']);
      }
      if (url.includes('/rules/')) {
        return jsonResponse(RULES_2026);
      }
      return jsonResponse(CALC_RESPONSE);
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculator />);

    // the newest year's rules are loaded (six slab rows incl. the 35% top band)
    const rows = await screen.findAllByTestId('slab-row');
    expect(rows).toHaveLength(6);
    expect(screen.getByText('35%')).toBeDefined();

    // rules were fetched for the newest year specifically
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/calculators/tax/rules/2026-27'),
      expect.anything(),
    );

    // calculating sends the selected (newest) year in the payload
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    await waitFor(() => {
      const calls = fetchMock.mock.calls as unknown as Array<
        [string, RequestInit]
      >;
      const calcCall = calls.find(([u]) => u.includes('/calculate'));
      expect(calcCall).toBeDefined();
      expect(String(calcCall![1].body)).toContain('"assessmentYear":"2026-27"');
    });
  });
});
