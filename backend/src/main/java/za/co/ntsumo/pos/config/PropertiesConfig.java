package za.co.ntsumo.pos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import za.co.ntsumo.pos.ai.AiProperties;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class PropertiesConfig {
}
