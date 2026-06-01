package com.unishare.api.common.event;

import java.util.UUID;

public record ModerationReportResolvedEvent(
        UUID reportId,
        UUID reporterId,
        UUID reportedUserId,
        String type,
        UUID entityId,
        String status,
        String resolutionNote
) implements DomainEvent {}
