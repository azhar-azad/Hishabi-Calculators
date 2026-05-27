'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, apiGet } from '@/lib/api';

type HealthResponse = {
  status: string;
};

type ProbeState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: HealthResponse }
  | { kind: 'error'; message: string };

export default function DevHealthPage() {
  const [state, setState] = useState<ProbeState>({ kind: 'loading' });

  const probe = useCallback(async () => {
    setState({ kind: 'loading' });
    try {
      const data = await apiGet<HealthResponse>('/api/health');
      setState({ kind: 'ok', data });
    } catch (e) {
      const message =
        e instanceof ApiError
          ? `HTTP ${e.status} — ${JSON.stringify(e.body)}`
          : e instanceof Error
            ? e.message
            : 'Unknown error';
      setState({ kind: 'error', message });
    }
  }, []);

  useEffect(() => {
    // Fetch on mount. The setState calls inside `probe` happen after the
    // async fetch resolves, not synchronously — so there's no render
    // cascade. The lint rule's static analysis can't see that.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    probe();
  }, [probe]);

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8">
      <header className="text-center">
        <h1 className="text-3xl font-bold">Backend health probe</h1>
        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">
          Dev-only — calls{' '}
          <code className="rounded bg-zinc-100 px-1 py-0.5 dark:bg-zinc-800">
            GET /api/health
          </code>{' '}
          on the configured backend.
        </p>
      </header>

      <section
        className="w-full max-w-md rounded-md border border-zinc-200 p-6 dark:border-zinc-800"
        aria-live="polite"
      >
        {state.kind === 'loading' && <p>Calling backend…</p>}
        {state.kind === 'ok' && (
          <div>
            <p className="font-medium text-green-700 dark:text-green-400">OK</p>
            <pre className="mt-2 overflow-auto rounded bg-zinc-50 p-2 text-sm dark:bg-zinc-900">
              {JSON.stringify(state.data, null, 2)}
            </pre>
          </div>
        )}
        {state.kind === 'error' && (
          <div>
            <p className="font-medium text-red-700 dark:text-red-400">Error</p>
            <p className="mt-2 text-sm break-all">{state.message}</p>
          </div>
        )}
      </section>

      <button
        type="button"
        onClick={probe}
        disabled={state.kind === 'loading'}
        className="rounded-md border border-zinc-300 bg-zinc-50 px-4 py-2 text-sm font-medium transition-colors hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:bg-zinc-900 dark:hover:bg-zinc-800"
      >
        Re-probe
      </button>
    </main>
  );
}
