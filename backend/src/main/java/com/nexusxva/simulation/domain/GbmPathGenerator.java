package com.nexusxva.simulation.domain;

import java.util.Random;

public class GbmPathGenerator {

    public double[][] generate(
            GbmParameters parameters,
            int paths,
            int timeSteps,
            double horizonYears,
            long seed
    ) {
        if (parameters == null) {
            throw new IllegalArgumentException("gbm parameters are required");
        }
        if (paths <= 0) {
            throw new IllegalArgumentException("paths must be greater than zero");
        }
        if (timeSteps <= 0) {
            throw new IllegalArgumentException("timeSteps must be greater than zero");
        }
        if (!Double.isFinite(horizonYears) || horizonYears <= 0.0) {
            throw new IllegalArgumentException("horizonYears must be greater than zero");
        }

        double[][] spots = new double[paths][timeSteps + 1];
        double dt = horizonYears / timeSteps;
        double drift = (parameters.riskFreeRate()
                - parameters.dividendYield()
                - 0.5 * parameters.volatility() * parameters.volatility()) * dt;
        double diffusionScale = parameters.volatility() * Math.sqrt(dt);
        Random random = new Random(seed);

        for (int path = 0; path < paths; path++) {
            spots[path][0] = parameters.initialSpot();
            for (int step = 1; step <= timeSteps; step++) {
                double z = random.nextGaussian();
                spots[path][step] = spots[path][step - 1] * Math.exp(drift + diffusionScale * z);
            }
        }

        return spots;
    }
}
