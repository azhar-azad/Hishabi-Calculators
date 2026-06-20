import type { TaxRulesResponse } from '@/features/tax/types';

const bdt = (n: number) => n.toLocaleString('en-US');
const pct = (rate: number) =>
  `${(rate * 100).toLocaleString('en-US', { maximumFractionDigits: 2 })}%`;

type Props = { rules: TaxRulesResponse };

export function TaxRulesView({ rules }: Props) {
  return (
    <section className="w-full max-w-3xl">
      <div className="flex flex-col gap-8">
        <div>
          <h2 className="mb-3 text-xl font-semibold">Tax slabs</h2>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-zinc-300 dark:border-zinc-700">
                  <th className="py-2 pr-4 font-medium">#</th>
                  <th className="py-2 pr-4 font-medium">Slab width (BDT)</th>
                  <th className="py-2 font-medium">Rate</th>
                </tr>
              </thead>
              <tbody>
                {rules.slabs.map((slab) => (
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
        </div>

        <div>
          <h2 className="mb-3 text-xl font-semibold">
            Tax-free thresholds by category
          </h2>
          <ul className="flex flex-col gap-1 text-sm">
            {rules.categoryThresholds.map((t) => (
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
    </section>
  );
}
