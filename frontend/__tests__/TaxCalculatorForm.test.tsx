import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TaxCalculatorForm } from '@/features/tax/TaxCalculatorForm';

describe('Tax calculator form', () => {
  it('renders income, investment, and other fields with sensible defaults', () => {
    render(<TaxCalculatorForm />);

    // representative number fields default to 0
    expect((screen.getByLabelText('Basic') as HTMLInputElement).value).toBe(
      '0',
    );
    expect(
      (screen.getByLabelText('Sanchay Patra') as HTMLInputElement).value,
    ).toBe('0');
    expect(
      (screen.getByLabelText(/Advance Income Tax/i) as HTMLInputElement).value,
    ).toBe('0');
    expect(
      (screen.getByLabelText(/disabled children/i) as HTMLInputElement).value,
    ).toBe('0');

    // dropdowns present
    expect(screen.getByLabelText('Taxpayer category')).toBeDefined();
    expect(screen.getByLabelText('Location')).toBeDefined();

    // all income + investment fields rendered
    expect(screen.getByLabelText('Transportation')).toBeDefined();
    expect(screen.getByLabelText('Stock')).toBeDefined();

    // submit button present (no submit logic yet)
    expect(screen.getByRole('button', { name: /Calculate/i })).toBeDefined();
  });
});
