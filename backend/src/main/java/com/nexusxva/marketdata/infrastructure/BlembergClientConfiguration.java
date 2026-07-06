package com.nexusxva.marketdata.infrastructure;

import com.nexusxva.marketdata.application.MarketDataValidationProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({BlembergProperties.class, MarketDataValidationProperties.class})
class BlembergClientConfiguration {

    @Bean
    RestClient blembergRestClient(BlembergProperties properties) {
        return restClient(properties.getBaseUrl(), properties.getTimeout());
    }

    @Bean
    RestClient blembergRefreshRestClient(BlembergProperties properties) {
        return restClient(properties.getBaseUrl(), properties.getRefreshTimeout());
    }

    private RestClient restClient(String baseUrl, java.time.Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
