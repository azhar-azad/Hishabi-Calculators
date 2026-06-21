package dev.azhar.hishabi.calculators.zakat.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ZakatCalculationRequest(
        @NotNull NisabBasis nisabBasis,
        @NotNull CalendarType calendarType,
        LocalDate zakatYearEndDate,
        @NotNull @Valid Assets assets,
        @NotNull @Valid Deductions deductions) {

    public record Assets(
            @NotNull @PositiveOrZero BigDecimal gold,
            @NotNull @PositiveOrZero BigDecimal silver,
            @NotNull @PositiveOrZero BigDecimal cashInHand,
            @NotNull @PositiveOrZero BigDecimal foreignCurrency,
            @NotNull @PositiveOrZero BigDecimal bankDeposits,
            @NotNull @PositiveOrZero BigDecimal savingsInstruments,
            @NotNull @PositiveOrZero BigDecimal refundableInsurancePremium,
            @NotNull @PositiveOrZero BigDecimal optionalProvidentFund,
            @NotNull @PositiveOrZero BigDecimal recoverableLoans,
            @NotNull @PositiveOrZero BigDecimal depositsPlacedWithOthers,
            @NotNull @PositiveOrZero BigDecimal refundableRentSecurity,
            @NotNull @PositiveOrZero BigDecimal refundableOtherSecurity,
            @NotNull @PositiveOrZero BigDecimal businessCash,
            @NotNull @PositiveOrZero BigDecimal customerReceivables,
            @NotNull @PositiveOrZero BigDecimal tradeStock,
            @NotNull @PositiveOrZero BigDecimal tradeAssetsForResale,
            @NotNull @PositiveOrZero BigDecimal mudarabaShareNetKnown,
            @NotNull @PositiveOrZero BigDecimal mudarabaPrincipal,
            @NotNull @PositiveOrZero BigDecimal shareMarketCapitalGain,
            @NotNull @PositiveOrZero BigDecimal shareDividendNetKnown,
            @NotNull @PositiveOrZero BigDecimal shareDividendMarketValue) {}

    public record Deductions(
            @NotNull @PositiveOrZero BigDecimal personalLoanDueWithinYear,
            @NotNull @PositiveOrZero BigDecimal businessLoanDueWithinYear,
            @NotNull @PositiveOrZero BigDecimal installmentPurchasesDueWithinYear,
            @NotNull @PositiveOrZero BigDecimal unpaidDowryDueWithinYear,
            @NotNull @PositiveOrZero BigDecimal unpaidEmployeeWages,
            @NotNull @PositiveOrZero BigDecimal payableBillsAndTaxes,
            @NotNull @PositiveOrZero BigDecimal priorYearUnpaidZakat) {}
}
