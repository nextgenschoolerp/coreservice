package com.kvs.core.config.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringSecurityAuditorAwareTest {

    private SpringSecurityAuditorAware auditorAware;

    @BeforeEach
    void setUp() {
        auditorAware = new SpringSecurityAuditorAware();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return 'system' when SecurityContext is empty")
    void shouldReturnDefaultSystemAuditorWhenContextEmpty() {
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent());
        assertEquals(SpringSecurityAuditorAware.DEFAULT_SYSTEM_AUDITOR, auditor.get());
    }

    @Test
    @DisplayName("Should return 'system' when Authentication is Anonymous")
    void shouldReturnDefaultSystemAuditorWhenAnonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "anonKey", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent());
        assertEquals("system", auditor.get());
    }

    @Test
    @DisplayName("Should return authenticated principal email/username when authenticated")
    void shouldReturnAuthenticatedUsernameWhenPresent() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice@example.com", "credentials", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent());
        assertEquals("alice@example.com", auditor.get());
    }

    @Test
    @DisplayName("Should return custom default auditor when provided and unauthenticated")
    void shouldReturnCustomDefaultAuditorWhenUnauthenticated() {
        SpringSecurityAuditorAware customAuditorAware = new SpringSecurityAuditorAware("batch-job");

        Optional<String> auditor = customAuditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent());
        assertEquals("batch-job", auditor.get());
    }
}
