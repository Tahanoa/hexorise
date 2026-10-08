package org.example.hexorise.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean InMemoryUserDetailsManager users(AdminProperties properties) {
        return new InMemoryUserDetailsManager(User.withUsername(properties.username())
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(properties.password())).roles("ADMIN").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        var contextRepository = new HttpSessionSecurityContextRepository();
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .httpBasic(basic -> basic.securityContextRepository(contextRepository))
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .build(); // CSRF remains enabled for browser-compatible Basic authentication.
    }
}
