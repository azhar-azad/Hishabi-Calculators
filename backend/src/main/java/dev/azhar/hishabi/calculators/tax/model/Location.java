package dev.azhar.hishabi.calculators.tax.model;

/**
 * Taxpayer residence location used for minimum-tax-floor determination (PLAN.md $10.6). Floors:
 * Dhaka/Chittagong CC = 5,000 BDT other city corps = 4,000 BDT elsewhere = 3,000 BDT
 */
public enum Location {

    /** Dhaka or Chittagong City Corporation. */
    DHAKA_CHITTAGONG_CITY_CORP,

    /** Any other City Corporation. */
    OTHER_CITY_CORP,

    /** Outside any City Corporation. */
    OTHER
}
