package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.MatrixService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/matrices")
public class MatrixController {
    private final MatrixService matrixService;

    public MatrixController(MatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @GetMapping("/components")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> components(@RequestParam(required = false) String bridgeTypeCode) {
        return ApiResponse.ok(matrixService.componentMatrix(bridgeTypeCode));
    }

    @GetMapping("/initial-items")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> initialItems(@RequestParam(required = false) String bridgeTypeCode) {
        return ApiResponse.ok(matrixService.initialItemMatrix(bridgeTypeCode));
    }

    @GetMapping("/components/{componentCode}/configuration")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> componentConfiguration(@PathVariable String componentCode) {
        return ApiResponse.ok(matrixService.componentConfiguration(componentCode));
    }

    @PutMapping("/components/{componentCode}/configuration")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> saveComponentConfiguration(@PathVariable String componentCode,
                                                       @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(matrixService.saveComponentConfiguration(componentCode, body));
    }

    @GetMapping("/initial-items/{itemCode}/configuration")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> initialItemConfiguration(@PathVariable String itemCode) {
        return ApiResponse.ok(matrixService.initialItemConfiguration(itemCode));
    }

    @PutMapping("/initial-items/{itemCode}/configuration")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> saveInitialItemConfiguration(@PathVariable String itemCode,
                                                        @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(matrixService.saveInitialItemConfiguration(itemCode, body));
    }

    @PostMapping("/bridges/{bridgeCode}/generate-components")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> generateComponents(@PathVariable String bridgeCode) {
        return ApiResponse.ok(Map.of("created", matrixService.generateBridgeComponents(bridgeCode)));
    }

    @PostMapping("/initial-inspections/{inspectionCode}/generate-items")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<?> generateItems(@PathVariable String inspectionCode) {
        return ApiResponse.ok(Map.of("created", matrixService.generateInitialItems(inspectionCode)));
    }
}
