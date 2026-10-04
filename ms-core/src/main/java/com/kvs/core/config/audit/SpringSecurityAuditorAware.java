package com.kvs.core.config.audit;

import com.kvs.core.util.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Feeds Spring Data JPA Auditing with the email/username of the currently authenticated principal.
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    public static final String DEFAULT_SYSTEM_AUDITOR = "system";

    private final String defaultAuditor;

    public SpringSecurityAuditorAware() {
        this(DEFAULT_SYSTEM_AUDITOR);
    }

    public SpringSecurityAuditorAware(String defaultAuditor) {
        this.defaultAuditor = defaultAuditor;
    }

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserEmail()
                .or(() -> Optional.ofNullable(defaultAuditor));
    }
}

