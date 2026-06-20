package com.nexusxva.frontoffice.application;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontOfficeDeskService {

    private final PortfolioService portfolioService;
    private final TradeBookingService tradeBookingService;
    private final UserAccessService userAccessService;

    public FrontOfficeDeskService(
            PortfolioService portfolioService,
            TradeBookingService tradeBookingService,
            UserAccessService userAccessService
    ) {
        this.portfolioService = portfolioService;
        this.tradeBookingService = tradeBookingService;
        this.userAccessService = userAccessService;
    }

    @Transactional(readOnly = true)
    public FrontOfficeDesk getDesk(AuthSession session, HttpServletRequest request) {
        List<PortfolioSummary> visiblePortfolios = userAccessService.filterVisiblePortfolios(
                request,
                portfolioService.listPortfolios()
        );
        Set<UUID> visiblePortfolioIds = visiblePortfolios.stream()
                .map(PortfolioSummary::id)
                .collect(Collectors.toSet());
        List<FrontOfficeDeskBooking> bookings = tradeBookingService.mine(actor(session))
                .stream()
                .map(booking -> FrontOfficeDeskBooking.from(
                        booking,
                        booking.portfolioId() != null && visiblePortfolioIds.contains(booking.portfolioId())
                ))
                .toList();

        return new FrontOfficeDesk(
                FrontOfficeDeskUser.from(session),
                FrontOfficeBookingCounts.from(bookings),
                visiblePortfolios,
                bookings
        );
    }

    private BookingActor actor(AuthSession session) {
        if (session == null) {
            return BookingActor.system();
        }
        return new BookingActor(
                session.user().id(),
                session.user().username(),
                session.user().displayName()
        );
    }

    public record FrontOfficeDesk(
            FrontOfficeDeskUser user,
            FrontOfficeBookingCounts bookingCounts,
            List<PortfolioSummary> portfolios,
            List<FrontOfficeDeskBooking> bookings
    ) {
    }

    public record FrontOfficeDeskUser(
            UUID id,
            String username,
            String displayName
    ) {
        static FrontOfficeDeskUser from(AuthSession session) {
            if (session == null) {
                return new FrontOfficeDeskUser(null, "system", "System");
            }
            return new FrontOfficeDeskUser(
                    session.user().id(),
                    session.user().username(),
                    session.user().displayName()
            );
        }
    }

    public record FrontOfficeBookingCounts(
            long pendingValidation,
            long confirmed,
            long rejected,
            long total
    ) {
        static FrontOfficeBookingCounts from(List<FrontOfficeDeskBooking> bookings) {
            return new FrontOfficeBookingCounts(
                    count(bookings, TradeBookingStatus.PENDING_VALIDATION),
                    count(bookings, TradeBookingStatus.CONFIRMED),
                    count(bookings, TradeBookingStatus.REJECTED),
                    bookings.size()
            );
        }

        private static long count(List<FrontOfficeDeskBooking> bookings, TradeBookingStatus status) {
            return bookings.stream()
                    .filter(booking -> booking.booking().status() == status)
                    .count();
        }
    }

    public record FrontOfficeDeskBooking(
            TradeBookingRequest booking,
            boolean portfolioVisible
    ) {
        static FrontOfficeDeskBooking from(TradeBookingRequest booking, boolean portfolioVisible) {
            return new FrontOfficeDeskBooking(booking, portfolioVisible);
        }
    }
}
