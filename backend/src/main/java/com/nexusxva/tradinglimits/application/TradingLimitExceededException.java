package com.nexusxva.tradinglimits.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class TradingLimitExceededException extends RuntimeException {

    private final Map<String, Object> metadata;

    public TradingLimitExceededException(
            String limitType,
            Number maximum,
            Number currentUsage,
            Number requested,
            Instant periodEndsAt
    ) {
        super("Trading limit exceeded: " + limitType);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("limitType", limitType);
        values.put("maximum", normalize(maximum));
        values.put("currentUsage", normalize(currentUsage));
        values.put("requested", normalize(requested));
        values.put("periodEndsAt", periodEndsAt.toString());
        this.metadata = Map.copyOf(values);
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    private static Object normalize(Number value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        return value;
    }
}

