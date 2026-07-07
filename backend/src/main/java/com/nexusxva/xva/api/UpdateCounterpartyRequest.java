package com.nexusxva.xva.api;

import com.nexusxva.xva.application.UpdateCounterpartyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCounterpartyRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 80) String externalId,
        @Size(max = 40) String creditRating,
        @NotNull Boolean active
) {

    UpdateCounterpartyCommand toCommand() {
        return new UpdateCounterpartyCommand(name, externalId, creditRating, active);
    }
}
