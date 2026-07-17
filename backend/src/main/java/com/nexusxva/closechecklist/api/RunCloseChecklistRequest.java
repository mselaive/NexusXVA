package com.nexusxva.closechecklist.api;

import java.time.LocalDate;

public record RunCloseChecklistRequest(
        LocalDate businessDate
) {
}
