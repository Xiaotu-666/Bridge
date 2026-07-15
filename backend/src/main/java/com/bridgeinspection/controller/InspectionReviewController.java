package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.service.InspectionReviewService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/review-workbench")
@PreAuthorize("hasAnyRole('admin','reviewer')")
public class InspectionReviewController {
    private final InspectionReviewService reviewService;
    private final OperationLogService logService;

    public InspectionReviewController(InspectionReviewService reviewService,
                                      OperationLogService logService) {
        this.reviewService = reviewService;
        this.logService = logService;
    }

    @GetMapping("/{type}")
    public ApiResponse<Map<String, Object>> list(@PathVariable String type,
                                                 @RequestParam(defaultValue = "pending") String state) {
        return ApiResponse.ok(reviewService.list(type, state));
    }

    @GetMapping("/{type}/{recordCode}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String type,
                                                   @PathVariable String recordCode) {
        return ApiResponse.ok(reviewService.detail(type, recordCode));
    }

    @PostMapping("/{type}/{recordCode}/approve")
    public ApiResponse<Void> approve(@PathVariable String type,
                                     @PathVariable String recordCode,
                                     @RequestBody(required = false) Map<String, String> body,
                                     HttpServletRequest request) {
        String opinion = body == null ? null : body.get("opinion");
        reviewService.approve(type, recordCode, opinion);
        logService.log(request, "inspection-review", "审核通过", table(type), recordCode,
                "检查表审核通过并归档", true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{type}/{recordCode}/reject")
    public ApiResponse<Void> reject(@PathVariable String type,
                                    @PathVariable String recordCode,
                                    @RequestBody Map<String, String> body,
                                    HttpServletRequest request) {
        reviewService.reject(type, recordCode, body == null ? null : body.get("reason"));
        logService.log(request, "inspection-review", "打回", table(type), recordCode,
                "检查表审核未通过，已退回检查人员修改", true);
        return ApiResponse.ok(null);
    }

    private String table(String type) {
        return "initial".equalsIgnoreCase(type) ? "tb_initial_inspection" : "tb_periodic_inspection";
    }
}
