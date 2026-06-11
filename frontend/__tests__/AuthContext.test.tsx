import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import {
  AuthProvider,
  useAuth,
  TOKEN_KEY,
  EMAIL_KEY,
} from '@/context/AuthContext';

function AuthConsumer() {
  const { user, token, isRestoring, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="email">{user?.email ?? 'none'}</span>
      <span data-testid="token">{token ?? 'none'}</span>
      <span data-testid="restoring">{String(isRestoring)}</span>
      <button onClick={() => login('a@b.com', 'tok123')}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  it('starts with no user and no token', () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );
    expect(screen.getByTestId('email').textContent).toBe('none');
    expect(screen.getByTestId('token').textContent).toBe('none');
  });

  it('login stores user and token in state and localStorage', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^login$/i }));
    });

    expect(screen.getByTestId('email').textContent).toBe('a@b.com');
    expect(screen.getByTestId('token').textContent).toBe('tok123');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('tok123');
    expect(localStorage.getItem(EMAIL_KEY)).toBe('a@b.com');
  });

  it('logout clears user, token, and localStorage', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^login$/i }));
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^logout$/i }));
    });

    expect(screen.getByTestId('email').textContent).toBe('none');
    expect(screen.getByTestId('token').textContent).toBe('none');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(EMAIL_KEY)).toBeNull();
  });

  it('restores session from localStorage on mount', async () => {
    localStorage.setItem(TOKEN_KEY, 'persisted-tok');
    localStorage.setItem(EMAIL_KEY, 'persisted@example.com');

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    // Flush useEffect
    await act(async () => {});

    expect(screen.getByTestId('email').textContent).toBe(
      'persisted@example.com',
    );
    expect(screen.getByTestId('token').textContent).toBe('persisted-tok');
  });

  it('isRestoring is false after the first effect completes', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await act(async () => {});

    expect(screen.getByTestId('restoring').textContent).toBe('false');
  });
});
