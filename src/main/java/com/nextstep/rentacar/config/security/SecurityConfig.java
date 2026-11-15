package com.nextstep.rentacar.config.security;

import com.nextstep.rentacar.service.auth.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.nextstep.rentacar.config.security.Permission.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProblemDetailSecurityHandler problemDetailSecurityHandler;

    /** Origins allowed to call the API from a browser on another origin (the UI's dev server proxies instead). */
    @Value("${cors.allowed-origins:}")
    private List<String> allowedOrigins;

    /**
     * Real security: every profile except the development ones served by {@link #localSecurityFilterChain}.
     * Rules check permissions; which role has which is defined in {@link RolePermissions}.
     */
    @Bean
    @Profile("!local & !test & !docker")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemDetailSecurityHandler)
                .accessDeniedHandler(problemDetailSecurityHandler))
            .authorizeHttpRequests(auth -> auth
                // Let error dispatches through so callers see the real status (e.g. 400) instead of 403
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Public endpoints (me and change-password need the caller's identity; refresh and logout use the refresh cookie)
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/change-password").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Also permit custom OpenAPI path configured in application.yml
                .requestMatchers("/api-docs", "/api-docs/**").permitAll()
                
                // Branches: anyone signed in can browse
                .requestMatchers(HttpMethod.GET, "/api/v1/branches/**").authenticated()
                .requestMatchers("/api/v1/branches/**").hasAuthority(BRANCH_MANAGE.name())

                // Cars: anyone signed in can browse; deleted cars and changes need CAR_MANAGE
                .requestMatchers(HttpMethod.GET, "/api/v1/cars/deleted").hasAuthority(CAR_MANAGE.name())
                .requestMatchers(HttpMethod.GET, "/api/v1/cars/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/cars/**").hasAuthority(CAR_DELETE.name())
                .requestMatchers(HttpMethod.POST, "/api/v1/cars/*/restore").hasAuthority(CAR_DELETE.name())
                .requestMatchers("/api/v1/cars/**").hasAuthority(CAR_MANAGE.name())

                // Settings: anyone signed in can read them (pages need e.g. the mileage unit)
                .requestMatchers(HttpMethod.GET, "/api/v1/settings").authenticated()
                .requestMatchers("/api/v1/settings/**").hasAuthority(SETTINGS_MANAGE.name())

                .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasAuthority(CUSTOMER_DELETE.name())
                .requestMatchers("/api/v1/customers/**").hasAuthority(CUSTOMER_MANAGE.name())

                .requestMatchers("/api/v1/reservations/**").hasAuthority(RESERVATION_MANAGE.name())

                // Maintenance: seeing jobs and scheduling new ones is for all staff; running them needs MAINTENANCE_MANAGE
                .requestMatchers(HttpMethod.GET, "/api/v1/maintenance/**").hasAuthority(MAINTENANCE_SCHEDULE.name())
                .requestMatchers(HttpMethod.POST, "/api/v1/maintenance/schedule").hasAuthority(MAINTENANCE_SCHEDULE.name())
                .requestMatchers("/api/v1/maintenance/**").hasAuthority(MAINTENANCE_MANAGE.name())

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // H2 Console configuration (for dev environment)
        http.headers(headers -> headers.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }

    /**
     * Development chain: everything is open except endpoints that need the caller's identity.
     * 'docker' is local development with PostgreSQL, so it uses this chain too (the UI has no login yet).
     */
    @Bean
    @Profile({"local", "test", "docker"})
    @Order(0)
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemDetailSecurityHandler)
                .accessDeniedHandler(problemDetailSecurityHandler))
            .authorizeHttpRequests(a -> a
                // These endpoints need the caller's identity
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/change-password").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * The JWT filter is a bean (so it can be injected into the security chains), which would also make
     * Spring Boot register it as a plain servlet filter on every request. It must only run inside the
     * security filter chains, so disable that extra registration.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Exact origins only: requests carry credentials, so any origin ("*") would let any site use them
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
