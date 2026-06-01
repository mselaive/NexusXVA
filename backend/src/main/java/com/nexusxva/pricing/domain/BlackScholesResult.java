package com.nexusxva.pricing.domain;

import com.nexusxva.instruments.domain.OptionType;

public record BlackScholesResult(
        OptionType optionType,
        double price,
        Greeks greeks
) {
}
