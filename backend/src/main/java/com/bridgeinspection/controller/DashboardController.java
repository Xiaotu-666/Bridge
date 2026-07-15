package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.security.SecurityUtils;
import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{roleCode}")
    public ApiResponse<?> overview(@PathVariable String roleCode) {
        var user = SecurityUtils.currentUserOrNull();
        String currentRole = user == null || user.roles().isEmpty() ? "" : user.roles().get(0);
        if (!"admin".equals(currentRole) && !currentRole.equals(roleCode)) {
            throw new BusinessException(403, "不能访问其他角色工作台");
        }
        return ApiResponse.ok(dashboardService.overview(roleCode, SecurityUtils.currentUserId()));
    }

    @GetMapping("/current")
    public ApiResponse<?> current(@RequestParam(defaultValue = "12") int months) {
        var user = SecurityUtils.currentUserOrNull();
        String role = user == null || user.roles().isEmpty() ? "viewer" : user.roles().get(0);
        return ApiResponse.ok(dashboardService.overview(role, SecurityUtils.currentUserId(), months));
    }
}
