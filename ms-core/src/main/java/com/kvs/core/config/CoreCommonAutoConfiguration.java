package com.kvs.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.config.audit.SpringSecurityAuditorAware;
import com.kvs.core.config.security.JwtAuthenticationEntryPoint;
import com.kvs.core.config.tracing.TracePropagationClientInterceptor;
import com.kvs.core.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for core-framework common web, exception handling, and auditing components.
 */
@AutoConfiguration
public class CoreCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationEntryPoint.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(TracePropagationClientInterceptor.class)
    public TracePropagationClientInterceptor tracePropagationClientInterceptor() {
        return new TracePropagationClientInterceptor();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.domain.AuditorAware")
    static class JpaAuditingConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "springSecurityAuditorAware")
        public SpringSecurityAuditorAware springSecurityAuditorAware() {
            return new SpringSecurityAuditorAware();
        }
    }
}
