package com.nextstep.rentacar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * JPA configuration class.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
@EnableJpaRepositories(basePackages = "com.nextstep.rentacar.repository")
public class JpaConfig {

    /**
     * Provides consistent UTC timestamps for JPA auditing.
     */
    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
