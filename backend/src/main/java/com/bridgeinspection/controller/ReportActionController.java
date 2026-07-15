package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.OperationLogService;
import com.bridgeinspection.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportActionController {
    private final ReportService reportService;
    private final OperationLogService logService;

    public ReportActionController(ReportService reportService, OperationLogService logService) {
        this.reportService = reportService;
        this.logService = logService;
    }

    @PostMapping("/generate/{taskId}")
    @PreAuthorize("hasAnyAuthority('report-create') or hasRole('admin')")
    public ApiResponse<Map<String, Object>> generate(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        String type = body == null ? "comprehensive" : body.getOrDefault("reportType", "comprehensive");
        String summary = body == null ? "生成检查报告" : body.getOrDefault("changeSummary", "生成检查报告");
        Map<String, Object> report = reportService.generate(taskId, type, summary);
        logService.log(request, "report", "新增", "tb_report", String.valueOf(report.get("report_id")), "生成检查报告", true);
        return ApiResponse.ok(report);
    }

    @PostMapping("/bridge-card/{bridgeCode}")
    @PreAuthorize("hasAnyAuthority('report-create') or hasRole('admin')")
    public ApiResponse<Map<String, Object>> generateBridgeCard(@PathVariable String bridgeCode, HttpServletRequest request) {
        Map<String, Object> report = reportService.generateBridgeCard(bridgeCode);
        logService.log(request, "report", "新增", "tb_report", String.valueOf(report.get("report_id")),
                "生成桥梁基本状况卡片 PDF", true);
        return ApiResponse.ok(report);
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> download(@PathVariable String reportId) {
        Resource resource = reportService.loadReportFile(reportId);
        String fileName = resource.getFilename() == null ? reportId + ".pdf" : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
