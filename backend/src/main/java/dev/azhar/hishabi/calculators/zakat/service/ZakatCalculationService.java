package dev.azhar.hishabi.calculators.zakat.service;

import dev.azhar.hishabi.calculators.zakat.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ZakatCalculationService {

    ZakatCalculationResponse calculate(
            ZakatRuleSet rules,
            ZakatCalculationRequest request,
            BigDecimal silverPricePerGram,
            BigDecimal goldPricePerGram,
            Instant priceFetchedAt,
            boolean priceStale) {

        BigDecimal totalAssets = sumAssets(request.assets());
        BigDecimal totalDeductions = sumDeductions(request.deductions());
        BigDecimal netWealth = Money.scale(totalAssets.subtract(totalDeductions).max(BigDecimal.ZERO));

        BigDecimal nisab = computeNisab(rules, request.nisabBasis(), silverPricePerGram, goldPricePerGram);
        boolean aboveNisab = netWealth.compareTo(nisab) >= 0;

        BigDecimal rate = request.calendarType() == CalendarType.HIJRI
                ? rules.getRateLunar()
                : rules.getRateSolar();

        BigDecimal zakatPayable = aboveNisab
                ? Money.scale(netWealth.multiply(rate))
                : BigDecimal.ZERO.setScale(Money.SCALE);

        return new ZakatCalculationResponse(
                request.nisabBasis(),
                request.calendarType(),
                totalAssets,
                totalDeductions,
                netWealth,
                nisab,
                aboveNisab,
                rate,
                zakatPayable,
                priceFetchedAt,
                priceStale);
    }

    BigDecimal computeNisab(
            ZakatRuleSet rules,
            NisabBasis basis,
            BigDecimal silverPricePerGram,
            BigDecimal goldPricePerGram) {

        if (basis == NisabBasis.SILVER) {
            return Money.scale(
                    rules.getNisabSilverGrams()
                            .multiply(silverPricePerGram)
                            .multiply(rules.getResaleFactor()));
        }

        return Money.scale(
                rules.getNisabGoldGrams()
                        .multiply(goldPricePerGram)
                        .multiply(rules.getResaleFactor()));
    }

    BigDecimal sumAssets(ZakatCalculationRequest.Assets a) {
        return Money.scale(
                a.gold()
                        .add(a.silver())
                        .add(a.cashInHand())
                        .add(a.foreignCurrency())
                        .add(a.bankDeposits())
                        .add(a.savingsInstruments())
                        .add(a.refundableInsurancePremium())
                        .add(a.optionalProvidentFund())
                        .add(a.recoverableLoans())
                        .add(a.depositsPlacedWithOthers())
                        .add(a.refundableRentSecurity())
                        .add(a.refundableOtherSecurity())
                        .add(a.businessCash())
                        .add(a.customerReceivables())
                        .add(a.tradeStock())
                        .add(a.tradeAssetsForResale())
                        .add(a.mudarabaShareNetKnown())
                        .add(a.mudarabaPrincipal())
                        .add(a.shareMarketCapitalGain())
                        .add(a.shareDividendNetKnown())
                        .add(a.shareDividendMarketValue()));
    }

    BigDecimal sumDeductions(ZakatCalculationRequest.Deductions d) {
        return Money.scale(
                d.personalLoanDueWithinYear()
                        .add(d.businessLoanDueWithinYear())
                        .add(d.installmentPurchasesDueWithinYear())
                        .add(d.unpaidDowryDueWithinYear())
                        .add(d.unpaidEmployeeWages())
                        .add(d.payableBillsAndTaxes())
                        .add(d.priorYearUnpaidZakat()));
    }
}
