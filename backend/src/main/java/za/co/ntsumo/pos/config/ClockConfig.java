package za.co.ntsumo.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Time is injected, never read from {@code Instant.now()} inside a service.
 * Ageing, credit and due dates all depend on "today", and a test that cannot
 * move today cannot prove a 90-day rule.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
