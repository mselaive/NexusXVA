package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.application.TradingLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/trading-limits/users")
public class BackOfficeTradingLimitController {

    private final TradingLimitService service;

    public BackOfficeTradingLimitController(TradingLimitService service) {
        this.service = service;
    }

    @GetMapping
    public TradingLimitUserPageResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return TradingLimitUserPageResponse.from(service.search(query, page, size));
    }

    @GetMapping("/{userId}")
    public TradingLimitSnapshotResponse get(@PathVariable UUID userId) {
        return TradingLimitSnapshotResponse.from(service.get(userId));
    }

    @PutMapping("/{userId}")
    public TradingLimitSnapshotResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTradingLimitRequest request,
            HttpServletRequest servletRequest
    ) {
        return TradingLimitSnapshotResponse.from(
                service.update(
                        userId,
                        request.toCommand(),
                        TradingLimitActorResolver.resolve(servletRequest)
                )
        );
    }
}
