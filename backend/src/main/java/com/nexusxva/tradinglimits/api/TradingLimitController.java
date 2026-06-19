package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.application.TradingLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trading-limits")
public class TradingLimitController {

    private final TradingLimitService service;

    public TradingLimitController(TradingLimitService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public TradingLimitSnapshotResponse mine(HttpServletRequest request) {
        return TradingLimitSnapshotResponse.from(
                service.mine(TradingLimitActorResolver.resolve(request))
        );
    }
}

