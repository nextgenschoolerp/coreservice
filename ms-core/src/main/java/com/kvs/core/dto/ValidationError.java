package com.kvs.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Details of field-level validation constraint violations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    private String field;
    private Object rejectedValue;
    private String message;
}
