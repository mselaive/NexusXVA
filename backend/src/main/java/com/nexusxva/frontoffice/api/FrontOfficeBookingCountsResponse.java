package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeDeskService.FrontOfficeBookingCounts;

public record FrontOfficeBookingCountsResponse(
        long pendingValidation,
        long confirmed,
        long rejected,
        long total
) {

    static FrontOfficeBookingCountsResponse from(FrontOfficeBookingCounts counts) {
        return new FrontOfficeBookingCountsResponse(
                counts.pendingValidation(),
                counts.confirmed(),
                counts.rejected(),
                counts.total()
        );
    }
}
