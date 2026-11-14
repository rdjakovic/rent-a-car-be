package com.nextstep.rentacar.config;

import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPasswordInitializerTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void doesNothingWhenPasswordNotConfigured() {
        new AdminPasswordInitializer(userRepository, passwordEncoder, "").run(null);

        verifyNoInteractions(userRepository);
    }

    @Test
    void setsPasswordWhenItDiffers() {
        User admin = adminWithPassword("old-password");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        new AdminPasswordInitializer(userRepository, passwordEncoder, "new-strong-password").run(null);

        verify(userRepository).save(admin);
        assertThat(passwordEncoder.matches("new-strong-password", admin.getPassword())).isTrue();
    }

    @Test
    void leavesPasswordUntouchedWhenAlreadyMatching() {
        User admin = adminWithPassword("same-password");
        String hashBefore = admin.getPassword();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        new AdminPasswordInitializer(userRepository, passwordEncoder, "same-password").run(null);

        verify(userRepository, never()).save(any());
        assertThat(admin.getPassword()).isEqualTo(hashBefore);
    }

    @Test
    void doesNotFailWhenAdminUserMissing() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        new AdminPasswordInitializer(userRepository, passwordEncoder, "new-strong-password").run(null);

        verify(userRepository, never()).save(any());
    }

    private User adminWithPassword(String rawPassword) {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(rawPassword));
        return admin;
    }
}
