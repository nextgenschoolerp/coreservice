package com.kvs.core.config.audit;

import com.kvs.core.util.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Feeds Spring Data JPA Auditing with the email/username of the currently authenticated principal.
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserEmail();
    }
}
