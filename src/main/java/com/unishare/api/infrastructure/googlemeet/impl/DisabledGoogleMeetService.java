package com.unishare.api.infrastructure.googlemeet.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.googlemeet.GoogleMeetService;
import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateCommand;
import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateResult;
import com.unishare.api.infrastructure.googlemeet.exception.GoogleMeetErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.google.meet", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledGoogleMeetService implements GoogleMeetService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public GoogleMeetCreateResult createMeeting(GoogleMeetCreateCommand command) {
        throw new AppException(
                GoogleMeetErrorCode.NOT_CONFIGURED,
                "Google Meet chưa được cấu hình. Bật GOOGLE_MEET_ENABLED và GOOGLE_MEET_OAUTH_CLIENT_PATH (file client_secret_*.json).");
    }
}
