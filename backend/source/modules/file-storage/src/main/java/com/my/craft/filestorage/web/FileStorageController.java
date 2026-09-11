package com.my.craft.filestorage.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.security.security.CustomAuthenticationToken;
import com.my.craft.security.security.UserContextUtils;

import com.my.craft.filestorage.service.FileStorageService;
import com.my.craft.filestorage.service.dto.ConfirmUploadRequest;
import com.my.craft.filestorage.service.dto.CreateUploadRequest;
import com.my.craft.filestorage.service.dto.DownloadUrlResponse;
import com.my.craft.filestorage.service.dto.FileSummaryResponse;
import com.my.craft.filestorage.service.dto.UploadUrlResponse;

/**
 * {@code /api/files/**} already falls under the {@code auth-type: BEARER} rule in
 * application.yml's {@code security:} list -- see modules/security -- so every request here
 * already carries an authenticated {@link CustomAuthenticationToken}; {@link UserContextUtils}
 * reads it off {@code SecurityContextHolder} for us.
 */
@RestController
@RequestMapping("/api/files")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<FileSummaryResponse> list() {
        return fileStorageService.listFiles(UserContextUtils.currentUserContext());
    }

    @PostMapping("/presign-upload")
    public UploadUrlResponse presignUpload(@Valid @RequestBody CreateUploadRequest request) {
        return fileStorageService.createUploadUrl(request, UserContextUtils.currentUserContext());
    }

    @GetMapping("/{fileId}/presign-download")
    public DownloadUrlResponse presignDownload(@PathVariable UUID fileId) {
        return fileStorageService.createDownloadUrl(fileId, UserContextUtils.currentUserContext());
    }

    @PostMapping("/{fileId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable UUID fileId, @Valid @RequestBody ConfirmUploadRequest request) {
        fileStorageService.confirm(fileId, request, UserContextUtils.currentUserContext());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        fileStorageService.delete(fileId, UserContextUtils.currentUserContext());
        return ResponseEntity.noContent().build();
    }
}
