package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeDeskService.FrontOfficeDeskUser;
import java.util.UUID;

public record FrontOfficeDeskUserResponse(
        UUID id,
        String username,
        String displayName
) {

    static FrontOfficeDeskUserResponse from(FrontOfficeDeskUser user) {
        return new FrontOfficeDeskUserResponse(user.id(), user.username(), user.displayName());
    }
}
