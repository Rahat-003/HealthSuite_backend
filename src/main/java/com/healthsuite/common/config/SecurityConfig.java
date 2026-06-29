package com.healthsuite.common.config;

import com.healthsuite.auth.security.JwtAuthenticationFilter;
import com.healthsuite.auth.security.ShareTokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ShareTokenAuthenticationFilter shareTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI & OpenAPI spec
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        // Public auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // Doctor directory — public read
                        .requestMatchers(HttpMethod.GET, "/api/doctors/**").permitAll()
                        // Share-token based record access — validation done in service layer
                        .requestMatchers(HttpMethod.GET, "/api/phr/shared/**").permitAll()
                        // Local file storage — served without auth
                        .requestMatchers("/files/**").permitAll()
                        // Actuator health
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Support-only ticket operations
                        .requestMatchers("/api/concierge/tickets/queue").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.PUT, "/api/concierge/tickets/*/claim").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.PUT, "/api/concierge/tickets/*/confirm").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.PUT, "/api/concierge/tickets/*/cancel").hasRole("SUPPORT")
                        // Admin-only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // ShareToken filter runs first so JWT filter can skip if already authenticated
                .addFilterBefore(shareTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
