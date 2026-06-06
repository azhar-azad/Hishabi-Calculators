'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from 'react';

interface AuthUser {
  email: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  /** True during the first render while localStorage is being read. Use to suppress redirects before the session is known. */
  isRestoring: boolean;
  login: (email: string, token: string) => void;
  logout: () => void;
}

export const TOKEN_KEY = 'hishabi_token';
export const EMAIL_KEY = 'hishabi_email';

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isRestoring, setIsRestoring] = useState(true);

  // Restore session from localStorage on first client render
  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    const storedEmail = localStorage.getItem(EMAIL_KEY);
    if (storedToken && storedEmail) {
      // TODO: decode the JWT exp claim and skip restore if expired.
      // e.g. const { exp } = JSON.parse(atob(storedToken.split('.')[1]));
      //      if (Date.now() >= exp * 1000) { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(EMAIL_KEY); }
      // else { setToken(storedToken); setUser({ email: storedEmail }); }
      setToken(storedToken);
      setUser({ email: storedEmail });
    }
    setIsRestoring(false);
  }, []);

  function login(email: string, newToken: string) {
    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(EMAIL_KEY, email);
    setToken(newToken);
    setUser({ email });
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, token, isRestoring, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}