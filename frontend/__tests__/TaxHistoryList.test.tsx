import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TaxHistoryList } from '@/features/tax/TaxHistoryList';
import type { TaxCalculationResponse } from '@/features/tax/types';

const mockPush = vi.fn();
const mockReplace = vi.fn();

// Return a stable router object so `useEffect([..., router])` doesn't re-run
// on every re-render. A new object on each useRouter() call would cause the
// effect to fire twice, making the second fetch reuse an already-consumed
// Response body and crash the component into the error state.
const stableRouter = { push: mockPush, replace: mockReplace };

vi.mock('next/navigation', () => ({
  useRouter: () => stableRouter,
}));

let currentToken: string | null = 'test-token';

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    token: currentToken,
    user: currentToken ? { email: 'user@example.com' } : null,
    isRestoring: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

const SAMPLE_RESPONSE: TaxCalculationResponse = {
  assessmentYear: '2025-26',
  totalEarnings: 600_000,
  taxFreeSalaryExemption: 150_000,
  taxableIncome: 450_000,
  effectiveFirstSlabThreshold: 350_000,
  slabs: [],
  grossTax: 20_000,
  eligibleInvestment: 0,
  rebate: 0,
  afterRebate: 20_000,
  minimumTaxFloor: 5_000,
  minimumTaxApplied: false,
  taxAfterFloor: 20_000,
  advanceIncomeTaxPaid: 0,
  netTax: 20_000,
};

const SAMPLE_REQUEST = {
  assessmentYear: '2025-26',
  category: 'GENERAL',
  location: 'DHAKA_CHITTAGONG_CITY_CORP',
  disabledChildren: 0,
  income: {
    basic: 600_000,
    houseRent: 0,
    conveyance: 0,
    medicalAllowance: 0,
    leaveEncashment: 0,
    performanceBonus: 0,
    yearlyBonus: 0,
    festivalBonus: 0,
    overtime: 0,
    transportation: 0,
  },
  investments: {
    sanchayPatra: 0,
    dps: 0,
    mutualFund: 0,
    treasuryBond: 0,
    providentFundEmployee: 0,
    providentFundEmployer: 0,
    stock: 0,
  },
  advanceIncomeTaxPaid: 0,
};

const HISTORY_PAGE = {
  content: [
    {
      id: 1,
      createdAt: '2025-06-01T10:00:00Z',
      request: SAMPLE_REQUEST,
      response: SAMPLE_RESPONSE,
    },
  ],
  totalElements: 1,
};

function makeFetchMock(body: unknown, status = 200) {
  return vi.fn().mockImplementation(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  );
}

beforeEach(() => {
  currentToken = 'test-token';
  mockPush.mockClear();
  mockReplace.mockClear();
  sessionStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaxHistoryList', () => {
  it('renders a list of history items from the API', async () => {
    vi.stubGlobal('fetch', makeFetchMock(HISTORY_PAGE));

    render(<TaxHistoryList />);

    expect(await screen.findByText(/1 Jun 2025/)).toBeDefined();
    expect(screen.getByText(/600,000/)).toBeDefined();
    expect(screen.getByText(/20,000/)).toBeDefined();
    expect(screen.getByRole('button', { name: /Open/i })).toBeDefined();
  });

  it('shows empty state when no calculations have been saved', async () => {
    vi.stubGlobal(
      'fetch',
      makeFetchMock({ content: [], totalElements: 0 }),
    );

    render(<TaxHistoryList />);

    expect(await screen.findByText(/No calculations saved/)).toBeDefined();
  });

  it('redirects to login when not authenticated', async () => {
    currentToken = null;

    render(<TaxHistoryList />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/account/login');
    });
  });

  it('clicking Open stores prefill in sessionStorage and navigates to tax page', async () => {
    vi.stubGlobal('fetch', makeFetchMock(HISTORY_PAGE));

    render(<TaxHistoryList />);
    fireEvent.click(await screen.findByRole('button', { name: /Open/i }));

    expect(sessionStorage.getItem('hishabi_tax_prefill')).toBe(
      JSON.stringify(SAMPLE_REQUEST),
    );
    expect(mockPush).toHaveBeenCalledWith('/calculators/tax');
  });
});
