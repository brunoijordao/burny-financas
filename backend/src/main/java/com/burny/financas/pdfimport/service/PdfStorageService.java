package com.burny.financas.pdfimport.service;

import com.burny.financas.pdfimport.config.PdfImportProperties;
import com.burny.financas.pdfimport.exception.InvalidPdfImportDataException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-disk storage for uploaded PDF statements, following {@code FileStorageService}'s
 * {@code {storagePath}/{userId}/{uuid}.pdf} convention (see design.md "Decision 8"). Kept separate
 * from {@code transactions.FileStorageService} since PDF imports are their own owned resource with
 * their own storage root, not a transaction attachment.
 */
@Service
@RequiredArgsConstructor
public class PdfStorageService {

    private final PdfImportProperties pdfImportProperties;

    public String store(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidPdfImportDataException("Only application/pdf files are accepted");
        }

        try {
            Path userDirectory = Path.of(pdfImportProperties.getStoragePath(), String.valueOf(userId));
            Files.createDirectories(userDirectory);

            Path target = userDirectory.resolve(UUID.randomUUID() + ".pdf");
            file.transferTo(target);

            return target.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store PDF import file", e);
        }
    }

    public byte[] load(String storagePath) {
        try {
            return Files.readAllBytes(Path.of(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read stored PDF import file", e);
        }
    }
}
