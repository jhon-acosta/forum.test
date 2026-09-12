package com.forum.api.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiscussionRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 10000) String content) {
}
