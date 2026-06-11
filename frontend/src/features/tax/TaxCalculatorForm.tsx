'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { ApiError, apiPost, apiPostAuth } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import type { TaxCalculationResponse } from '@/features/tax/types';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Controller,
  useForm,
  type FieldErrors,
  type FieldPath,
} from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { taxFormSchema, type TaxFormValues } from '@/features/tax/schema';
import { TaxBreakdown } from '@/features/tax/TaxBreakdown';
import { PREFILL_KEY } from '@/features/tax/TaxHistoryList';

type NumberField = {
  name: FieldPath<TaxFormValues>;
  label: string;
  step?: string;
};

const INCOME_FIELDS: NumberField[] = [
  { name: 'income.basic', label: 'Basic' },
  { name: 'income.houseRent', label: 'House rent' },
  { name: 'income.conveyance', label: 'Conveyance' },
  { name: 'income.medicalAllowance', label: 'Medical allowance' },
  { name: 'income.leaveEncashment', label: 'Leave encashment' },
  { name: 'income.performanceBonus', label: 'Performance bonus' },
  { name: 'income.yearlyBonus', label: 'Yearly bonus' },
  { name: 'income.festivalBonus', label: 'Festival bonus' },
  { name: 'income.overtime', label: 'Overtime' },
  { name: 'income.transportation', label: 'Transportation' },
];

const INVESTMENT_FIELDS: NumberField[] = [
  { name: 'investments.sanchayPatra', label: 'Sanchay Patra' },
  { name: 'investments.dps', label: 'DPS' },
  { name: 'investments.mutualFund', label: 'Mutual Fund' },
  { name: 'investments.treasuryBond', label: 'Treasury Bond' },
  {
    name: 'investments.providentFundEmployee',
    label: 'Provident Fund (employee)',
  },
  {
    name: 'investments.providentFundEmployer',
    label: 'Provident Fund (employer)',
  },
  { name: 'investments.stock', label: 'Stock' },
];

const CATEGORY_OPTIONS = [
  { value: 'GENERAL', label: 'General' },
  { value: 'WOMAN', label: 'Woman' },
  { value: 'SENIOR_65_PLUS', label: 'Senior (65+)' },
  {
    value: 'PHYSICALLY_MENTALLY_DISABLED',
    label: 'Physically/Mentally disabled',
  },
  { value: 'GAZETTED_FREEDOM_FIGHTER', label: 'Gazetted freedom fighter' },
  { value: 'THIRD_GENDER', label: 'Third gender' },
];

const LOCATION_OPTIONS = [
  {
    value: 'DHAKA_CHITTAGONG_CITY_CORP',
    label: 'Dhaka / Chittagong City Corporation',
  },
  { value: 'OTHER_CITY_CORP', label: 'Other City Corporation' },
  { value: 'OTHER', label: 'Other (municipality / rural)' },
];

const ASSESSMENT_YEAR = '2025-26';

const DEFAULT_VALUES: TaxFormValues = {
  category: 'GENERAL',
  location: 'DHAKA_CHITTAGONG_CITY_CORP',
  disabledChildren: 0,
  income: {
    basic: 0,
    houseRent: 0,
    conveyance: 0,
    medicalAllowance: 0,
    leaveEncashment: 0,
    performanceBonus: 0,
    yearlyBonus: 0,
    festivalBonus: 0,
    overtime: 0,
    transportation: 0,
  },
  investments: {
    sanchayPatra: 0,
    dps: 0,
    mutualFund: 0,
    treasuryBond: 0,
    providentFundEmployee: 0,
    providentFundEmployer: 0,
    stock: 0,
  },
  advanceIncomeTaxPaid: 0,
};

