package com.unishare.api.config;

import com.unishare.api.infrastructure.realtime.RealtimeTopics;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket/STOMP runtime config. Scale-out: set {@code broker.type=relay} with RabbitMQ (see application.yaml).
 */
@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(
        String endpoint,
        long heartbeatMs,
        Broker broker
) {
    public WebSocketProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = RealtimeTopics.ENDPOINT;
        }
        if (heartbeatMs <= 0) {
            heartbeatMs = 10_000L;
        }
        if (broker == null) {
            broker = new Broker("simple", null, null, null, 61613);
        }
    }

    public record Broker(
            /** {@code simple} = in-memory (single node). {@code relay} = external STOMP broker. */
            String type,
            String relayHost,
            String relayLogin,
            String relayPasscode,
            int relayPort
    ) {
        public Broker {
            if (type == null || type.isBlank()) {
                type = "simple";
            }
            if (relayHost == null) {
                relayHost = "localhost";
            }
            if (relayLogin == null) {
                relayLogin = "guest";
            }
            if (relayPasscode == null) {
                relayPasscode = "guest";
            }
            if (relayPort <= 0) {
                relayPort = 61613;
            }
        }

        public boolean isRelay() {
            return "relay".equalsIgnoreCase(type);
        }
    }

    public boolean isRelayBroker() {
        return broker.isRelay();
    }
}
