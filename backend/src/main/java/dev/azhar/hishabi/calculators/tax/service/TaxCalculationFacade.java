package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.AssessmentYear;
import dev.azhar.hishabi.calculators.tax.model.CategoryThreshold;
import dev.azhar.hishabi.calculators.tax.model.MinimumTaxFloor;
import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse;
import dev.azhar.hishabi.calculators.tax.model.TaxRulesResponse;
import dev.azhar.hishabi.calculators.tax.repository.AssessmentYearRepository;
import dev.azhar.hishabi.platform.error.NotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the tax calculator; resolves the rule set for an assessment year
 * (defaulting to the latest) and either runs a calculation or returns the rule set itself. Runs in
 * a read-only transaction so the rule set's lazy {@code @OneToMany} collections initialize before
 * they are walked or mapped.
 */
@Service
public class TaxCalculationFacade {

    private final AssessmentYearRepository assessmentYears;
    private final TaxCalculationService calculationService;

    public TaxCalculationFacade(
            AssessmentYearRepository assessmentYears, TaxCalculationService calculationService) {
        this.assessmentYears = assessmentYears;
        this.calculationService = calculationService;
    }

    @Transactional(readOnly = true)
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        AssessmentYear assessmentYear = resolveAssessmentYear(request.assessmentYear());
        return calculationService.calculate(
                assessmentYear.getRuleSet(), assessmentYear.getLabel(), request);
    }

    @Transactional(readOnly = true)
    public TaxRulesResponse getRules(String assessmentYearLabel) {
        AssessmentYear assessmentYear = resolveAssessmentYear(assessmentYearLabel);
        return toRulesResponse(assessmentYear);
    }

    private AssessmentYear resolveAssessmentYear(String label) {
        if (label == null || label.isBlank()) {
            return assessmentYears
                    .findTopByOrderByLabelDesc()
                    .orElseThrow(
                            () -> new NotFoundException(("No assessment years are configured")));
        }
        return assessmentYears
                .findByLabel(label)
                .orElseThrow(() -> new NotFoundException("Unknown assessment year: " + label));
    }

    private TaxRulesResponse toRulesResponse(AssessmentYear assessmentYear) {
        RuleSet ruleSet = assessmentYear.getRuleSet();

        List<TaxRulesResponse.Slab> slabs =
                ruleSet.getSlabs().stream()
                        .map(
                                s ->
                                        new TaxRulesResponse.Slab(
                                                s.getOrdinal(), s.getWidth(), s.getRate()))
                        .toList();

        List<TaxRulesResponse.Threshold> thresholds =
                ruleSet.getCategoryThresholds().stream()
                        .sorted(Comparator.comparing(CategoryThreshold::getCategory))
                        .map(t -> new TaxRulesResponse.Threshold(t.getCategory(), t.getAmount()))
                        .toList();

        List<TaxRulesResponse.Floor> floors =
                ruleSet.getMinimumTaxFloors().stream()
                        .sorted(Comparator.comparing(MinimumTaxFloor::getLocation))
                        .map(f -> new TaxRulesResponse.Floor(f.getLocation(), f.getAmount()))
                        .toList();

        return new TaxRulesResponse(
                assessmentYear.getLabel(),
                ruleSet.getName(),
                ruleSet.getSalaryExemptionCap(),
                ruleSet.getSalaryExemptionDivisor(),
                ruleSet.getDisabledChildThresholdBonus(),
                ruleSet.getRebateTaxableFraction(),
                ruleSet.getRebateEligibleFraction(),
                ruleSet.getRebateCap(),
                ruleSet.getSanchayPatraCap(),
                ruleSet.getDpsCap(),
                slabs,
                thresholds,
                floors);
    }
}
