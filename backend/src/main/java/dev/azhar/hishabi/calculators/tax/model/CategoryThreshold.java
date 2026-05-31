package dev.azhar.hishabi.calculators.tax.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tax-free threshold by taxpayer category (PLAN.md #10.3). */
@Entity
@Table(
        name = "tax_category_threshold",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rule_set_id", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class CategoryThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    private TaxpayerCategory category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
