package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.RoutePlanningService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RoutePlanningController {
    private final RoutePlanningService service;

    public RoutePlanningController(RoutePlanningService service) { this.service = service; }

    @PostMapping("/plan")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<Map<String, Object>> plan(@RequestBody Map<String, Object> body) {
        Object points = body.get("points");
        return ApiResponse.ok(service.plan(points instanceof List<?> list ? list : List.of()));
    }
}
