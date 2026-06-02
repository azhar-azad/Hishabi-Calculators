import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { TaxCalculationResponse } from '@/features/tax/types';

function fmt(n: number): string {
  return n.toLocaleString('en-US');
}

function pct(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}

function rebateLegLabel(r: TaxCalculationResponse): string {
  const candidates = [
    { label: '3% of taxable income', value: r.taxableIncome * 0.03 },
    { label: '15% of eligible investment', value: r.eligibleInvestment * 0.15 },
    { label: 'rebate cap', value: 1_000_000 },
  ];
  return candidates.reduce((a, b) => (a.value <= b.value ? a : b)).label;
}

function Row({
               label,
               value,
               bold,
             }: {
  label: string;
  value: string;
  bold?: boolean;
}) {
  return (
    <div
      className={`flex justify-between gap-4 py-1.5 text-sm${bold ? ' font-semibold' : ''}`}
    >
      <span className="min-w-0">{label}</span>
      <span className="shrink-0 tabular-nums">{value}</span>
    </div>
  );
}

type Props = { result: TaxCalculationResponse };

export function TaxBreakdown({ result }: Props) {
  return (
    <Card
      aria-live="polite"
      data-testid="tax-result"
      className="w-full max-w-3xl"
    >
      <CardHeader>
        <CardTitle>Tax Breakdown — AY {result.assessmentYear}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="border-b border-zinc-200 pb-3 dark:border-zinc-700">
          <Row
            label="Total earnings"
            value={`${fmt(result.totalEarnings)} BDT`}
          />
          <Row
            label="Tax-free salary exemption"
            value={`(${fmt(result.taxFreeSalaryExemption)}) BDT`}
          />
          <Row
            label="Taxable income"
            value={`${fmt(result.taxableIncome)} BDT`}
            bold
          />
        </div>

        <div className="border-b border-zinc-200 pb-3 dark:border-zinc-700">
          {result.slabs
            .filter((s) => s.taxableAmountInSlab > 0)
            .map((slab) => (
              <Row
                key={slab.ordinal}
                label={`Slab ${slab.ordinal + 1}: ${fmt(slab.taxableAmountInSlab)} @ ${pct(slab.rate)}`}
                value={`${fmt(slab.tax)} BDT`}
              />
            ))}
          <Row
            label="Gross tax"
            value={`${fmt(result.grossTax)} BDT`}
            bold
          />
        </div>

        <div className="border-b border-zinc-200 pb-3 dark:border-zinc-700">
          <Row
            label={`Investment rebate (bound: ${rebateLegLabel(result)})`}
            value={`(${fmt(result.rebate)}) BDT`}
          />
          <Row
            label="After rebate"
            value={`${fmt(result.afterRebate)} BDT`}
            bold
          />
        </div>

        <div>
          {result.minimumTaxApplied ? (
            <Row
              label={`Minimum tax floor applied (${fmt(result.minimumTaxFloor)} BDT)`}
              value={`${fmt(result.taxAfterFloor)} BDT`}
            />
          ) : (
            <Row
              label={`Minimum tax floor (${fmt(result.minimumTaxFloor)} BDT)`}
              value="not binding"
            />
          )}
          {result.advanceIncomeTaxPaid > 0 && (
            <Row
              label="AIT credit"
              value={`(${fmt(result.advanceIncomeTaxPaid)}) BDT`}
            />
          )}
          <Row
            label="Net tax"
            value={`${fmt(result.netTax)} BDT`}
            bold
          />
        </div>
      </CardContent>
    </Card>
  );
}