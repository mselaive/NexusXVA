package com.nexusxva.pricing.domain;

public record Greeks(
        double delta,
        double gamma,
        double vega,
        double theta,
        double rho
) {
}
