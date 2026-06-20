package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeDeskService.FrontOfficeDesk;
import java.util.List;

public record FrontOfficeDeskResponse(
        FrontOfficeDeskUserResponse user,
        FrontOfficeBookingCountsResponse bookingCounts,
        List<FrontOfficePortfolioSummaryResponse> portfolios,
        List<FrontOfficeDeskBookingResponse> bookings
) {

    static FrontOfficeDeskResponse from(FrontOfficeDesk desk) {
        return new FrontOfficeDeskResponse(
                FrontOfficeDeskUserResponse.from(desk.user()),
                FrontOfficeBookingCountsResponse.from(desk.bookingCounts()),
                desk.portfolios().stream().map(FrontOfficePortfolioSummaryResponse::from).toList(),
                desk.bookings().stream().map(FrontOfficeDeskBookingResponse::from).toList()
        );
    }
}
