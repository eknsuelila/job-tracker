package com.portfolio.jobtracker.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates each HTTP request from its JWT.
 *
 * <p><b>Where it runs:</b> every request passes through a chain of servlet filters before reaching a
 * controller. {@code SecurityConfig} inserts this filter into Spring Security's chain. For each request:
 * <ol>
 *   <li>Read the header {@code Authorization: Bearer <token>}.</li>
 *   <li>Validate the token and get the email from it ({@link JwtService#extractEmail}).</li>
 *   <li>Load the user from the database.</li>
 *   <li>Store an "Authentication" object in the {@code SecurityContextHolder} (a per-request,
 *       thread-local holder). From now on Spring Security treats the request as logged in, and
 *       {@code @AuthenticationPrincipal} in controllers returns this user.</li>
 * </ol>
 * If there is no token or it is invalid, nothing is stored; the request continues anonymously and
 * Spring Security's authorization rules answer 401 for protected endpoints.
 *
 * <p>{@link OncePerRequestFilter} guarantees the filter runs only once per request.
 * This class is deliberately NOT a {@code @Component}: Spring Boot would otherwise ALSO register it
 * as a normal servlet filter, outside the security chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractEmail(header.substring(BEARER_PREFIX.length()));
                // Loading the user from the DB (instead of trusting the token alone) means a deleted
                // account stops working immediately, even if its token hasn't expired yet.
                UserDetails user = userDetailsService.loadUserByUsername(email);
                // Arguments: principal (the user), credentials (null: no password needed any more), roles.
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                // Invalid/expired token or unknown user: continue unauthenticated.
                // Protected endpoints will then answer 401 Unauthorized.
            }
        }
        // ALWAYS pass the request on to the next filter (and finally the controller).
        chain.doFilter(request, response);
    }
}
