package com.kvs.core.config.tracing;

import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter and Brave CurrentTraceContext configuration responsible for traceId and spanId lifecycle:
 * 1. Checks request headers for trace/span IDs (e.g. X-Trace-Id, X-Span-Id, X-Correlation-Id, traceId, spanId).
 * 2. If present, sets them into MDC and ensures Brave's trace context preserves them across all internal
 *    observations (such as Spring Security's ObservationFilterChainDecorator and Controller observations).
 * 3. If absent, falls back to active Micrometer/Brave tracer or generates compliant IDs.
 * 4. Echoes traceId and spanId back into HTTP response headers.
 * 5. Cleans up MDC and thread-local state on request completion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceCorrelationFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String SPAN_HEADER = "X-Span-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";

    private static final ThreadLocal<String> CUSTOM_TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CUSTOM_SPAN_ID = new ThreadLocal<>();

    @Autowired(required = false)
    private Tracer tracer;

    public static String getCustomTraceId() {
        return CUSTOM_TRACE_ID.get();
    }

    public static String getCustomSpanId() {
        return CUSTOM_SPAN_ID.get();
    }

    public static void setCustomTrace(String traceId, String spanId) {
        CUSTOM_TRACE_ID.set(traceId);
        CUSTOM_SPAN_ID.set(spanId);
    }

    public static void clearCustomTrace() {
        CUSTOM_TRACE_ID.remove();
        CUSTOM_SPAN_ID.remove();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String customTrace = resolveHeader(request, TRACE_HEADER, CORRELATION_HEADER, "traceId");
        String customSpan = resolveHeader(request, SPAN_HEADER, "spanId");

        String traceId;
        String spanId;

        if (StringUtils.hasText(customTrace) || StringUtils.hasText(customSpan)) {
            traceId = StringUtils.hasText(customTrace) ? customTrace : generateHexId(32);
            spanId = StringUtils.hasText(customSpan) ? customSpan : generateHexId(16);
            setCustomTrace(traceId, spanId);
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);
            if (tracer != null && tracer.currentSpan() != null) {
                tracer.currentSpan().tag("custom.traceId", traceId);
                tracer.currentSpan().tag("custom.spanId", spanId);
            }
        } else {
            traceId = (tracer != null && tracer.currentSpan() != null)
                    ? tracer.currentSpan().context().traceId()
                    : MDC.get(MDC_TRACE_ID);
            if (!StringUtils.hasText(traceId)) {
                traceId = generateHexId(32);
            }
            spanId = (tracer != null && tracer.currentSpan() != null)
                    ? tracer.currentSpan().context().spanId()
                    : MDC.get(MDC_SPAN_ID);
            if (!StringUtils.hasText(spanId)) {
                spanId = generateHexId(16);
            }
            setCustomTrace(traceId, spanId);
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);
        }

        request.setAttribute(TRACE_HEADER, traceId);
        request.setAttribute(SPAN_HEADER, spanId);

        // Echo back in response headers for client / gateway correlation
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(SPAN_HEADER, spanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            clearCustomTrace();
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
        }
    }

    private String resolveHeader(HttpServletRequest request, String... headerNames) {
        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String generateHexId(int length) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (uuid.length() >= length) ? uuid.substring(0, length) : uuid;
    }

    @Configuration(proxyBeanMethods = false)
    static class BraveMdcAutoConfiguration {

        /**
         * Replaces default braveCurrentTraceContext to guarantee that when a custom traceId
         * or spanId is present in the request (e.g. X-Trace-Id / X-Span-Id), it is maintained in MDC
         * across all internal observation scopes (including Spring Security and Controller observation handlers).
         * If no custom header is present, it uses Brave's native traceId and spanId.
         */
        @Bean
        public CurrentTraceContext braveCurrentTraceContext() {
            CurrentTraceContext.ScopeDecorator mdcScopeDecorator = (TraceContext currentSpan, Scope scope) -> {
                String customTraceId = getCustomTraceId();
                String customSpanId = getCustomSpanId();

                String traceId = (customTraceId != null) ? customTraceId : (currentSpan != null ? currentSpan.traceIdString() : null);
                String spanId = (customSpanId != null) ? customSpanId : (currentSpan != null ? currentSpan.spanIdString() : null);

                String previousTraceId = MDC.get(MDC_TRACE_ID);
                String previousSpanId = MDC.get(MDC_SPAN_ID);

                if (traceId != null) {
                    MDC.put(MDC_TRACE_ID, traceId);
                }
                if (spanId != null) {
                    MDC.put(MDC_SPAN_ID, spanId);
                }

                return () -> {
                    scope.close();
                    if (customTraceId != null) {
                        MDC.put(MDC_TRACE_ID, customTraceId);
                    } else if (previousTraceId != null) {
                        MDC.put(MDC_TRACE_ID, previousTraceId);
                    } else {
                        MDC.remove(MDC_TRACE_ID);
                    }

                    if (customSpanId != null) {
                        MDC.put(MDC_SPAN_ID, customSpanId);
                    } else if (previousSpanId != null) {
                        MDC.put(MDC_SPAN_ID, previousSpanId);
                    } else {
                        MDC.remove(MDC_SPAN_ID);
                    }
                };
            };

            return ThreadLocalCurrentTraceContext.newBuilder()
                    .addScopeDecorator(mdcScopeDecorator)
                    .build();
        }
    }
}
