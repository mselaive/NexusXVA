package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.domain.BookingActor;
import java.util.UUID;

public record BookingActorResponse(
        UUID userId,
        String username,
        String displayName
) {

    static BookingActorResponse from(BookingActor actor) {
        if (actor == null) {
            return null;
        }
        return new BookingActorResponse(actor.userId(), actor.username(), actor.displayName());
    }
}

