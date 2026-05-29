package dev.azhar.hishabi.calculators.tax.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshot of all configurable values needed to compute Bangladeshi individual income tax for an
 * assessment year (or several, when NBR doesn't amend the schedule). Mirrors PLAN.MD #10.
 */
@Entity
@Table(name = "tax_rule_set")
@Getter
@Setter
@NoArgsConstructor
public class RuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable label, e.g. "AY 2024-25 / 2025-26 (unchanged from prior year)". */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * PLAN.md #10.2 - taxFreeSalary = MIN(totalEarnings / divisor, cap). 450,000 BDT for AY
     * 2025-26.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal salaryExemptionCap;

    /** PLAN.md #10.2 = denominator in totalEarnings / N. 3 for AY 2025-26 (one-third exemption). */
    @Column(nullable = false)
    private Integer salaryExemptionDivisor;

    /** PLAN.md #10.3 - bonus added to first-slab threshold per disabled child. 50,000 BDT. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal disabledChildThresholdBonus;

    /** PLAN.md #10.5 - first leg: 0.03 * taxableIncome for AY 2025-26. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rebateTaxableFraction;

    /** PLAN.md #10.5 - second leg: 0.15 * eligibleInvestment for AY 2025-26. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rebateEligibleFraction;

    /** PLAN.md #10.5 - third leg (absolute cap on rebate): 1,000,000 BDT for AY 2025-26. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rebateCap;

    /**
     * PLAN.md #10.4 - paying slabs (rows 2-7): the 0% threshold band is per-taxpayer, not stored.
     */
    @OneToMany(
            mappedBy = "ruleSet",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("ordinal ASC")
    private List<TaxSlab> slabs = new ArrayList<>();

    /** PLAN.md #10.3 - one threshold row per taxpayer category. */
    @OneToMany(
            mappedBy = "ruleSet",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<CategoryThreshold> categoryThresholds = new ArrayList<>();

    /** PLAN.md #10.6 - one floor row per location. */
    @OneToMany(
            mappedBy = "ruleSet",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<MinimumTaxFloor> minimumTaxFloors = new ArrayList<>();
}
