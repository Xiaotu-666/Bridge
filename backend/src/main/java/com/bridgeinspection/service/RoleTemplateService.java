package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleTemplateService {
    private static final List<RoleTemplate> TEMPLATES = List.of(
            new RoleTemplate("admin", "系统管理员", "系统配置与全部业务权限", List.of("*")),
            new RoleTemplate("engineer", "桥梁工程师", "桥梁建档、任务编制与报告生成", List.of(
                    "bridge-view", "bridge-create", "bridge-edit", "matrix-view", "task-view",
                    "task-create", "task-edit", "task-review", "report-view", "report-create", "report-export")),
            new RoleTemplate("inspector", "检查人员", "执行初始检查与定期检查任务", List.of(
                    "task-view", "task-accept", "initial-view", "initial-edit", "periodic-view",
                    "periodic-edit", "defect-view", "defect-edit", "attachment-upload")),
            new RoleTemplate("reviewer", "审核人员", "检查成果审核与归档", List.of(
                    "task-view", "task-review", "initial-view", "initial-review", "periodic-view",
                    "periodic-review", "report-view", "report-review")),
            new RoleTemplate("viewer", "查询人员", "只读查询与统计分析", List.of(
                    "bridge-view", "initial-view", "periodic-view", "defect-view", "report-view", "statistics-view"))
    );

    public List<RoleTemplate> templates() {
        return TEMPLATES;
    }

    public RoleTemplate byDescription(String description) {
        return TEMPLATES.stream()
                .filter(template -> template.description().equals(description))
                .findFirst()
                .orElseThrow(() -> new BusinessException("请选择系统提供的角色说明模板"));
    }

    public String permissionJson(String description) {
        return "[\"" + String.join("\",\"", byDescription(description).permissions()) + "\"]";
    }

    public record RoleTemplate(String code, String name, String description, List<String> permissions) {
    }
}
