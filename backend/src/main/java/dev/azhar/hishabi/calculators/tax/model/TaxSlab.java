package dev.azhar.hishabi.calculators.tax.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One paying slab (rows 2-7 of PLAN.md #10.4). The 0% threshold band is NOT stored here - it's a
 * function of {@link CategoryThreshold} + disabled-child count, applied at calc time.
 */
@Entity
@Table(
        name = "tax_slab",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rule_set_id", "ordinal"}))
@Getter
@Setter
@NoArgsConstructor
public class TaxSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;

    /** 1-based slab order within the rule set (1..N where N is the top slab.) */
    @Column(nullable = false)
    private Integer ordinal;

    /** Slab width in BDT. Null means "(rest)" - the top open-ended slab. */
    @Column(precision = 15, scale = 2)
    private BigDecimal width;

    /** Tax rate as a fraction, e.g., 0.05 for 5%. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;
}
