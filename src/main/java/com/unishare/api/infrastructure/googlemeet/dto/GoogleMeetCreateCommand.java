package com.unishare.api.infrastructure.googlemeet.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record GoogleMeetCreateCommand(
        UUID mentorUserId,
        String title,
        String description,
        Instant scheduledAt,
        Instant scheduledAtEnd,
        List<String> attendeeEmails) {}
