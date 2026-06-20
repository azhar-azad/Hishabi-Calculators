import type { Metadata } from 'next';
import { TaxCalculator } from '@/features/tax/TaxCalculator';

export const metadata: Metadata = {
  title: 'Bangladeshi Income Tax',
};

export default function TaxCalculatorPage() {
  return (
    <main className="flex flex-1 flex-col items-center gap-8 p-4 sm:p-8">
      <header className="w-full max-w-3xl">
        <h1 className="text-3xl font-bold tracking-tight">
          Bangladeshi Income Tax
        </h1>
        <p className="mt-2 text-zinc-600 dark:text-zinc-400">
          Estimate your individual income tax. Pick an assessment year below.
        </p>
      </header>
      <TaxCalculator />
    </main>
  );
}
