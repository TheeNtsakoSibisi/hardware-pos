package za.co.ntsumo.pos.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the optional Claude-backed quote parser.
 *
 * <p>The key is read from the environment (POS_AI_ANTHROPIC_API_KEY) and is
 * never committed. With no key set, the rule-based parser runs instead.
 */
@ConfigurationProperties(prefix = "pos.ai")
public record AiProperties(String anthropicApiKey, String model, String baseUrl) {

    public AiProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-5";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com";
        }
    }
}
