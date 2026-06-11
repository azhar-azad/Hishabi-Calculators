import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TaxCalculatorForm } from '@/features/tax/TaxCalculatorForm';
import type { TaxCalculationResponse } from '@/features/tax/types';

const WORKED_EXAMPLE: TaxCalculationResponse = {
  assessmentYear: '2025-26',
  totalEarnings: 1_611_000,
  taxFreeSalaryExemption: 450_000,
  taxableIncome: 1_161_000,
  effectiveFirstSlabThreshold: 350_000,
  slabs: [
    { ordinal: 0, rate: 0.0, taxableAmountInSlab: 350_000, tax: 0 },
    { ordinal: 1, rate: 0.05, taxableAmountInSlab: 100_000, tax: 5_000 },
    { ordinal: 2, rate: 0.1, taxableAmountInSlab: 400_000, tax: 40_000 },
    { ordinal: 3, rate: 0.15, taxableAmountInSlab: 311_000, tax: 46_650 },
  ],
  grossTax: 91_650,
  eligibleInvestment: 320_000,
  rebate: 34_830,
  afterRebate: 56_820,
  minimumTaxFloor: 5_000,
  minimumTaxApplied: false,
  taxAfterFloor: 56_820,
  advanceIncomeTaxPaid: 0,
  netTax: 56_820,
};

// Mutable — tests set this before render to simulate auth state.
let currentToken: string | null = null;

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    token: currentToken,
    user: currentToken ? { email: 'user@example.com' } : null,
    isRestoring: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe('Tax calculator form', () => {
  beforeEach(() => {
    currentToken = null; // default: logged out
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

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

  it('submits and shows the net tax on success', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(WORKED_EXAMPLE), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    expect(
      (await screen.findAllByText(/56,820/)).length,
    ).toBeGreaterThanOrEqual(1);
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

  it('renders full breakdown for worked example (§10.8)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(WORKED_EXAMPLE), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    // income section
    expect(await screen.findByText('Taxable income')).toBeDefined();
    expect(screen.getByText('1,161,000 BDT')).toBeDefined();

    // slab walk
    expect(screen.getByText('Slab 2: 100,000 @ 5%')).toBeDefined();
    expect(screen.getByText('5,000 BDT')).toBeDefined();
    expect(screen.getByText('Slab 3: 400,000 @ 10%')).toBeDefined();
    expect(screen.getByText('Slab 4: 311,000 @ 15%')).toBeDefined();
    expect(screen.getByText('Gross tax')).toBeDefined();
    expect(screen.getByText('91,650 BDT')).toBeDefined();

    // rebate — leg 1 (3% of taxable) is binding for this example
    expect(
      screen.getByText(/Investment rebate \(bound: 3% of taxable income\)/),
    ).toBeDefined();
    expect(screen.getByText('(34,830) BDT')).toBeDefined();

    // minimum tax floor not binding
    expect(screen.getByText('not binding')).toBeDefined();

    // net tax (appears as "56,820 BDT" in both after-rebate and net-tax rows)
    expect(screen.getAllByText('56,820 BDT').length).toBeGreaterThanOrEqual(2);
  });

  it('shows no save indicator when logged out after a successful calculation', async () => {
    // currentToken is null (set in beforeEach)
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(WORKED_EXAMPLE), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    // Wait for the result to appear (confirms the response was processed)
    await screen.findAllByText(/56,820/);
    expect(screen.queryByTestId('save-indicator')).toBeNull();
  });

  it('shows save indicator and sends auth header when logged in', async () => {
    currentToken = 'test-tok';
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(WORKED_EXAMPLE), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    render(<TaxCalculatorForm />);
    fireEvent.click(screen.getByRole('button', { name: /Calculate/i }));

    expect(await screen.findByTestId('save-indicator')).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/calculators/tax/calculate'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-tok',
        }),
      }),
    );
  });
});
