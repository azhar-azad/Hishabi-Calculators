package dev.azhar.hishabi.calculators.zakat.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {

    static final int SCALE = 2;
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, SCALE, ROUNDING);
    }
}
