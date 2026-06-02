import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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

  it('shows a non-negative error and clears it when corrected', async () => {
    render(<TaxCalculatorForm />);
    const basic = screen.getByLabelText('Basic');

    fireEvent.change(basic, { target: { value: '-1' } });
    fireEvent.blur(basic);

    expect(await screen.findByText(/Basic cannot be negative/i)).toBeDefined();

    fireEvent.change(basic, { target: { value: '5' } });
    await waitFor(() => {
      expect(screen.queryByText(/Basic cannot be negative/i)).toBeNull();
    });
  });

  it('shows a required error when a number field is emptied', async () => {
    render(<TaxCalculatorForm />);
    const ait = screen.getByLabelText(/Advance Income Tax/i);

    fireEvent.change(ait, { target: { value: '' } });
    fireEvent.blur(ait);

    expect(
      await screen.findByText(/Advance Income Tax is required/i),
    ).toBeDefined();
  });
});
