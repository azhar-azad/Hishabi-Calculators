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

// After this delay with no response, we tell the user the backend is warming up.
const WARMING_DELAY_MS = 7_000;

export function TaxCalculator() {
  const [years, setYears] = useState<string[] | null>(null);
  const [yearsError, setYearsError] = useState(false);
  const [yearsWarming, setYearsWarming] = useState(false);
  const [yearsRetry, setYearsRetry] = useState(0);
  const [selectedYear, setSelectedYear] = useState<string | null>(null);
  const [rulesState, setRulesState] = useState<RulesState>({ kind: 'loading' });

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setYears(null);
    setYearsError(false);
    setYearsWarming(false);

    // Render free tier sleeps after inactivity; first request can take ~60 s.
    const warmTimer = setTimeout(() => {
      if (active) setYearsWarming(true);
    }, WARMING_DELAY_MS);

    apiGet<string[]>('/api/calculators/tax/years')
      .then((list) => {
        clearTimeout(warmTimer);
        if (!active) return;
        setYears(list);
        setSelectedYear((cur) => cur ?? list[0] ?? null);
      })
      .catch(() => {
        clearTimeout(warmTimer);
        if (active) setYearsError(true);
      });

    return () => {
      active = false;
      clearTimeout(warmTimer);
    };
  }, [yearsRetry]);

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
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRules(selectedYear);
  }, [selectedYear, loadRules]);

  const rules = rulesState.kind === 'ok' ? rulesState.rules : undefined;

  function renderYearSelector() {
    if (years && years.length > 0) {
      // Render the Select only once years are loaded: base-ui resolves the
      // trigger label from items present at mount.
      return (
        <Select
          value={selectedYear ?? undefined}
          onValueChange={setSelectedYear}
        >
          <SelectTrigger id="assessmentYear" className="w-full sm:w-64">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {years.map((y) => (
              <SelectItem key={y} value={y}>
                AY {y}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      );
    }

    if (yearsError) {
      return (
        <div className="flex items-center gap-2">
          <span className="text-sm text-red-600 dark:text-red-400">
            Could not load assessment years.
          </span>
          <button
            type="button"
            onClick={() => setYearsRetry((r) => r + 1)}
            className="rounded border border-zinc-300 px-2.5 py-1 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          >
            Retry
          </button>
        </div>
      );
    }

    return (
      <div
        id="assessmentYear"
        className="border-input text-muted-foreground flex h-8 w-full items-center rounded-lg border px-2.5 text-sm sm:w-64"
      >
        {yearsWarming ? 'Backend waking up…' : 'Loading years…'}
      </div>
    );
  }

  return (
    <div className="flex w-full max-w-3xl flex-col items-center gap-8">
      <div className="flex w-full flex-col gap-1">
        <Label htmlFor="assessmentYear">Assessment year</Label>
        {renderYearSelector()}
        {yearsWarming && !yearsError && years === null && (
          <p className="text-muted-foreground mt-1 text-xs">
            The backend is starting up — first load can take up to 60 seconds.
          </p>
        )}
      </div>

      <TaxCalculatorForm
        assessmentYear={selectedYear ?? undefined}
        rules={rules}
      />

      {years !== null && rulesState.kind === 'loading' && (
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
