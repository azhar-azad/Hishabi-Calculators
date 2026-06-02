export type Slab = {
  ordinal: number;
  width: number | null;
  rate: number;
};

export type CategoryThreshold = {
  category: string;
  amount: number;
};

export type MinimumTaxFloor = {
  location: string;
  amount: number;
};

export type TaxRulesResponse = {
  assessmentYear: string;
  ruleSetName: string;
  salaryExemptionCap: number;
  salaryExemptionDivisor: number;
  disabledChildThresholdBonus: number;
  rebateTaxableFraction: number;
  rebateEligibleFraction: number;
  rebateCap: number;
  sanchayPatraCap: number;
  dpsCap: number;
  slabs: Slab[];
  categoryThresholds: CategoryThreshold[];
  minimumTaxFloors: MinimumTaxFloor[];
};

export type SlabTax = {
  ordinal: number;
  rate: number;
  taxableAmountInSlab: number;
  tax: number;
};

export type TaxCalculationResponse = {
  assessmentYear: string;
  totalEarnings: number;
  taxFreeSalaryExemption: number;
  taxableIncome: number;
  effectiveFirstSlabThreshold: number;
  slabs: SlabTax[];
  grossTax: number;
  eligibleInvestment: number;
  rebate: number;
  afterRebate: number;
  minimumTaxFloor: number;
  minimumTaxApplied: boolean;
  taxAfterFloor: number;
  advanceIncomeTaxPaid: number;
  netTax: number;
};
