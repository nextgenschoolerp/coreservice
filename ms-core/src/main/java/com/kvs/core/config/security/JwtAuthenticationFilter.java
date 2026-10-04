package com.kvs.core.config.security;

import com.kvs.core.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Filter that parses and validates Bearer JWT tokens from the Authorization header.
 * Populates SecurityContextHolder with the authenticated user and authorities.
 * Decoupled from user service repositories by using standard Spring Security UserDetailsService
 * and/or JWT claims.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = null;
    }

    @Autowired
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, Optional<UserDetailsService> userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService.orElse(null);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            final String jwt = authHeader.substring(7).trim();
            try {
                jwtTokenService.validateToken(jwt);
                final String userEmail = jwtTokenService.extractUsername(jwt);

                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (StringUtils.hasText(userEmail) && (currentAuth == null || !currentAuth.isAuthenticated() || currentAuth instanceof AnonymousAuthenticationToken)) {
                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // 1. Try extracting authorities from JWT claims (roles / role / authorities)
                    try {
                        Claims claims = jwtTokenService.extractAllClaims(jwt);
                        authorities.addAll(extractAuthoritiesFromClaims(claims));
                    } catch (Exception ex) {
                        log.debug("Could not extract authorities from token claims: {}", ex.getMessage());
                    }

                    // 2. If token did not provide authorities, attempt loading via optional UserDetailsService
                    if (authorities.isEmpty() && userDetailsService != null) {
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                            if (userDetails != null && userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
                                authorities.addAll(userDetails.getAuthorities());
                            }
                        } catch (Exception ex) {
                            log.debug("UserDetailsService could not load authorities for user {}: {}", userEmail, ex.getMessage());
                        }
                    }

                    // 3. Fallback to default ROLE_USER if no authorities resolved
                    if (authorities.isEmpty()) {
                        authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user '{}' for URI '{}'", userEmail, request.getRequestURI());
                }
            } catch (Exception ex) {
                log.warn("JWT validation failed for URI '{}': {}", request.getRequestURI(), ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> extractAuthoritiesFromClaims(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (claims == null) {
            return authorities;
        }

        Object rolesClaim = claims.get("roles");
        if (rolesClaim == null) {
            rolesClaim = claims.get("role");
        }
        if (rolesClaim == null) {
            rolesClaim = claims.get("authorities");
        }

        if (rolesClaim instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null) {
                    String roleStr = item.toString().trim();
                    if (!roleStr.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr));
                    }
                }
            }
        } else if (rolesClaim instanceof String str && StringUtils.hasText(str)) {
            for (String role : str.split(",")) {
                String trimmed = role.trim();
                if (!trimmed.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed));
                }
            }
        }

        return authorities;
    }
}
