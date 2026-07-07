package com.burny.financas.transactions.service;

import com.burny.financas.transactions.config.AttachmentProperties;
import com.burny.financas.transactions.exception.InvalidTransactionDataException;
import com.burny.financas.transactions.exception.TransactionNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-disk attachment storage (see design.md "Anexos" — no cloud dependency at this scale).
 * Stores files under {@code {storagePath}/{userId}/{uuid}.{ext}}: the generated filename never
 * reuses the caller-supplied original filename, avoiding both collisions and path traversal.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "application/pdf");

    private final AttachmentProperties attachmentProperties;

    public record StoredFile(String storagePath, String originalFilename, String contentType, long sizeBytes) {
    }

    public StoredFile store(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidTransactionDataException("Unsupported attachment file type: " + contentType);
        }

        try {
            Path userDirectory = Path.of(attachmentProperties.getStoragePath(), String.valueOf(userId));
            Files.createDirectories(userDirectory);

            Path target = userDirectory.resolve(UUID.randomUUID() + extensionFor(contentType));
            file.transferTo(target);

            return new StoredFile(target.toString(), file.getOriginalFilename(), contentType, file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment", e);
        }
    }

    public Resource load(String storagePath) {
        try {
            Resource resource = new UrlResource(Path.of(storagePath).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new TransactionNotFoundException("Attachment file not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new UncheckedIOException("Failed to load attachment", e);
        }
    }

    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete attachment file", e);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
