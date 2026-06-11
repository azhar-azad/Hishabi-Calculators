'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ApiError, apiGetAuth } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import type { TaxCalculationResponse } from '@/features/tax/types';
import type { TaxFormValues } from '@/features/tax/schema';
import { Button } from '@/components/ui/button';

export const PREFILL_KEY = 'hishabi_tax_prefill';

type TaxHistoryRequest = TaxFormValues & { assessmentYear: string };

type TaxHistoryItem = {
  id: number;
  createdAt: string;
  request: TaxHistoryRequest | null;
  response: TaxCalculationResponse | null;
};

type HistoryPage = {
  content: TaxHistoryItem[];
  totalElements: number;
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

function formatBDT(amount: number): string {
  return amount.toLocaleString('en-US') + ' BDT';
}

export function TaxHistoryList() {
  const { token, isRestoring } = useAuth();
  const router = useRouter();
  const [items, setItems] = useState<TaxHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isRestoring) return;
    if (!token) {
      setLoading(false);
      router.replace('/account/login');
      return;
    }
    apiGetAuth<HistoryPage>('/api/calculators/tax/history', token)
      .then((data) => {
        setItems(data.content);
        setLoading(false);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 401) {
          router.replace('/account/login');
          return;
        }
        setError(
          e instanceof ApiError
            ? `Failed to load history (HTTP ${e.status})`
            : 'Could not reach the server.',
        );
        setLoading(false);
      });
  }, [token, isRestoring, router]);

  function openItem(item: TaxHistoryItem) {
    if (!item.request) return;
    sessionStorage.setItem(PREFILL_KEY, JSON.stringify(item.request));
    router.push('/calculators/tax');
  }

  if (isRestoring || loading) {
    return <p className="text-zinc-500">Loading…</p>;
  }

  if (error) {
    return (
      <p role="alert" className="text-destructive">
        {error}
      </p>
    );
  }

  if (items.length === 0) {
    return (
      <p className="text-zinc-500">
        No calculations saved yet.{' '}
        <a href="/calculators/tax" className="underline">
          Calculate your tax
        </a>{' '}
        to get started.
      </p>
    );
  }

  return (
    <ul className="w-full max-w-3xl space-y-3">
      {items.map((item) => (
        <li
          key={item.id}
          className="flex items-center justify-between gap-4 rounded-lg border p-4"
        >
          <div>
            <p className="font-medium">{formatDate(item.createdAt)}</p>
            {item.response ? (
              <p className="text-sm text-zinc-500">
                Income: {formatBDT(item.response.totalEarnings)} · Net tax:{' '}
                {formatBDT(item.response.netTax)}
              </p>
            ) : (
              <p className="text-sm text-zinc-400">Details unavailable</p>
            )}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => openItem(item)}
            disabled={!item.request}
          >
            Open
          </Button>
        </li>
      ))}
    </ul>
  );
}