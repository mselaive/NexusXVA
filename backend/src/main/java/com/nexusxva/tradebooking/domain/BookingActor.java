package com.nexusxva.tradebooking.domain;

import java.util.UUID;

public record BookingActor(
        UUID userId,
        String username,
        String displayName
) {

    public static BookingActor system() {
        return new BookingActor(null, "system", "System");
    }
}

