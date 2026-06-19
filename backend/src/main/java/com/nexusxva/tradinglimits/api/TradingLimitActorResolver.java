package com.nexusxva.tradinglimits.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.tradebooking.domain.BookingActor;
import jakarta.servlet.http.HttpServletRequest;

final class TradingLimitActorResolver {

    private TradingLimitActorResolver() {
    }

    static BookingActor resolve(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        if (value instanceof AuthSession session) {
            return new BookingActor(
                    session.user().id(),
                    session.user().username(),
                    session.user().displayName()
            );
        }
        return BookingActor.system();
    }
}