// Read a nested error message by dotted field path (e.g. "income.basic").
function errorAt(
  errors: FieldErrors<TaxFormValues>,
  name: FieldPath<TaxFormValues>,
): string | undefined {
  let cur: unknown = errors;
  for (const part of name.split('.')) {
    if (cur && typeof cur === 'object') {
      cur = (cur as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  const message = (cur as { message?: unknown } | undefined)?.message;
  return typeof message === 'string' ? message : undefined;
}

function serverErrorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    const body = e.body;
    if (body && typeof body === 'object' && 'message' in body) {
      const m = (body as { message?: unknown }).message;
      if (typeof m === 'string') return m;
    }
    return `Request failed (HTTP ${e.status})`;
  }
  return 'Could not reach the server. Is the backend running?';
}

export function TaxCalculatorForm() {
  const { token, isRestoring } = useAuth();
  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<TaxFormValues>({
    resolver: zodResolver(taxFormSchema),
    defaultValues: DEFAULT_VALUES,
    mode: 'onTouched',
  });

  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [result, setResult] = useState<TaxCalculationResponse | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const raw = sessionStorage.getItem(PREFILL_KEY);
    if (!raw) return;
    sessionStorage.removeItem(PREFILL_KEY);
    try {
      reset(JSON.parse(raw) as TaxFormValues);
    } catch {
      // malformed JSON — ignore
    }
  }, [reset]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/incompatible-library
    const { unsubscribe } = watch(() => setSaved(false));
    return unsubscribe;
  }, [watch]);

  const onSubmit = async (values: TaxFormValues) => {
    setSubmitting(true);
    setServerError(null);
    setSaved(false);
    try {
      const payload = { assessmentYear: ASSESSMENT_YEAR, ...values };
      const response = token
        ? await apiPostAuth<TaxCalculationResponse>(
            '/api/calculators/tax/calculate',
            payload,
            token,
          )
        : await apiPost<TaxCalculationResponse>(
            '/api/calculators/tax/calculate',
            payload,
          );
      setResult(response);
      if (token) setSaved(true);
    } catch (e) {
      setResult(null);
      setServerError(serverErrorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  const numberField = ({ name, label, step }: NumberField) => {
    const message = errorAt(errors, name);
    return (
      <div key={name} className="flex flex-col gap-1">
        <Label htmlFor={name}>{label}</Label>
        <Input
          id={name}
          type="number"
          step={step ?? 'any'}
          min={0}
          aria-invalid={message ? true : undefined}
          {...register(name, { valueAsNumber: true })}
        />
        {message && <p className="text-destructive text-sm">{message}</p>}
      </div>
    );
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex w-full max-w-3xl flex-col gap-6"
      noValidate
    >
      <Card>
        <CardHeader>
          <CardTitle>Income (annual, BDT)</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          {INCOME_FIELDS.map(numberField)}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Investments (annual, BDT)</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          {INVESTMENT_FIELDS.map(numberField)}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Other details</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1">
            <Label htmlFor="category">Taxpayer category</Label>
            <Controller
              control={control}
              name="category"
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={(v) => field.onChange(v)}
                >
                  <SelectTrigger id="category" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORY_OPTIONS.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="location">Location</Label>
            <Controller
              control={control}
              name="location"
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={(v) => field.onChange(v)}
                >
                  <SelectTrigger id="location" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LOCATION_OPTIONS.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </div>

          {numberField({
            name: 'disabledChildren',
            label: 'Number of disabled children',
            step: '1',
          })}
          {numberField({
            name: 'advanceIncomeTaxPaid',
            label: 'Advance Income Tax already paid (AIT)',
          })}
        </CardContent>
      </Card>

      <Button
        type="submit"
        className="self-start"
        disabled={submitting || isRestoring}
      >
        {submitting ? 'Calculating…' : 'Calculate'}
      </Button>

      {serverError && (
        <p role="alert" className="text-destructive text-sm">
          {serverError}
        </p>
      )}

      {result && <TaxBreakdown result={result} />}

      {saved && (
        <p
          role="status"
          data-testid="save-indicator"
          className="text-sm text-green-600"
        >
          ✓ Saved to your history.{' '}
          <Link
            href="/account/history"
            className="underline hover:no-underline"
          >
            View history →
          </Link>
        </p>
      )}
    </form>
  );
}
