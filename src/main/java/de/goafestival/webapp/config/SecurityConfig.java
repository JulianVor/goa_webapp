package de.goafestival.webapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Protects the admin area behind a single configurable login (see
 * {@code app.admin.username} / {@code app.admin.password}). Everything else -
 * the public festival pages, static assets and uploaded images - stays open,
 * unless {@code app.test-mode.enabled} adds a whole-site password gate in
 * front of all of it (see {@link SiteLockFilter}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:admin}") String rawPassword,
            PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(rawPassword))
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${app.test-mode.enabled:false}") boolean testModeEnabled,
            @Value("${app.test-mode.username:}") String testModeUsername,
            @Value("${app.test-mode.password:}") String testModePassword) throws Exception {
        if (testModeEnabled) {
            http.addFilterBefore(new SiteLockFilter(testModeUsername, testModePassword),
                    UsernamePasswordAuthenticationFilter.class);
        }
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());
        return http.build();
    }
}
