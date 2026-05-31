package dev.azhar.hishabi.calculators.tax.model;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.Investments;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TaxCalculationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validRequestHasNoViolations() {
        Set<ConstraintViolation<TaxCalculationRequest>> violations =
                validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    void nullCategoryIsRejected() {
        TaxCalculationRequest req =
                new TaxCalculationRequest(
                        "2025-26",
                        null,
                        Location.OTHER,
                        0,
                        validIncome(),
                        validInvestments(),
                        BigDecimal.ZERO);
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("category"));
    }

    @Test
    void negativeIncomeComponentIsRejected() {
        IncomeComponents badIncome =
                new IncomeComponents(
                        new BigDecimal("-1"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        TaxCalculationRequest req =
                new TaxCalculationRequest(
                        "2025-26",
                        TaxpayerCategory.GENERAL,
                        Location.OTHER,
                        0,
                        badIncome,
                        validInvestments(),
                        BigDecimal.ZERO);
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("income.basic"));
    }

    @Test
    void negativeDisabledChildrenIsRejected() {
        TaxCalculationRequest req =
                new TaxCalculationRequest(
                        "2025-26",
                        TaxpayerCategory.GENERAL,
                        Location.OTHER,
                        -1,
                        validIncome(),
                        validInvestments(),
                        BigDecimal.ZERO);
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("disabledChildren"));
    }

    @Test
    void nullMoneyFieldIsRejected() {
        TaxCalculationRequest req =
                new TaxCalculationRequest(
                        "2025-26",
                        TaxpayerCategory.GENERAL,
                        Location.OTHER,
                        0,
                        validIncome(),
                        validInvestments(),
                        null);
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("advanceIncomeTaxPaid"));
    }

    private static TaxCalculationRequest validRequest() {
        return new TaxCalculationRequest(
                "2025-26",
                TaxpayerCategory.GENERAL,
                Location.OTHER,
                0,
                validIncome(),
                validInvestments(),
                BigDecimal.ZERO);
    }

    private static IncomeComponents validIncome() {
        return new IncomeComponents(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }

    private static Investments validInvestments() {
        return new Investments(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
