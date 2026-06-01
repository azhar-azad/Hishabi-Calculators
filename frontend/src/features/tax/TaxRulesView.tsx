'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, apiGet } from '@/lib/api';
import type { TaxRulesResponse } from '@/features/tax/types';

const ASSESSMENT_YEAR = '2025-26';

type ViewState =
  | { kind: 'loading' }
  | { kind: 'ok'; rules: TaxRulesResponse }
  | { kind: 'error'; message: string };

const bdt = (n: number) => n.toLocaleString('en-US');
const pct = (rate: number) =>
  `${(rate * 100).toLocaleString('en-US', { maximumFractionDigits: 2 })}%`;

export function TaxRulesView() {
  const [state, setState] = useState<ViewState>({ kind: 'loading' });

  const load = useCallback(async () => {
    setState({ kind: 'loading' });
    try {
      const rules = await apiGet<TaxRulesResponse>(
        `/api/calculators/tax/rules/${ASSESSMENT_YEAR}`,
      );
      setState({ kind: 'ok', rules });
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
    // setState runs after the async fetch resolves, not during render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return (
    <section
      className="w-full max-w-3xl"
      aria-live="polite"
      aria-busy={state.kind === 'loading'}
    >
      {state.kind === 'loading' && (
        <p className="text-zinc-600 dark:text-zinc-400">Loading tax rules…</p>
      )}

      {state.kind === 'error' && (
        <div className="rounded-md border border-red-300 p-4 dark:border-red-800">
          <p className="font-medium text-red-700 dark:text-red-400">
            Error loading tax rules
          </p>
          <p className="mt-1 text-sm break-all">{state.message}</p>
          <button
            type="button"
            onClick={load}
            className="mt-3 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          >
            Retry
          </button>
        </div>
      )}

      {state.kind === 'ok' && (
        <div className="flex flex-col gap-8">
          <div>
            <h2 className="mb-3 text-xl font-semibold">Tax slabs</h2>
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-zinc-300 dark:border-zinc-700">
                  <th className="py-2 pr-4 font-medium">#</th>
                  <th className="py-2 pr-4 font-medium">Slab width (BDT)</th>
                  <th className="py-2 font-medium">Rate</th>
                </tr>
              </thead>
              <tbody>
                {state.rules.slabs.map((slab) => (
                  <tr
                    key={slab.ordinal}
                    data-testid="slab-row"
                    className="border-b border-zinc-100 dark:border-zinc-800"
                  >
                    <td className="py-2 pr-4">{slab.ordinal}</td>
                    <td className="py-2 pr-4">
                      {slab.width === null ? 'Remaining' : bdt(slab.width)}
                    </td>
                    <td className="py-2">{pct(slab.rate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div>
            <h2 className="mb-3 text-xl font-semibold">
              Tax-free thresholds by category
            </h2>
            <ul className="flex flex-col gap-1 text-sm">
              {state.rules.categoryThresholds.map((t) => (
                <li
                  key={t.category}
                  data-testid="category-row"
                  className="flex justify-between border-b border-zinc-100 py-1 dark:border-zinc-800"
                >
                  <span>{t.category}</span>
                  <span className="tabular-nums">{bdt(t.amount)} BDT</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </section>
  );
}
