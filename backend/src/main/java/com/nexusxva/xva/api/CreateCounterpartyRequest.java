package com.nexusxva.xva.api;

import com.nexusxva.xva.application.CreateCounterpartyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCounterpartyRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 80) String externalId,
        @Size(max = 40) String creditRating
) {

    CreateCounterpartyCommand toCommand() {
        return new CreateCounterpartyCommand(name, externalId, creditRating);
    }
}
