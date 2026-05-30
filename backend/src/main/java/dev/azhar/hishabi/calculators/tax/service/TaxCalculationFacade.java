package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.AssessmentYear;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse;
import dev.azhar.hishabi.calculators.tax.repository.AssessmentYearRepository;
import dev.azhar.hishabi.platform.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the tax calculator; resolves the rule set for an assessment year
 * (defaulting to the latest) and delegates to the pure {@link TaxCalculationService}. Runs in a
 * read-only transaction so the rule set's lazy {@code @OneToMany} collections initialize before the
 * pure calculation walks them.
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
}
