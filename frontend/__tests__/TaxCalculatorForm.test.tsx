import { afterEach, describe, expect, it, vi } from 'vitest';
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

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('submits and shows the net tax on success', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ netTax: 56820 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    expect(await screen.findByText(/56,820/)).toBeDefined();
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/calculators/tax/calculate'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('surfaces a server error on failure', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(
          JSON.stringify({ message: 'Unknown assessment year: 2025-26' }),
          { status: 404 },
        ),
      );
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    expect(await screen.findByRole('alert')).toBeDefined();
    expect(await screen.findByText(/Unknown assessment year/i)).toBeDefined();
  });
});
