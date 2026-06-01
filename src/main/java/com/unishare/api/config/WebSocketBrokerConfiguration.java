package com.unishare.api.config;

import com.unishare.api.infrastructure.realtime.RealtimeTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP broker wiring. {@code simple} = one JVM; {@code relay} = RabbitMQ STOMP for horizontal scale.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketBrokerConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties webSocketProperties;
    private final AppUrlsProperties appUrls;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();

        long heartbeat = webSocketProperties.heartbeatMs();

        if (webSocketProperties.isRelayBroker()) {
            WebSocketProperties.Broker relay = webSocketProperties.broker();
            log.info("STOMP broker relay enabled host={} port={}", relay.relayHost(), relay.relayPort());
            registry.enableStompBrokerRelay(RealtimeTopics.TOPIC_PREFIX, RealtimeTopics.QUEUE_PREFIX)
                    .setRelayHost(relay.relayHost())
                    .setRelayPort(relay.relayPort())
                    .setClientLogin(relay.relayLogin())
                    .setClientPasscode(relay.relayPasscode())
                    .setSystemLogin(relay.relayLogin())
                    .setSystemPasscode(relay.relayPasscode())
                    .setTaskScheduler(scheduler);
        } else {
            registry.enableSimpleBroker(RealtimeTopics.TOPIC_PREFIX, RealtimeTopics.QUEUE_PREFIX)
                    .setHeartbeatValue(new long[]{heartbeat, heartbeat})
                    .setTaskScheduler(scheduler);
        }

        registry.setApplicationDestinationPrefixes(RealtimeTopics.APP_PREFIX);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var originsList = appUrls.corsAllowedOrigins();
        String[] origins = originsList != null && !originsList.isEmpty()
                ? originsList.toArray(new String[0])
                : new String[]{"*"};

        String endpoint = webSocketProperties.endpoint();

        registry.addEndpoint(endpoint)
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketHandshakeInterceptor());

        registry.addEndpoint(endpoint)
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .withSockJS();
    }
}
