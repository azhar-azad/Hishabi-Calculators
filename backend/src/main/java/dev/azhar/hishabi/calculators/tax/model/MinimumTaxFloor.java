package dev.azhar.hishabi.calculators.tax.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Minimum tax floor by location, applied when grossTax - rebate < floor (PLAN.md #1-.6). */
@Entity
@Table(
        name = "tax_minimum_tax_floor",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rule_set_id", "location"}))
@Getter
@Setter
@NoArgsConstructor
public class MinimumTaxFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(48)")
    private Location location;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
