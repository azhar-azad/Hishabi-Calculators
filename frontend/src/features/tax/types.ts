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
