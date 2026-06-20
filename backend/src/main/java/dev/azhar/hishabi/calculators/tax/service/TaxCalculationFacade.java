package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.AssessmentYear;
import dev.azhar.hishabi.calculators.tax.model.CategoryThreshold;
import dev.azhar.hishabi.calculators.tax.model.MinimumTaxFloor;
import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse;
import dev.azhar.hishabi.calculators.tax.model.TaxHistoryItemResponse;
import dev.azhar.hishabi.calculators.tax.model.TaxRulesResponse;
import dev.azhar.hishabi.calculators.tax.repository.AssessmentYearRepository;
import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import dev.azhar.hishabi.platform.error.NotFoundException;
import dev.azhar.hishabi.platform.error.UnauthorizedException;
import dev.azhar.hishabi.platform.history.Calculation;
import dev.azhar.hishabi.platform.history.CalculationRepository;
import dev.azhar.hishabi.platform.history.CalculatorType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Application service for the tax calculator; resolves the rule set for an assessment year
 * (defaulting to the latest) and either runs a calculation or returns the rule set itself.
 * Transactions keep lazy {@code @OneToMany} collections open until after they are walked or mapped.
 */
@Service
public class TaxCalculationFacade {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationFacade.class);

    private final AssessmentYearRepository assessmentYears;
    private final TaxCalculationService calculationService;
    private final UserRepository userRepository;
    private final CalculationRepository calculationRepository;
    private final ObjectMapper objectMapper;

    public TaxCalculationFacade(
            AssessmentYearRepository assessmentYears,
            TaxCalculationService calculationService,
            UserRepository userRepository,
            CalculationRepository calculationRepository,
            ObjectMapper objectMapper) {
        this.assessmentYears = assessmentYears;
        this.calculationService = calculationService;
        this.userRepository = userRepository;
        this.calculationRepository = calculationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        AssessmentYear assessmentYear = resolveAssessmentYear(request.assessmentYear());

        TaxCalculationResponse response =
                calculationService.calculate(
                        assessmentYear.getRuleSet(), assessmentYear.getLabel(), request);
        persistIfAuthenticated(request, response);
        return response;
    }

    @Transactional(readOnly = true)
    public TaxRulesResponse getRules(String assessmentYearLabel) {
        AssessmentYear assessmentYear = resolveAssessmentYear(assessmentYearLabel);
        return toRulesResponse(assessmentYear);
    }

    @Transactional(readOnly = true)
    public List<String> listYears() {
        return assessmentYears.findAllByOrderByLabelDesc().stream()
                .map(AssessmentYear::getLabel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TaxHistoryItemResponse> listHistory(Pageable pageable) {
        User user =
                resolveAuthenticatedUser()
                        .orElseThrow(
                                () -> new UnauthorizedException("Authenticated user not found"));
        return calculationRepository
                .findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                        user.getId(), CalculatorType.TAX, pageable)
                .map(this::toHistoryItem);
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

    private void persistIfAuthenticated(
            TaxCalculationRequest request, TaxCalculationResponse response) {
        resolveAuthenticatedUser()
                .ifPresent(
                        user -> {
                            try {
                                calculationRepository.save(
                                        Calculation.builder()
                                                .user(user)
                                                .calculatorType(CalculatorType.TAX)
                                                .requestJson(
                                                        objectMapper.writeValueAsString(request))
                                                .responseJson(
                                                        objectMapper.writeValueAsString(response))
                                                .build());
                            } catch (JacksonException e) {
                                throw new IllegalStateException(
                                        "Serialization of validated DTOs cannot fail", e);
                            }
                        });
    }

    /**
     * Resolves the currently authenticated {@link User} from the security context. Returns {@code
     * Optional.empty()} for anonymous or unauthenticated principals, so callers decide how to
     * handle the absence.
     */
    private Optional<User> resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(auth.getName());
    }

    private TaxHistoryItemResponse toHistoryItem(Calculation calc) {
        try {
            JsonNode req = objectMapper.readTree(calc.getRequestJson());
            JsonNode res = objectMapper.readTree(calc.getResponseJson());
            return new TaxHistoryItemResponse(calc.getId(), calc.getCreatedAt(), req, res);
        } catch (JacksonException e) {
            log.warn(
                    "Calculation row id={} has malformed JSON; returning degraded entry",
                    calc.getId());
            return new TaxHistoryItemResponse(calc.getId(), calc.getCreatedAt(), null, null);
        }
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
