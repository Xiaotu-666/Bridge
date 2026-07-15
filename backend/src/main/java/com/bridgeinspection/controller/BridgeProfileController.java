package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.BridgeProfileService;
import com.bridgeinspection.service.BridgeStructureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bridge-profiles")
public class BridgeProfileController {
    private final BridgeProfileService bridgeProfileService;
    private final BridgeStructureService bridgeStructureService;

    public BridgeProfileController(BridgeProfileService bridgeProfileService, BridgeStructureService bridgeStructureService) {
        this.bridgeProfileService = bridgeProfileService;
        this.bridgeStructureService = bridgeStructureService;
    }

    @GetMapping("/map-points")
    public ApiResponse<List<Map<String, Object>>> mapPoints(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(bridgeProfileService.mapPoints(keyword));
    }

    @GetMapping("/geocode")
    public ApiResponse<Map<String, Object>> geocode(@RequestParam String address) {
        return ApiResponse.ok(bridgeProfileService.geocode(address));
    }


    @PostMapping("/{bridgeCode}/photos")
    @PreAuthorize("hasAnyRole('admin','engineer','inspector')")
    public ApiResponse<Map<String, Object>> uploadPhoto(
            @PathVariable String bridgeCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category
    ) {
        return ApiResponse.ok(bridgeProfileService.uploadPhoto(bridgeCode, file, description, category));
    }

    @GetMapping("/{bridgeCode}")
    public ApiResponse<Map<String, Object>> profile(@PathVariable String bridgeCode) {
        return ApiResponse.ok(bridgeProfileService.profile(bridgeCode));
    }

    @GetMapping("/{bridgeCode}/structures")
    public ApiResponse<Map<String, Object>> structures(@PathVariable String bridgeCode) {
        return ApiResponse.ok(bridgeStructureService.get(bridgeCode));
    }

    @PutMapping("/{bridgeCode}/structures")
    @PreAuthorize("hasAnyRole('admin','engineer')")
    public ApiResponse<Map<String, Object>> saveStructures(@PathVariable String bridgeCode,
                                                            @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(bridgeStructureService.replace(bridgeCode, body));
    }
}
