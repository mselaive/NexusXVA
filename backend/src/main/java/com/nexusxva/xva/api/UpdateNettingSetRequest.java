package com.nexusxva.xva.api;

import com.nexusxva.xva.application.UpdateNettingSetCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNettingSetRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull Boolean active
) {

    UpdateNettingSetCommand toCommand() {
        return new UpdateNettingSetCommand(name, active);
    }
}
