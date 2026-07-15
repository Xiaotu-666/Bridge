package com.bridgeinspection.service;

import com.bridgeinspection.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final JdbcTemplate jdbcTemplate;

    public OperationLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void log(HttpServletRequest request, String module, String operationType,
                    String targetTable, String targetId, String description, boolean success) {
        jdbcTemplate.update("""
                        INSERT INTO tb_operation_log
                        (user_id, user_name, ip_address, module, operation_type, target_table, target_id, description, operation_result)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                SecurityUtils.currentUserId(),
                SecurityUtils.currentUserName(),
                clientIp(request),
                module,
                operationType,
                targetTable,
                targetId,
                description,
                success ? "成功" : "失败"
        );
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
