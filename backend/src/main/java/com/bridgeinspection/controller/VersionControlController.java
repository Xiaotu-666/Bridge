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
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PostMapping("/github/check")
    public ApiResponse<Map<String, Object>> checkGithubUpdates(HttpServletRequest request) {
        Map<String, Object> result = versionControlService.checkGithubUpdates();
        logService.log(request, "version-control", "检查更新", ".git", String.valueOf(result.get("branch")),
                "检查 GitHub 更新：" + result.get("message"), true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/github/update")
    public ApiResponse<Map<String, Object>> applyGithubUpdate(HttpServletRequest request) {
        Map<String, Object> result = versionControlService.applyGithubUpdate();
        logService.log(request, "version-control", "版本更新", ".git", String.valueOf(result.get("current_version")),
                String.valueOf(result.get("message")), true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/versions/{versionNo}/rollback")
    public ApiResponse<Map<String, Object>> rollbackVersion(@PathVariable String versionNo,
                                                            HttpServletRequest request) {
        Map<String, Object> result = versionControlService.rollbackVersion(versionNo);
        logService.log(request, "version-control", "版本回溯", ".git", versionNo,
                String.valueOf(result.get("message")), true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/backups")
    public ApiResponse<Map<String, Object>> createBackup(@RequestBody(required = false) Map<String, String> body,
                                                         HttpServletRequest request) {
        String message = body == null ? null : body.get("message");
        Map<String, Object> result = versionControlService.createDatabaseBackup(message);
        logService.log(request, "version-control", "数据库备份", "tb_backup_record",
                String.valueOf(result.get("backup_id")), "创建独立数据库备份", true);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/backups/{backupId}")
    public ApiResponse<Map<String, Object>> deleteBackup(@PathVariable long backupId,
                                                         HttpServletRequest request) {
        Map<String, Object> result = versionControlService.deleteBackup(backupId);
        logService.log(request, "version-control", "删除备份", "tb_backup_record", String.valueOf(backupId),
                "删除数据库备份：" + result.get("file_name"), true);
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
