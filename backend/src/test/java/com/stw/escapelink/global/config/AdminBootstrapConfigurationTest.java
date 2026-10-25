package com.stw.escapelink.global.config;

import com.stw.escapelink.admin.domain.Admin;
import com.stw.escapelink.admin.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminBootstrapConfigurationTest {

    private final AdminRepository adminRepository = mock(AdminRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminBootstrapConfiguration configuration = new AdminBootstrapConfiguration();

    @Test
    void skipsBootstrapWhenCredentialsAreNotConfigured() throws Exception {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();

        configuration.bootstrapAdmin(adminRepository, passwordEncoder, properties).run();

        verifyNoInteractions(adminRepository, passwordEncoder);
    }

    @Test
    void requiresBothUsernameAndPassword() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setUsername("admin");

        CommandLineRunner runner = configuration.bootstrapAdmin(adminRepository, passwordEncoder, properties);

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username and password");
    }

    @Test
    void createsConfiguredAdminOnlyOnce() throws Exception {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setUsername("admin");
        properties.setPassword("strong-password");
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");

        configuration.bootstrapAdmin(adminRepository, passwordEncoder, properties).run();

        verify(passwordEncoder).encode("strong-password");
        verify(adminRepository).save(any(Admin.class));

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(mock(Admin.class)));
        configuration.bootstrapAdmin(adminRepository, passwordEncoder, properties).run();
        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(passwordEncoder, times(1)).encode("strong-password");
    }
}
