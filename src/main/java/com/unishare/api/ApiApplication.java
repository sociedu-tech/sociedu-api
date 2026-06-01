package com.unishare.api;

import org.springframework.boot.SpringApplication;
import com.unishare.api.config.AppUrlsProperties;
import com.unishare.api.config.WebSocketProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({ AppUrlsProperties.class, WebSocketProperties.class })
@EnableJpaAuditing
@EnableAsync
public class ApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ApiApplication.class, args);
    }
}
