import type { Metadata } from 'next';
import { TaxHistoryList } from '@/features/tax/TaxHistoryList';

export const metadata: Metadata = {
  title: 'Calculation history — Hishabi',
};

export default function HistoryPage() {
  return (
    <main className="flex flex-1 flex-col items-center gap-8 p-4 sm:p-8">
      <header className="w-full max-w-3xl">
        <h1 className="text-3xl font-bold tracking-tight">
          Calculation history
        </h1>
        <p className="mt-2 text-zinc-600 dark:text-zinc-400">
          Your saved income tax calculations.
        </p>
      </header>
      <TaxHistoryList />
    </main>
  );
}
