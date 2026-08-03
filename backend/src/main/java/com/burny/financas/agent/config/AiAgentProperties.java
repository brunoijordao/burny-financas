package com.burny.financas.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Entirely independent from {@code com.burny.financas.pdfimport.config.GemmaProperties} (design.md
 * Decision 6): a different model, base URL, and timeouts can be configured without touching
 * pdf-import's existing behavior.
 */
@Component
@ConfigurationProperties(prefix = "app.ai-agent")
@Getter
@Setter
public class AiAgentProperties {

    private String baseUrl;
    private String model;
    private String apiKey;
    private long connectTimeoutMs;
    private long readTimeoutMs;
    private int maxHistoryMessages;
}
