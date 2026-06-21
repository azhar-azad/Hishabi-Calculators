package dev.azhar.hishabi.calculators.zakat.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ZakatCalculationResponse(
        NisabBasis nisabBasis,
        CalendarType calendarType,
        BigDecimal totalAssets,
        BigDecimal totalDeductions,
        BigDecimal netWealth,
        BigDecimal nisabUsed,
        boolean aboveNisab,
        BigDecimal rateApplied,
        BigDecimal zakatPayable,
        Instant priceFetchedAt,
        boolean priceStale) { }
