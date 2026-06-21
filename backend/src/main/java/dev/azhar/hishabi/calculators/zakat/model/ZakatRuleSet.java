package dev.azhar.hishabi.calculators.zakat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "zakat_rule_set")
@Getter
@Setter
@NoArgsConstructor
public class ZakatRuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal nisabSilverGrams;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal nisabGoldGrams;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal resaleFactor;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rateLunar;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rateSolar;
}
