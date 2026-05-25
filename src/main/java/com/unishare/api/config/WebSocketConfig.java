package com.unishare.api.config;

import com.unishare.api.infrastructure.security.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final AppUrlsProperties appUrls;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(scheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> originsList = appUrls.corsAllowedOrigins();
        String[] origins = originsList != null && !originsList.isEmpty() 
                ? originsList.toArray(new String[0]) 
                : new String[]{"*"};

        // Raw WebSocket Endpoint
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketHandshakeInterceptor());

        // SockJS fallback Endpoint
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
