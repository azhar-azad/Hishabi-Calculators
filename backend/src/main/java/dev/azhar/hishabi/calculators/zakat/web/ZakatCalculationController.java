package dev.azhar.hishabi.calculators.zakat.web;

import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationResponse;
import dev.azhar.hishabi.calculators.zakat.model.ZakatConfigResponse;
import dev.azhar.hishabi.calculators.zakat.service.ZakatCalculationFacade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculators/zakat")
public class ZakatCalculationController {

    private final ZakatCalculationFacade facade;

    public ZakatCalculationController(ZakatCalculationFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/config")
    public ZakatConfigResponse config() {
        return facade.getConfig();
    }

    @PostMapping("/calculate")
    public ZakatCalculationResponse calculate(@Valid @RequestBody ZakatCalculationRequest request) {
        return facade.calculate(request);
    }
}
