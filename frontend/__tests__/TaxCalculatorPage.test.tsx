import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import TaxCalculatorPage from '@/app/calculators/tax/page';

afterEach(() => {
  vi.unstubAllGlobals();
});

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    token: null,
    user: null,
    isRestoring: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

// The page renders the TaxCalculator wrapper, which fetches the year list and
// then the rule set for the selected year.
function stubApi() {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      const body = url.includes('/years')
        ? ['2026-27', '2025-26', '2024-25']
        : { slabs: [], categoryThresholds: [], minimumTaxFloors: [] };
      return Promise.resolve({
        ok: true,
        status: 200,
        text: async () => JSON.stringify(body),
      });
    }),
  );
}

describe('Tax calculator page', () => {
  it('renders the income tax heading and the calculator', async () => {
    stubApi();

    render(<TaxCalculatorPage />);

    const heading = await screen.findByRole('heading', {
      level: 1,
      name: /Bangladeshi Income Tax/i,
    });
    expect(heading).toBeDefined();
    // the form is wired into the page
    expect(screen.getByRole('button', { name: /Calculate/i })).toBeDefined();
  });
});
