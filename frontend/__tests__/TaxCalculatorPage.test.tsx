import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TaxCalculatorPage from '@/app/calculators/tax/page';

describe('Tax calculator page', () => {
  it('renders the AY 2025-26 income tax heading', () => {
    render(<TaxCalculatorPage />);
    const heading = screen.getByRole('heading', {
      level: 1,
      name: /Bangladeshi Income Tax/i,
    });
    expect(heading).toBeDefined();
    expect(heading.textContent).toContain('2025-26');
  });
});
