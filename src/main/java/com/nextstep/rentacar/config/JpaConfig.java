package com.nextstep.rentacar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration class.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.nextstep.rentacar.repository")
public class JpaConfig {
}
