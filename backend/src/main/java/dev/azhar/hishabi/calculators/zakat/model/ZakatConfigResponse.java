package dev.azhar.hishabi.calculators.zakat.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ZakatConfigResponse(
        BigDecimal nisabSilver,
        BigDecimal nisabGold,
        BigDecimal nisabSilverGrams,
        BigDecimal nisabGoldGrams,
        BigDecimal resaleFactor,
        BigDecimal rateLunar,
        BigDecimal rateSolar,
        Instant priceFetchedAt,
        boolean priceStale) { }
