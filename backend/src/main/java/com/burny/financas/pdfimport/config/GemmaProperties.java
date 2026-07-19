package com.burny.financas.pdfimport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.gemma")
@Getter
@Setter
public class GemmaProperties {

    private String baseUrl;
    private String model;
    private String apiKey;
    private long connectTimeoutMs;
    private long readTimeoutMs;
}
