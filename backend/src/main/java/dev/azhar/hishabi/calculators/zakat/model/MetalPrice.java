package dev.azhar.hishabi.calculators.zakat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "metal_price")
@Getter
@Setter
@NoArgsConstructor
public class MetalPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Metal metal;

    @Column(nullable = false, length = 16)
    private String carat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerGram;

    @Column(length = 120)
    private String source;

    @Column(nullable = false)
    private Instant fetchedAt;
}
