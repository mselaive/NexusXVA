package com.nexusxva.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GbmPathGeneratorTest {

    private final GbmPathGenerator generator = new GbmPathGenerator();

    @Test
    void generatesDeterministicPathsWithFixedSeed() {
        GbmParameters parameters = new GbmParameters(100.0, 0.05, 0.01, 0.20);

        double[][] first = generator.generate(parameters, 3, 4, 1.0, 12345L);
        double[][] second = generator.generate(parameters, 3, 4, 1.0, 12345L);

        assertThat(second).isDeepEqualTo(first);
    }

    @Test
    void dividendYieldReducesDeterministicDrift() {
        GbmParameters noDividend = new GbmParameters(100.0, 0.05, 0.0, 0.0);
        GbmParameters withDividend = new GbmParameters(100.0, 0.05, 0.03, 0.0);

        double[][] noDividendPath = generator.generate(noDividend, 1, 1, 1.0, 1L);
        double[][] withDividendPath = generator.generate(withDividend, 1, 1, 1.0, 1L);

        assertThat(noDividendPath[0][1]).isGreaterThan(withDividendPath[0][1]);
        assertThat(noDividendPath[0][1]).isCloseTo(105.1271, org.assertj.core.data.Offset.offset(1.0e-4));
        assertThat(withDividendPath[0][1]).isCloseTo(102.0201, org.assertj.core.data.Offset.offset(1.0e-4));
    }
}
