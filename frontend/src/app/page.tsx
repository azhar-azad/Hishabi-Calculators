import Link from 'next/link';

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-12 p-8">
      <header className="text-center">
        <h1 className="text-5xl font-bold tracking-tight">Hishabi</h1>
        <p className="mt-4 text-lg text-zinc-600 dark:text-zinc-400">
          Calculators for Bangladeshi finance &amp; life
        </p>
      </header>

      <section className="w-full max-w-md">
        <h2 className="mb-4 text-2xl font-semibold">Calculators</h2>
        <ul className="space-y-3">
          <li>
            <Link
              href="/calculators/tax"
              className="block rounded-md border border-zinc-200 p-4 transition-colors hover:bg-zinc-50 dark:border-zinc-800 dark:hover:bg-zinc-900"
            >
              <div className="font-medium">Income Tax</div>
              <div className="text-sm text-zinc-600 dark:text-zinc-400">
                Bangladeshi individual income tax - AY 2025-26
              </div>
            </Link>
          </li>
          <li className="rounded-md border border-zinc-200 p-4 opacity-60 dark:border-zinc-800">
            <div className="font-medium">Zakat</div>
            <div className="text-sm text-zinc-600 dark:text-zinc-400">
              Coming soon
            </div>
          </li>
        </ul>
      </section>
    </main>
  );
}
