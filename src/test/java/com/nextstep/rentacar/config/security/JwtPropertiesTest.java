package com.nextstep.rentacar.config.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void rejectsMissingSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("  ");

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be set");
    }

    @Test
    void rejectsShortSecretWithoutRevealingIt() {
        String tooShort = "almost-the-real-secret-value";
        JwtProperties properties = new JwtProperties();
        properties.setSecret(tooShort);

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters")
                .hasMessageContaining("but has " + tooShort.length())
                .hasMessageNotContaining(tooShort);
    }

    @Test
    void acceptsLongEnoughSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("a-secret-that-is-at-least-32-characters-long");

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void toStringDoesNotExposeSecret() {
        String secret = "a-secret-that-is-at-least-32-characters-long";
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);

        assertThat(properties.toString()).doesNotContain(secret);
    }
}
