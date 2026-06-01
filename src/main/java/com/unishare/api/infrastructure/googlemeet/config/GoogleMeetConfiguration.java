package com.unishare.api.infrastructure.googlemeet.config;

import com.unishare.api.infrastructure.googlemeet.GoogleMeetProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleMeetProperties.class)
public class GoogleMeetConfiguration {
}
