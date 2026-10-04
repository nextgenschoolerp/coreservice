package com.kvs.core.config.tracing;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Client interceptor that attaches traceId and spanId headers to outgoing HTTP requests.
 */
public class TracePropagationClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    @NonNull
    public ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {

        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");

        if (StringUtils.hasText(traceId) && !request.getHeaders().containsKey("X-Trace-Id")) {
            request.getHeaders().add("X-Trace-Id", traceId);
        }
        if (StringUtils.hasText(spanId) && !request.getHeaders().containsKey("X-Span-Id")) {
            request.getHeaders().add("X-Span-Id", spanId);
        }

        return execution.execute(request, body);
    }
}
