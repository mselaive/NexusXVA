package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import java.util.List;
import org.springframework.data.domain.Page;

public record TradeBookingPageResponse(
        List<TradeBookingResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static TradeBookingPageResponse from(Page<TradeBookingRequest> bookings) {
        return new TradeBookingPageResponse(
                bookings.getContent().stream().map(TradeBookingResponse::from).toList(),
                bookings.getNumber(),
                bookings.getSize(),
                bookings.getTotalElements(),
                bookings.getTotalPages()
        );
    }
}

