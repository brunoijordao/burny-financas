package com.burny.financas.transactions.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.attachments")
@Getter
@Setter
public class AttachmentProperties {

    private String storagePath;
}
