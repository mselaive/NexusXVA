package com.nexusxva.notifications.application;

import com.nexusxva.notifications.domain.UserNotification;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradebooking.domain.TradeBookingType;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final String BO_GROUP = "BO";

    private final NotificationStore notificationStore;

    public NotificationService(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @Transactional(readOnly = true)
    public NotificationInbox inbox(UUID userId, boolean unreadOnly, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<UserNotification> notifications = notificationStore.findForUser(
                userId,
                unreadOnly,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "created_at"))
        );
        return new NotificationInbox(notifications, notificationStore.countUnread(userId));
    }

    @Transactional
    public UserNotification markRead(UUID notificationId, UUID userId) {
        return notificationStore.markRead(notificationId, userId, Instant.now())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationStore.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void notifyTradeBookingSubmitted(TradeBookingRequest booking) {
        String bookingDescription = booking.bookingType() == TradeBookingType.OPTION_STRATEGY
                ? "%s %s (%d legs)".formatted(booking.strategyType(), booking.underlyingSymbol(), booking.legs().size())
                : "%s %s %s".formatted(
                        booking.optionType(),
                        booking.underlyingSymbol(),
                        booking.quantity().stripTrailingZeros().toPlainString()
                );
        notificationStore.createForGroup(
                BO_GROUP,
                "TRADE_BOOKING_SUBMITTED",
                "New trade booking awaiting BO",
                "%s in %s was sent by %s.".formatted(
                        bookingDescription,
                        booking.portfolioName(),
                        booking.submittedBy().displayName()
                ),
                "/trade-validation",
                Instant.now()
        );
    }

    @Transactional
    public void notifyTradeBookingReviewed(TradeBookingRequest booking) {
        if (booking.submittedBy().userId() == null) {
            return;
        }
        String outcome = booking.status() == TradeBookingStatus.CONFIRMED ? "approved" : "rejected";
        String bookingDescription = booking.bookingType() == TradeBookingType.OPTION_STRATEGY
                ? "%s %s".formatted(booking.strategyType(), booking.underlyingSymbol())
                : "%s %s".formatted(booking.optionType(), booking.underlyingSymbol());
        notificationStore.createForUser(
                booking.submittedBy().userId(),
                "TRADE_BOOKING_" + booking.status().name(),
                "Trade booking %s".formatted(outcome),
                "%s in %s was %s by BO%s.".formatted(
                        bookingDescription,
                        booking.portfolioName(),
                        outcome,
                        booking.rejectionReason() == null ? "" : ": " + booking.rejectionReason()
                ),
                booking.portfolioId() == null ? "/fo-desk" : "/upad?portfolioId=" + booking.portfolioId(),
                Instant.now()
        );
    }

    @Transactional
    public void notifyLifecycleSubmitted(TradeLifecycleRequest request) {
        notificationStore.createForGroup(
                BO_GROUP,
                "LIFECYCLE_SUBMITTED",
                "Lifecycle request awaiting BO",
                "%s request for %s in %s was sent by %s.".formatted(
                        request.requestType(),
                        request.originalUnderlyingSymbol(),
                        request.portfolioName(),
                        request.submittedBy().displayName()
                ),
                "/trade-validation",
                Instant.now()
        );
    }

    @Transactional
    public void notifyLifecycleReviewed(TradeLifecycleRequest request) {
        if (request.submittedBy().userId() == null) {
            return;
        }
        String outcome = request.status() == TradeLifecycleRequestStatus.APPROVED ? "approved" : "rejected";
        notificationStore.createForUser(
                request.submittedBy().userId(),
                "LIFECYCLE_" + request.status().name(),
                "Lifecycle request %s".formatted(outcome),
                "%s request for %s in %s was %s%s.".formatted(
                        request.requestType(),
                        request.originalUnderlyingSymbol(),
                        request.portfolioName(),
                        outcome,
                        request.rejectionReason() == null ? "" : ": " + request.rejectionReason()
                ),
                request.portfolioId() == null ? "/fo-desk" : "/portfolios?portfolioId=" + request.portfolioId(),
                Instant.now()
        );
    }
}
