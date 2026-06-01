import type { Metadata } from 'next';
import { TaxRulesView } from '@/features/tax/TaxRulesView';

export const metadata: Metadata = {
  title: 'Bangladeshi Income Tax - AY 2025-26',
};

export default function TaxCalculatorPage() {
  return (
    <main className="flex flex-1 flex-col items-center gap-8 p-8">
      <header className="w-full max-w-3xl">
        <h1 className="text-3xl font-bold tracking-tight">
          Bangladeshi Income Tax - AY 2025-26
        </h1>
        <p className="mt-2 text-zinc-600 dark:text-zinc-400">
          Estimate your individual income tax. Rules and form coming next.
        </p>
      </header>
      <TaxRulesView />
    </main>
  );
}
