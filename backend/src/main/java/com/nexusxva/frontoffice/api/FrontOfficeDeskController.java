package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.frontoffice.application.FrontOfficeDeskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office")
public class FrontOfficeDeskController {

    private final FrontOfficeDeskService service;

    public FrontOfficeDeskController(FrontOfficeDeskService service) {
        this.service = service;
    }

    @GetMapping("/desk")
    public FrontOfficeDeskResponse desk(HttpServletRequest request) {
        return FrontOfficeDeskResponse.from(service.getDesk(currentSession(request), request));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
