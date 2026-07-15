package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.RoleTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final RoleTemplateService roleTemplateService;

    public SystemController(RoleTemplateService roleTemplateService) {
        this.roleTemplateService = roleTemplateService;
    }

    @GetMapping("/version")
    public ApiResponse<Map<String, Object>> version() {
        return ApiResponse.ok(Map.of("version_no", "2.0.0", "model", "bridge_inspection_v2"));
    }

    @GetMapping("/role-templates")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<List<RoleTemplateService.RoleTemplate>> roleTemplates() {
        return ApiResponse.ok(roleTemplateService.templates());
    }
}
