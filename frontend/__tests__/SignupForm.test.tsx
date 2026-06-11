import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
// import { SignupForm } from './SignupForm';
import { SignupForm } from '@/features/auth/SignupForm';
import * as api from '@/lib/api';

const mockPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock('@/lib/api', () => ({
  apiPost: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    body: unknown;
    constructor(status: number, body: unknown) {
      super(`HTTP ${status}`);
      this.name = 'ApiError';
      this.status = status;
      this.body = body;
    }
  },
}));

describe('SignupForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders email, password fields and a submit button', () => {
    render(<SignupForm />);
    expect(screen.getByLabelText(/email/i)).toBeTruthy();
    expect(screen.getByLabelText(/password/i)).toBeTruthy();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeTruthy();
  });

  it('redirects to login on successful signup', async () => {
    vi.mocked(api.apiPost).mockResolvedValueOnce({ userId: 1 });

    render(<SignupForm />);
    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'Password1' },
    });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));

    await waitFor(() => {
      expect(api.apiPost).toHaveBeenCalledWith('/api/auth/signup', {
        email: 'user@example.com',
        password: 'Password1',
      });
      expect(mockPush).toHaveBeenCalledWith('/account/login');
    });
  });

  it('shows an error on duplicate email (409)', async () => {
    vi.mocked(api.apiPost).mockRejectedValueOnce(new api.ApiError(409, {}));

    render(<SignupForm />);
    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: 'taken@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'Password1' },
    });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));

    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(alert.textContent).toMatch(/already exists/i);
    });
  });
});
