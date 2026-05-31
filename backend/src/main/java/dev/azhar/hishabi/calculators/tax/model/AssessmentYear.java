package dev.azhar.hishabi.calculators.tax.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Bangladeshi income-tax assessment year (e.g., "2025-26"), Multiple AYs can reference the same
 * {@link RuleSet} when NBR leaves the schedule unchanged year-over-year (PLAN.md #10.0).
 */
@Entity
@Table(name = "tax_assessment_year", uniqueConstraints = @UniqueConstraint(columnNames = "label"))
@Getter
@Setter
@NoArgsConstructor
public class AssessmentYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Canonical AY label, e.g., "2025-26", Unique. */
    @Column(nullable = false, length = 16)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;
}
