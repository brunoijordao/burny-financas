package com.burny.financas.pdfimport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.pdf-imports")
@Getter
@Setter
public class PdfImportProperties {

    private String storagePath;
}
