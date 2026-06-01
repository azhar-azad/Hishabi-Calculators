import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import TaxCalculatorPage from '@/app/calculators/tax/page';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('Tax calculator page', () => {
  it('renders the AY 2025-26 income tax heading', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: async () =>
          JSON.stringify({
            slabs: [],
            categoryThresholds: [],
            minimumTaxFloors: [],
          }),
      }),
    );

    render(<TaxCalculatorPage />);

    const heading = await screen.findByRole('heading', {
      level: 1,
      name: /Bangladeshi Income Tax/i,
    });
    expect(heading).toBeDefined();
    expect(heading.textContent).toContain('2025-26');
  });
});
