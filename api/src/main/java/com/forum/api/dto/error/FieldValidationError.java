package com.forum.api.dto.error;

public record FieldValidationError(String field, String message) {
}
