package com.unishare.api.infrastructure.googlemeet;

import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateCommand;
import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateResult;

public interface GoogleMeetService {

    boolean isEnabled();

    GoogleMeetCreateResult createMeeting(GoogleMeetCreateCommand command);
}
