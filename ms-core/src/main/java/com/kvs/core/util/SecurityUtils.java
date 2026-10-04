package com.kvs.core.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility methods for accessing the Spring SecurityContextHolder.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static Optional<Authentication> getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    public static Optional<String> getCurrentUserEmail() {
        return getAuthentication().map(Authentication::getName);
    }

    public static List<String> getCurrentUserRoles() {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentUserRoles().contains(targetRole);
    }
}
