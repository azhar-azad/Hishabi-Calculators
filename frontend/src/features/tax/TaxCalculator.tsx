'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, apiGet } from '@/lib/api';
import type { TaxRulesResponse } from '@/features/tax/types';
import { TaxCalculatorForm } from '@/features/tax/TaxCalculatorForm';
import { TaxRulesView } from '@/features/tax/TaxRulesView';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

type RulesState =
  | { kind: 'loading' }
  | { kind: 'ok'; rules: TaxRulesResponse }
  | { kind: 'error'; message: string };

/**
 * Shell for the tax calculator: loads the available assessment years, lets the
 * user pick one, fetches that year's rule set once, and threads it down to both
 * the form (for the calculate payload + rebate-label config) and the rules view
 * (for display). Keeping the single rules fetch here avoids the form and the
 * rules view each fetching the same data.
 */
export function TaxCalculator() {
  const [years, setYears] = useState<string[] | null>(null);
  const [selectedYear, setSelectedYear] = useState<string | null>(null);
  const [rulesState, setRulesState] = useState<RulesState>({ kind: 'loading' });

  // Load available assessment years once; default to the newest (first).
  useEffect(() => {
    let active = true;
    apiGet<string[]>('/api/calculators/tax/years')
      .then((list) => {
        if (!active) return;
        setYears(list);
        setSelectedYear((cur) => cur ?? list[0] ?? null);
      })
      .catch(() => {
        if (active) setYears([]);
      });
    return () => {
      active = false;
    };
  }, []);

  const loadRules = useCallback(async (year: string) => {
    setRulesState({ kind: 'loading' });
    try {
      const rules = await apiGet<TaxRulesResponse>(
        `/api/calculators/tax/rules/${year}`,
      );
      setRulesState({ kind: 'ok', rules });
    } catch (e) {
      const message =
        e instanceof ApiError
          ? `HTTP ${e.status} — ${JSON.stringify(e.body)}`
          : e instanceof Error
            ? e.message
            : 'Unknown error';
      setRulesState({ kind: 'error', message });
    }
  }, []);

  useEffect(() => {
    if (!selectedYear) return;
    // setState runs after the async fetch resolves, not during render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRules(selectedYear);
  }, [selectedYear, loadRules]);

  const rules = rulesState.kind === 'ok' ? rulesState.rules : undefined;

  return (
    <div className="flex w-full max-w-3xl flex-col items-center gap-8">
      <div className="flex w-full flex-col gap-1">
        <Label htmlFor="assessmentYear">Assessment year</Label>
        <Select
          value={selectedYear ?? undefined}
          onValueChange={setSelectedYear}
          disabled={!years || years.length === 0}
        >
          <SelectTrigger id="assessmentYear" className="w-full sm:w-64">
            <SelectValue placeholder="Loading years…" />
          </SelectTrigger>
          <SelectContent>
            {(years ?? []).map((y) => (
              <SelectItem key={y} value={y}>
                AY {y}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <TaxCalculatorForm
        assessmentYear={selectedYear ?? undefined}
        rules={rules}
      />

      {rulesState.kind === 'loading' && (
        <p className="text-zinc-600 dark:text-zinc-400">Loading tax rules…</p>
      )}

      {rulesState.kind === 'error' && (
        <div className="w-full rounded-md border border-red-300 p-4 dark:border-red-800">
          <p className="font-medium text-red-700 dark:text-red-400">
            Error loading tax rules
          </p>
          <p className="mt-1 text-sm break-all">{rulesState.message}</p>
          <button
            type="button"
            onClick={() => selectedYear && loadRules(selectedYear)}
            className="mt-3 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          >
            Retry
          </button>
        </div>
      )}

      {rules && <TaxRulesView rules={rules} />}
    </div>
  );
}
