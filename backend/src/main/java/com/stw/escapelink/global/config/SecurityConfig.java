package com.stw.escapelink.global.config;

import com.stw.escapelink.global.security.AdminLoginFailureHandler;
import com.stw.escapelink.global.security.AdminLoginSuccessHandler;
import com.stw.escapelink.global.security.AdminLogoutSuccessHandler;
import com.stw.escapelink.global.security.RestAccessDeniedHandler;
import com.stw.escapelink.global.security.RestAuthenticationEntryPoint;
import com.stw.escapelink.global.security.TeamSessionAuthenticationFilter;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                         CorsProperties corsProperties,
                                                         RestAuthenticationEntryPoint entryPoint,
                                                         RestAccessDeniedHandler accessDeniedHandler,
                                                         AdminLoginSuccessHandler loginSuccessHandler,
                                                         AdminLoginFailureHandler loginFailureHandler,
                                                         AdminLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        // Session-cookie admin console: same CSRF reasoning as the participant API
        // chain (JSON-only state-changing endpoints + SameSite=Lax cookie) rather
        // than a CSRF token — kept consistent rather than mixing two models.
        http.securityMatcher("/api/admin/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginProcessingUrl("/api/admin/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**", "/docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/ws/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                       TeamSessionRepository teamSessionRepository,
                                                       TeamSessionProperties teamSessionProperties,
                                                       CorsProperties corsProperties,
                                                       RestAuthenticationEntryPoint entryPoint,
                                                       RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        TeamSessionAuthenticationFilter filter = new TeamSessionAuthenticationFilter(teamSessionRepository, teamSessionProperties);

        // JSON-only API secured by an HttpOnly, SameSite=Lax cookie: no forms can trigger
        // cross-site state changes with the right content type, so CSRF tokens are skipped.
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/team-sessions/join").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .anyRequest().hasRole("TEAM"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
