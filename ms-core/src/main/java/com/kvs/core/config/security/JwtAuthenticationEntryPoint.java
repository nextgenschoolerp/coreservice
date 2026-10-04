package com.kvs.core.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kvs.core.config.tracing.TraceCorrelationFilter;
import com.kvs.core.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * AuthenticationEntryPoint that writes a standardized ApiErrorResponse JSON on 401 Unauthorized.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String traceId = null;
        if (response != null) {
            traceId = response.getHeader(TraceCorrelationFilter.TRACE_HEADER);
        }
        if (!org.springframework.util.StringUtils.hasText(traceId) && request != null) {
            traceId = (String) request.getAttribute(TraceCorrelationFilter.TRACE_HEADER);
        }
        if (!org.springframework.util.StringUtils.hasText(traceId)) {
            traceId = TraceCorrelationFilter.getCustomTraceId();
        }
        if (!org.springframework.util.StringUtils.hasText(traceId)) {
            traceId = org.slf4j.MDC.get(TraceCorrelationFilter.MDC_TRACE_ID);
        }
        if (!org.springframework.util.StringUtils.hasText(traceId)) {
            traceId = java.util.UUID.randomUUID().toString().replace("-", "");
        }

        response.setHeader(TraceCorrelationFilter.TRACE_HEADER, traceId);
        org.slf4j.MDC.put(TraceCorrelationFilter.MDC_TRACE_ID, traceId);
        TraceCorrelationFilter.setCustomTrace(traceId, TraceCorrelationFilter.getCustomSpanId());

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode("UNAUTHORIZED")
                .message(authException != null && authException.getMessage() != null
                        ? authException.getMessage()
                        : "Full authentication is required to access this resource")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
