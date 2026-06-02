import { z } from 'zod';

// Empty number inputs arrive as NaN (valueAsNumber); z.number() rejects NaN,
// so the "required" message covers the empty case too.
const amount = (label: string) =>
  z
    .number({ error: `${label} is required` })
    .min(0, { error: `${label} cannot be negative` });

export const taxFormSchema = z.object({
  category: z.enum([
    'GENERAL',
    'WOMAN',
    'SENIOR_65_PLUS',
    'PHYSICALLY_MENTALLY_DISABLED',
    'GAZETTED_FREEDOM_FIGHTER',
    'THIRD_GENDER',
  ]),
  location: z.enum(['DHAKA_CHITTAGONG_CITY_CORP', 'OTHER_CITY_CORP', 'OTHER']),
  disabledChildren: z
    .number({ error: 'Number of disabled children is required' })
    .int({ error: 'Must be a whole number' })
    .min(0, { error: 'Cannot be negative' }),
  income: z.object({
    basic: amount('Basic'),
    houseRent: amount('House rent'),
    conveyance: amount('Conveyance'),
    medicalAllowance: amount('Medical allowance'),
    leaveEncashment: amount('Leave encashment'),
    performanceBonus: amount('Performance bonus'),
    yearlyBonus: amount('Yearly bonus'),
    festivalBonus: amount('Festival bonus'),
    overtime: amount('Overtime'),
    transportation: amount('Transportation'),
  }),
  investments: z.object({
    sanchayPatra: amount('Sanchay Patra'),
    dps: amount('DPS'),
    mutualFund: amount('Mutual Fund'),
    treasuryBond: amount('Treasury Bond'),
    providentFundEmployee: amount('Provident Fund (employee)'),
    providentFundEmployer: amount('Provident Fund (employer)'),
    stock: amount('Stock'),
  }),
  advanceIncomeTaxPaid: amount('Advance Income Tax'),
});

export type TaxFormValues = z.infer<typeof taxFormSchema>;
