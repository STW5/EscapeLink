package com.stw.escapelink.global.config;

import com.stw.escapelink.admin.domain.Admin;
import com.stw.escapelink.admin.domain.AdminRole;
import com.stw.escapelink.admin.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
public class AdminBootstrapConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfiguration.class);

    @Bean
    CommandLineRunner bootstrapAdmin(AdminRepository adminRepository,
                                     PasswordEncoder passwordEncoder,
                                     AdminBootstrapProperties properties) {
        return args -> {
            boolean hasUsername = StringUtils.hasText(properties.getUsername());
            boolean hasPassword = StringUtils.hasText(properties.getPassword());

            if (!hasUsername && !hasPassword) {
                log.warn("No bootstrap admin configured; the admin console will have no initial login account");
                return;
            }
            if (!hasUsername || !hasPassword) {
                throw new IllegalStateException("Both bootstrap admin username and password must be configured");
            }

            if (adminRepository.findByUsername(properties.getUsername()).isPresent()) {
                log.info("Bootstrap admin already exists: username={}", properties.getUsername());
                return;
            }

            adminRepository.save(new Admin(
                    properties.getUsername(),
                    passwordEncoder.encode(properties.getPassword()),
                    AdminRole.ADMIN));
            log.info("Bootstrap admin created: username={}", properties.getUsername());
        };
    }
}
