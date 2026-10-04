package com.kvs.core.config.audit;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenient meta-annotation to enable Spring Data JPA Auditing using the
 * framework's default {@link SpringSecurityAuditorAware} auditor provider.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public @interface EnableCoreJpaAuditing {
}
