package com.nexusxva.pricing.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.shared.error.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BlackScholesPricingControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BlackScholesPricingController controller = new BlackScholesPricingController(new EuropeanOptionPricingService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pricesEuropeanCallWithBlackScholes() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "CALL",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES"))
                .andExpect(jsonPath("$.optionType").value("CALL"))
                .andExpect(jsonPath("$.price").value(closeTo(10.4506, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.delta").value(closeTo(0.6368, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.gamma").value(closeTo(0.0188, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.vega").value(closeTo(37.5240, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.theta").value(closeTo(-6.4140, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.rho").value(closeTo(53.2325, 0.0001), Double.class));
    }

    @Test
    void pricesEuropeanPutWithBlackScholes() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "PUT",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES"))
                .andExpect(jsonPath("$.optionType").value("PUT"))
                .andExpect(jsonPath("$.price").value(closeTo(5.5735, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.delta").value(closeTo(-0.3632, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.gamma").value(closeTo(0.0188, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.vega").value(closeTo(37.5240, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.theta").value(closeTo(-1.6579, 0.0001), Double.class))
                .andExpect(jsonPath("$.greeks.rho").value(closeTo(-41.8905, 0.0001), Double.class));
    }

    @Test
    void acceptsNegativeRiskFreeRateWhenResultIsFinite() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "CALL",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": -0.01,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES"))
                .andExpect(jsonPath("$.price", greaterThan(0.0)));
    }

    @Test
    void invalidNumericInputsReturnValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "CALL",
                                  "spot": 0.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("spot"));
    }

    @Test
    void zeroMaturityReturnsValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "CALL",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 0.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("timeToMaturityYears"));
    }

    @Test
    void missingFieldsReturnValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("optionType"));
    }

    @Test
    void nonFinitePricingResultReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "CALL",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1000.0,
                                  "riskFreeRate": -1000.0,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("must be finite")));
    }

    @Test
    void malformedOptionTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/pricing/european-options/black-scholes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "optionType": "DIGITAL",
                                  "spot": 100.0,
                                  "strike": 100.0,
                                  "timeToMaturityYears": 1.0,
                                  "riskFreeRate": 0.05,
                                  "volatility": 0.2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid optionType. Accepted values are CALL and PUT"))
                .andExpect(jsonPath("$.message", not(containsString("com.nexusxva"))));
    }
}
