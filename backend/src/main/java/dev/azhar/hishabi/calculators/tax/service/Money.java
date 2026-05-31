package dev.azhar.hishabi.calculators.tax.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central monetary rounding policy for tax calculations (PLAN.md #10.10): 2 decimal places (paisa),
 * HALF_UP. The source Excel keeps paisa precision. Kept in one place so the policy can be changed
 * if the spreadsheet's behavior is ever found to differ.
 */
final class Money {

    static final int SCALE = 2;
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    /** Round a value to the monetary scale (2dp, HALF_UP). */
    static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    /** Divide and round the quotient to the monetary scale. */
    static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, SCALE, ROUNDING);
    }
}
