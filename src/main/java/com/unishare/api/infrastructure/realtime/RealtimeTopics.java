package com.unishare.api.infrastructure.realtime;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * STOMP destination naming — single source of truth for web/mobile clients.
 */
public final class RealtimeTopics {

    /** REST API prefix — WS endpoint lives under the same prefix on the same host/port. */
    public static final String API_V1_PREFIX = "/api/v1";

    /** SockJS / raw WebSocket STOMP entry (same server as REST, no separate WS host). */
    public static final String ENDPOINT = API_V1_PREFIX + "/ws";

    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String APP_PREFIX = "/app";

    public static final String CONVERSATION_TOPIC_PREFIX = TOPIC_PREFIX + "/conversations/";
    public static final String USER_NOTIFICATIONS_TOPIC_PREFIX = TOPIC_PREFIX + "/users/";

    public static final Pattern CONVERSATION_TOPIC =
            Pattern.compile("^" + Pattern.quote(CONVERSATION_TOPIC_PREFIX) + "([^/]+)$");
    public static final Pattern USER_NOTIFICATIONS_TOPIC =
            Pattern.compile("^" + Pattern.quote(USER_NOTIFICATIONS_TOPIC_PREFIX) + "([^/]+)/notifications$");

    private RealtimeTopics() {
    }

    public static String conversation(UUID conversationId) {
        return CONVERSATION_TOPIC_PREFIX + conversationId;
    }

    public static String userNotifications(UUID userId) {
        return USER_NOTIFICATIONS_TOPIC_PREFIX + userId + "/notifications";
    }
}
