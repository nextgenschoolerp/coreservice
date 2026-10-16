package com.kvs.core.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LogDataSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final int MAX_LOG_LENGTH = 8_000;
    private static final Set<String> SENSITIVE_FIELD_PARTS = Set.of(
            "password", "token", "secret", "authorization", "cookie",
            "credential", "otp", "mfa", "privatekey", "apikey", "backupcode",
            "salt", "hash"
    );

    private final ObjectMapper objectMapper;

    public LogDataSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String sanitize(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            JsonNode tree = objectMapper.valueToTree(value);
            redact(tree);
            String serialized = objectMapper.writeValueAsString(tree);
            if (serialized.length() <= MAX_LOG_LENGTH) {
                return serialized;
            }
            return serialized.substring(0, MAX_LOG_LENGTH) + "...[TRUNCATED]";
        } catch (IllegalArgumentException exception) {
            return "[unserializable " + value.getClass().getName() + "]";
        } catch (Exception exception) {
            return "[serialization failed for " + value.getClass().getName() + "]";
        }
    }

    public String sanitizeUrl(String url, String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return url;
        }

        StringBuilder sanitizedQuery = new StringBuilder();
        for (String parameter : queryString.split("&")) {
            if (sanitizedQuery.length() > 0) {
                sanitizedQuery.append('&');
            }

            int separator = parameter.indexOf('=');
            if (separator < 0) {
                sanitizedQuery.append(parameter).append('=').append(REDACTED);
            } else {
                sanitizedQuery.append(parameter, 0, separator)
                        .append('=')
                        .append(REDACTED);
            }
        }
        return url + "?" + sanitizedQuery;
    }

    private void redact(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), REDACTED);
                } else {
                    redact(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
    }

    private boolean isSensitive(String name) {
        String normalized = name.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_PARTS.stream().anyMatch(normalized::contains);
    }
}
