package com.nexusxva.cva.api;

import com.nexusxva.cva.application.CvaCalculationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class CvaController {

    private final CvaCalculationService cvaCalculationService;

    public CvaController(CvaCalculationService cvaCalculationService) {
        this.cvaCalculationService = cvaCalculationService;
    }

    @PostMapping("/cva")
    public CvaCalculationResponse calculateCva(@Valid @RequestBody CvaCalculationRequest request) {
        return CvaCalculationResponse.from(cvaCalculationService.calculate(request.toCommand()));
    }
}
