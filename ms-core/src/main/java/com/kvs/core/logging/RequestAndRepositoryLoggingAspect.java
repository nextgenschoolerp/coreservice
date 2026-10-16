package com.kvs.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class RequestAndRepositoryLoggingAspect {

    private final LogDataSanitizer sanitizer;

    public RequestAndRepositoryLoggingAspect(ObjectMapper objectMapper) {
        this.sanitizer = new LogDataSanitizer(objectMapper);
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        String requestDescription = describeRequest(attributes);
        String body = sanitizer.sanitize(Arrays.stream(joinPoint.getArgs())
                .filter(this::isLoggableArgument)
                .toList());

        log.debug("HTTP request {} {} body={}",
                joinPoint.getSignature().toShortString(), requestDescription, body);

        Object result = joinPoint.proceed();
        log.debug("HTTP response {} result={}",
                joinPoint.getSignature().toShortString(), sanitizer.sanitize(result));
        return result;
    }

    @Around("bean(*Repository)")
    public Object logRepositoryResult(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        log.debug("Repository result {}.{} = {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                sanitizer.sanitize(result));
        return result;
    }

    private String describeRequest(RequestAttributes attributes) {
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return "[no servlet request]";
        }

        var request = servletAttributes.getRequest();
        return request.getMethod() + " "
                + sanitizer.sanitizeUrl(request.getRequestURL().toString(), request.getQueryString());
    }

    private boolean isLoggableArgument(Object argument) {
        return argument != null
                && !(argument instanceof ServletRequest)
                && !(argument instanceof ServletResponse);
    }
}
