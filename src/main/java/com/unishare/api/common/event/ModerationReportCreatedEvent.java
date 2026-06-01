package com.unishare.api.common.event;

import java.util.UUID;

public record ModerationReportCreatedEvent(
        UUID reportId,
        UUID reporterId,
        UUID reportedUserId,
        String type,
        UUID entityId,
        String reason,
        String description
) implements DomainEvent {}
