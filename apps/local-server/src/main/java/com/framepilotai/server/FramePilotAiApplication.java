package com.framepilotai.server;

import com.framepilotai.server.common.config.FramePilotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FramePilotProperties.class)
public class FramePilotAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FramePilotAiApplication.class, args);
    }
}
