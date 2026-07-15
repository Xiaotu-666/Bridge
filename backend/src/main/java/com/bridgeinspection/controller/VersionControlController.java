package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.OperationLogService;
import com.bridgeinspection.service.VersionControlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/version-control")
@PreAuthorize("hasRole('admin')")
public class VersionControlController {
    private final VersionControlService versionControlService;
    private final OperationLogService logService;

    public VersionControlController(VersionControlService versionControlService,
                                    OperationLogService logService) {
        this.versionControlService = versionControlService;
        this.logService = logService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(versionControlService.summary());
    }

    @PostMapping("/initialize")
    public ApiResponse<Map<String, Object>> initialize(HttpServletRequest request) {
        Map<String, Object> result = versionControlService.initialize();
        logService.log(request, "version-control", "初始化", ".git", "repository", "初始化Git版本仓库", true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/versions")
    public ApiResponse<Map<String, Object>> createVersion(@RequestBody(required = false) Map<String, String> body,
                                                          HttpServletRequest request) {
        String message = body == null ? null : body.get("message");
        Map<String, Object> result = versionControlService.createVersion(message);
        logService.log(request, "version-control", "创建版本", ".git", String.valueOf(result.get("short_hash")),
                "创建Git版本并包含数据库备份", true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/backups/{backupId}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable long backupId) {
        Resource resource = versionControlService.backupFile(backupId);
        String fileName = resource.getFilename() == null ? "database-backup.sql" : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .body(resource);
    }
}
