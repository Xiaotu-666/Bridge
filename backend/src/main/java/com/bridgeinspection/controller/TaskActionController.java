package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.OperationLogService;
import com.bridgeinspection.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskActionController {
    private final TaskService taskService;
    private final OperationLogService logService;

    public TaskActionController(TaskService taskService, OperationLogService logService) {
        this.taskService = taskService;
        this.logService = logService;
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('task-create','task-edit') or hasRole('admin')")
    public ApiResponse<Void> assign(@PathVariable String id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object users = body.get("userIds");
        List<String> userIds = users instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
        taskService.assign(id, userIds);
        logService.log(request, "task", "修改", "tb_inspection_task", id, "分配检查任务", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyAuthority('task-accept') or hasRole('admin')")
    public ApiResponse<Void> accept(@PathVariable String id, HttpServletRequest request) {
        taskService.accept(id);
        logService.log(request, "task", "修改", "tb_inspection_task", id, "接受检查任务", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('initial-edit','periodic-edit') or hasRole('admin')")
    public ApiResponse<Void> complete(@PathVariable String id, HttpServletRequest request) {
        taskService.complete(id);
        logService.log(request, "task", "修改", "tb_inspection_task", id, "完成检查任务", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('task-review') or hasRole('admin')")
    public ApiResponse<Void> review(@PathVariable String id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        taskService.review(id, body == null ? null : body.get("opinion"));
        logService.log(request, "task", "审核", "tb_inspection_task", id, "审核通过检查任务", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('task-review') or hasRole('admin')")
    public ApiResponse<Void> reject(@PathVariable String id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        taskService.reject(id, body == null ? null : body.get("reason"));
        logService.log(request, "task", "审核", "tb_inspection_task", id, "驳回检查任务", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('task-edit') or hasRole('admin')")
    public ApiResponse<Void> cancel(@PathVariable String id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        taskService.cancel(id, body == null ? null : body.get("reason"));
        logService.log(request, "task", "修改", "tb_inspection_task", id, "取消检查任务", true);
        return ApiResponse.ok(null);
    }
}
