package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.common.PageResult;
import com.bridgeinspection.service.InspectionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InspectionQueryController {
    private final InspectionQueryService queryService;

    public InspectionQueryController(InspectionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/inspection-results")
    public ApiResponse<PageResult<Map<String, Object>>> inspections(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(queryService.inspections(params));
    }

    @GetMapping("/defect-results")
    public ApiResponse<PageResult<Map<String, Object>>> defects(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(queryService.defects(params));
    }
}
