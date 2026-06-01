package com.unishare.api.infrastructure.googlemeet.dto;

import lombok.Builder;

@Builder
public record GoogleMeetCreateResult(
        String meetingUrl,
        String calendarEventId,
        String calendarHtmlLink) {}
