package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.common.PageResult;
import com.bridgeinspection.service.CrudService;
import com.bridgeinspection.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CrudController {
    private final CrudService crudService;
    private final OperationLogService logService;

    public CrudController(CrudService crudService, OperationLogService logService) {
        this.crudService = crudService;
        this.logService = logService;
    }

    @GetMapping("/{resource}")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @PathVariable String resource,
            @RequestParam Map<String, String> params
    ) {
        return ApiResponse.ok(crudService.list(resource, params));
    }

    @GetMapping("/{resource}/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String resource, @PathVariable String id) {
        return ApiResponse.ok(crudService.get(resource, id));
    }

    @PostMapping("/{resource}")
    @PreAuthorize("hasAnyRole('admin','engineer','inspector')")
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String resource,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> row = crudService.create(resource, payload);
        CrudService.ResourceConfig config = crudService.config(resource);
        logService.log(request, module(resource), "新增", config.tableName(), String.valueOf(row.get(config.idColumn())),
                "新增" + resource + "记录", true);
        return ApiResponse.ok(row);
    }

    @PutMapping("/{resource}/{id}")
    @PreAuthorize("hasAnyRole('admin','engineer','inspector')")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> row = crudService.update(resource, id, payload);
        logService.log(request, module(resource), "修改", crudService.config(resource).tableName(), id,
                "修改" + resource + "记录", true);
        return ApiResponse.ok(row);
    }

    @DeleteMapping("/{resource}/{id}")
    @PreAuthorize("hasAnyAuthority('bridge-delete','task-delete','defect-delete','system-user','system-role') or hasRole('admin') or (#resource == 'defects' and hasAnyRole('engineer','inspector'))")
    public ApiResponse<Void> delete(@PathVariable String resource, @PathVariable String id, HttpServletRequest request) {
        crudService.delete(resource, id);
        logService.log(request, module(resource), "删除", crudService.config(resource).tableName(), id,
                "删除" + resource + "记录", true);
        return ApiResponse.ok(null);
    }

    private String module(String resource) {
        if (resource.contains("bridge") || resource.contains("archive") || resource.contains("component")) {
            return "bridge";
        }
        if (resource.contains("task")) {
            return "task";
        }
        if (resource.contains("inspection-data")) {
            return "data";
        }
        if (resource.contains("defect")) {
            return "defect";
        }
        if (resource.contains("report")) {
            return "report";
        }
        return "system";
    }
}
