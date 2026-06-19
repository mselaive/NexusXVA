package com.nexusxva.auth.infrastructure;

import com.nexusxva.auth.application.AuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AuthConfiguration {
}
