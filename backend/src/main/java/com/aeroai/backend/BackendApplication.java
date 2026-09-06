package com.aeroai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * AeroAI - AI-based Air Traffic Monitoring and Prediction System.
 * Spring Boot modular-monolith backend entry point. See IMPLEMENT_v2.md for
 * the full specification this backend implements.
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
        // Section 45 - store/handle timestamps in UTC internally. Forcing the
        // JVM default timezone here avoids the host OS reporting a legacy
        // zone ID (e.g. "Asia/Calcutta") that some PostgreSQL builds reject
        // during the JDBC driver's session TimeZone negotiation.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BackendApplication.class, args);
    }
}
