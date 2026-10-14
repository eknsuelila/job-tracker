package com.portfolio.jobtracker.config;

import com.portfolio.jobtracker.security.JwtAuthenticationFilter;
import com.portfolio.jobtracker.security.JwtService;
import com.portfolio.jobtracker.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * All security rules of the application in one place.
 *
 * <p>{@code @Configuration} classes declare beans with {@code @Bean} methods: Spring calls each method
 * once at startup and keeps the returned object, injecting it wherever that type is needed.
 */
@Configuration
public class SecurityConfig {

    /** URLs anyone may call without a token. Everything else requires authentication. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"            // Spring's internal error page; must be public or errors turn into 401s
    };

    /**
     * The security filter chain: the list of checks every HTTP request goes through.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserDetailsService userDetailsService) throws Exception {
        return http
                // CSRF attacks abuse cookies that browsers send automatically. We authenticate with a
                // header the JavaScript adds explicitly (not a cookie), so CSRF protection isn't needed.
                .csrf(AbstractHttpConfigurer::disable)
                // Use the CorsConfigurationSource bean below (only matters when frontend and API
                // are on different origins, e.g. calling the API directly from localhost:4200).
                .cors(Customizer.withDefaults())
                // The H2 web console uses HTML frames; allow frames from the same origin (dev only).
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // STATELESS: never create an HTTP session. Each request must carry its own JWT.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules, checked top to bottom.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                // When an anonymous request hits a protected URL, answer a plain 401 (instead of the
                // default behaviour of redirecting to a login HTML page, which makes no sense for an API).
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Run our JWT filter before Spring's built-in username/password filter.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Tells Spring Security how to load a user by "username" (our email). Used by the JWT filter.
     * Written as a lambda because UserDetailsService has a single method: loadUserByUsername(String).
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * BCrypt hashes passwords with a random salt and a configurable cost (default 10 = 2^10 rounds),
     * so brute-forcing leaked hashes is very slow. Used at registration (encode) and login (matches).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS (Cross-Origin Resource Sharing): browsers block JavaScript on one origin (e.g. localhost:4200)
     * from reading responses from another (localhost:8080) unless the server allows it with headers.
     * Allowed origins come from app.cors.allowed-origins in application.yml.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
