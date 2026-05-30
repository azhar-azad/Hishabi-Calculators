package dev.azhar.hishabi.calculators.tax.web;

import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse;
import dev.azhar.hishabi.calculators.tax.service.TaxCalculationFacade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint for the Bangladeshi income-tax calculator. */
@RestController
@RequestMapping("/api/calculators/tax")
public class TaxCalculationController {

    private final TaxCalculationFacade facade;

    public TaxCalculationController(TaxCalculationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/calculate")
    public TaxCalculationResponse calculate(@Valid @RequestBody TaxCalculationRequest request) {
        return facade.calculate(request);
    }
}
