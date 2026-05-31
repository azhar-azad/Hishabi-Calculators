package dev.azhar.hishabi.calculators.tax.model;

/**
 * Bangladeshi income-tax taxpayer category. Each category has a different first-slab threshold (the
 * tax-free amount before slab rates apply) — see {@code CategoryThreshold} in PLAN.md §10.3.
 */
public enum TaxpayerCategory {
    /** Default category for adult male residents not otherwise classified. */
    GENERAL,

    /** Adult female residents. */
    WOMAN,

    /** Residents aged 65 or older at the end of the assessment year. */
    SENIOR_65_PLUS,

    /** Residents with a recognized physical or mental disability. */
    PHYSICALLY_MENTALLY_DISABLED,

    /** Gazetted freedom fighters of the Bangladesh Liberation war. */
    GAZETTED_FREEDOM_FIGHTER,

    /** Residents recognized under the third-gender (hijra) category. */
    THIRD_GENDER
}
