package com.my.craft.filestorage.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.security.user.CustomAuthenticationToken;
import com.my.craft.security.user.UserContext;

import com.my.craft.filestorage.service.FileStorageService;
import com.my.craft.filestorage.service.dto.ConfirmUploadRequest;
import com.my.craft.filestorage.service.dto.CreateUploadRequest;
import com.my.craft.filestorage.service.dto.DownloadUrlResponse;
import com.my.craft.filestorage.service.dto.FileSummaryResponse;
import com.my.craft.filestorage.service.dto.UploadUrlResponse;

/**
 * {@code /api/files/**} already falls under the {@code auth-type: BEARER} rule in
 * application.yml's {@code security:} list -- see modules/security -- so every request here
 * already carries an authenticated {@link CustomAuthenticationToken}; {@link #actorOf} just reads
 * the {@link UserContext} it enriched the request with, the same way {@code MeController} does.
 */
@RestController
@RequestMapping("/api/files")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<FileSummaryResponse> list(Authentication authentication) {
        return fileStorageService.listFiles(actorOf(authentication));
    }

    @PostMapping("/presign-upload")
    public UploadUrlResponse presignUpload(@Valid @RequestBody CreateUploadRequest request, Authentication authentication) {
        return fileStorageService.createUploadUrl(request, actorOf(authentication));
    }

    @GetMapping("/{fileId}/presign-download")
    public DownloadUrlResponse presignDownload(@PathVariable UUID fileId, Authentication authentication) {
        return fileStorageService.createDownloadUrl(fileId, actorOf(authentication));
    }

    @PostMapping("/{fileId}/confirm")
    public ResponseEntity<Void> confirm(
            @PathVariable UUID fileId, @Valid @RequestBody ConfirmUploadRequest request, Authentication authentication) {
        fileStorageService.confirm(fileId, request, actorOf(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId, Authentication authentication) {
        fileStorageService.delete(fileId, actorOf(authentication));
        return ResponseEntity.noContent().build();
    }

    private static UserContext actorOf(Authentication authentication) {
        if (!(authentication instanceof CustomAuthenticationToken customAuthentication)) {
            throw new IllegalStateException(
                    "Expected a CustomAuthenticationToken -- is UserContextEnrichmentFilter registered on this chain?");
        }
        return customAuthentication.getUserContext();
    }
}
