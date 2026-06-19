package com.nexusxva.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActiveGroupRequest(
        @NotBlank @Size(max = 32) String group
) {
}
