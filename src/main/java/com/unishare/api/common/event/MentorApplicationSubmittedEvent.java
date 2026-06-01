package com.unishare.api.common.event;

import java.util.UUID;

/** User gửi đơn đăng ký mentor. */
public record MentorApplicationSubmittedEvent(UUID requestId, UUID userId) implements DomainEvent {}
