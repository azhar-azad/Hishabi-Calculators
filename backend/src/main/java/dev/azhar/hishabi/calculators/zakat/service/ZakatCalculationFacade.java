package dev.azhar.hishabi.calculators.zakat.service;

import dev.azhar.hishabi.calculators.zakat.model.*;
import dev.azhar.hishabi.calculators.zakat.repository.MetalPriceRepository;
import dev.azhar.hishabi.calculators.zakat.repository.ZakatRuleSetRepository;
import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import dev.azhar.hishabi.platform.error.NotFoundException;
import dev.azhar.hishabi.platform.history.Calculation;
import dev.azhar.hishabi.platform.history.CalculationRepository;
import dev.azhar.hishabi.platform.history.CalculatorType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ZakatCalculationFacade {

    private static final long STALE_THRESHOLD_HOURS = 48;

    private final ZakatRuleSetRepository ruleSetRepository;
    private final MetalPriceRepository metalPriceRepository;
    private final ZakatCalculationService calculationService;
    private final UserRepository userRepository;
    private final CalculationRepository calculationRepository;
    private final ObjectMapper objectMapper;

    public ZakatCalculationFacade(ZakatRuleSetRepository ruleSetRepository, MetalPriceRepository metalPriceRepository, ZakatCalculationService calculationService, UserRepository userRepository, CalculationRepository calculationRepository, ObjectMapper objectMapper) {
        this.ruleSetRepository = ruleSetRepository;
        this.metalPriceRepository = metalPriceRepository;
        this.calculationService = calculationService;
        this.userRepository = userRepository;
        this.calculationRepository = calculationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ZakatConfigResponse getConfig() {
        PriceContext ctx = loadPriceContext();
        ZakatRuleSet rules = ctx.rules();
        BigDecimal nisabSilver = calculationService.computeNisab(rules, NisabBasis.SILVER,
                ctx.silverPricePerGram(), ctx.goldPricePerGram());
        BigDecimal nisabGold = calculationService.computeNisab(rules, NisabBasis.GOLD,
                ctx.silverPricePerGram(), ctx.goldPricePerGram());
        return new ZakatConfigResponse(
                nisabSilver,
                nisabGold,
                rules.getNisabSilverGrams(),
                rules.getNisabGoldGrams(),
                rules.getResaleFactor(),
                rules.getRateLunar(),
                rules.getRateSolar(),
                ctx.priceFetchedAt(),
                ctx.priceStale());
    }

    @Transactional
    public ZakatCalculationResponse calculate(ZakatCalculationRequest request) {
        PriceContext ctx = loadPriceContext();
        ZakatCalculationResponse response = calculationService.calculate(
                ctx.rules(),
                request,
                ctx.silverPricePerGram(),
                ctx.goldPricePerGram(),
                ctx.priceFetchedAt(),
                ctx.priceStale());
        persistIfAuthenticated(request, response);
        return response;
    }

    private void persistIfAuthenticated(ZakatCalculationRequest request,
                                        ZakatCalculationResponse response) {

        resolveAuthenticatedUser()
                .ifPresent(user -> {
                    try {
                        calculationRepository.save(
                                Calculation.builder()
                                        .user(user)
                                        .calculatorType(CalculatorType.ZAKAT)
                                        .requestJson(objectMapper.writeValueAsString(request))
                                        .responseJson(objectMapper.writeValueAsString(response))
                                        .build());
                    } catch (JacksonException e) {
                        throw new IllegalStateException("Serialization of validated DTOs cannot fail", e);
                    }
                });
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(auth.getName());
    }

    private PriceContext loadPriceContext() {
        ZakatRuleSet rules = ruleSetRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No Zakat rule set configured"));
        MetalPrice silverPrice = metalPriceRepository.findByMetalAndCarat(Metal.SILVER, "STANDARD")
                .orElseThrow(() -> new NotFoundException("Silver price not in cache"));
        MetalPrice goldPrice = metalPriceRepository.findByMetalAndCarat(Metal.GOLD, "22")
                .orElseThrow(() -> new NotFoundException("Gold price not in cache"));
        Instant fetchedAt = silverPrice.getFetchedAt().isBefore(goldPrice.getFetchedAt())
                ? silverPrice.getFetchedAt()
                : goldPrice.getFetchedAt();
        boolean stale = fetchedAt.isBefore(Instant.now().minus(STALE_THRESHOLD_HOURS, ChronoUnit.HOURS));
        return new PriceContext(
                rules,
                silverPrice.getPricePerGram(),
                goldPrice.getPricePerGram(),
                fetchedAt,
                stale);
    }

    private record PriceContext(
            ZakatRuleSet rules,
            BigDecimal silverPricePerGram,
            BigDecimal goldPricePerGram,
            Instant priceFetchedAt,
            boolean priceStale) {}
}
