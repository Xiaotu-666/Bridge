package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.InspectionWorkbenchService;
import com.bridgeinspection.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspection-workbench")
@PreAuthorize("hasAnyRole('admin','inspector')")
public class InspectionWorkbenchController {
    private final InspectionWorkbenchService workbenchService;
    private final OperationLogService logService;

    public InspectionWorkbenchController(InspectionWorkbenchService workbenchService,
                                         OperationLogService logService) {
        this.workbenchService = workbenchService;
        this.logService = logService;
    }

    @GetMapping("/{type}/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks(@PathVariable String type) {
        return ApiResponse.ok(workbenchService.tasks(type));
    }

    @GetMapping("/{type}/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> task(@PathVariable String type, @PathVariable String taskId) {
        return ApiResponse.ok(workbenchService.task(type, taskId));
    }

    @PostMapping("/{type}/tasks/{taskId}/draft")
    public ApiResponse<Map<String, Object>> saveDraft(@PathVariable String type,
                                                      @PathVariable String taskId,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        Map<String, Object> result = workbenchService.save(type, taskId, body, false);
        logService.log(request, "inspection", "保存", inspectionTable(type), taskId,
                "保存检查填表草稿", true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{type}/tasks/{taskId}/submit")
    public ApiResponse<Map<String, Object>> submit(@PathVariable String type,
                                                   @PathVariable String taskId,
                                                   @RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        Map<String, Object> result = workbenchService.save(type, taskId, body, true);
        logService.log(request, "inspection", "上传", inspectionTable(type), taskId,
                "检查填表上传至检查记录并生成病害记录", true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/periodic/photos")
    public ApiResponse<Map<String, Object>> uploadComponentPhoto(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam("componentInspectionId") Integer componentInspectionId) {
        return ApiResponse.ok(workbenchService.uploadComponentPhoto(file, componentInspectionId));
    }

    private String inspectionTable(String type) {
        return "initial".equalsIgnoreCase(type) ? "tb_initial_inspection" : "tb_periodic_inspection";
    }
}
