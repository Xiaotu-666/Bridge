package com.bridgeinspection.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DashboardService {
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview(String roleCode, String userId) {
        return overview(roleCode, userId, 12);
    }

    public Map<String, Object> overview(String roleCode, String userId, int requestedMonths) {
        int months = List.of(6, 12, 24).contains(requestedMonths) ? requestedMonths : 12;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", roleCode);
        result.put("bridges", count("SELECT COUNT(*) FROM tb_bridge WHERE status = 1"));
        result.put("tasks", count("SELECT COUNT(*) FROM tb_inspection_task"));
        result.put("pendingTasks", count("SELECT COUNT(*) FROM tb_inspection_task WHERE task_status IN ('待分配','待接受','进行中')"));
        result.put("initialInspections", count("SELECT COUNT(*) FROM tb_initial_inspection"));
        result.put("periodicInspections", count("SELECT COUNT(*) FROM tb_periodic_inspection"));
        result.put("defects", count("SELECT COUNT(*) FROM tb_defect"));
        result.put("reports", count("SELECT COUNT(*) FROM tb_report"));
        result.put("users", count("SELECT COUNT(*) FROM tb_user WHERE user_status = 1"));
        result.put("myTasks", count("SELECT COUNT(*) FROM tb_task_assignment WHERE user_id = ?", userId));
        result.put("recentTasks", jdbcTemplate.queryForList("""
                SELECT task_id, bridge_code, inspection_type, task_status, plan_end_date
                FROM tb_inspection_task ORDER BY create_time DESC LIMIT 8
                """));
        result.put("recentInitialTasks", recentTasks("initial"));
        result.put("recentPeriodicTasks", recentTasks("periodic"));
        result.put("roleDistribution", jdbcTemplate.queryForList("""
                SELECT r.role_name AS name, COUNT(*) AS value
                FROM tb_user u JOIN tb_role r ON r.role_id=u.role_id
                WHERE u.user_status=1 GROUP BY r.role_id,r.role_name ORDER BY value DESC
                """));
        result.put("bridgeTypeDistribution", jdbcTemplate.queryForList("""
                SELECT CASE WHEN b.bridge_type_code IN ('beam','arch','rigid_arch','composite_arch','cable_stayed','suspension')
                       THEN bt.bridge_type_name ELSE '其他' END AS name, COUNT(*) AS value
                FROM tb_bridge b LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                WHERE b.status=1 GROUP BY name ORDER BY value DESC
                """));
        result.put("taskTypeSummary", jdbcTemplate.queryForList("""
                SELECT inspection_type,
                       SUM(task_status IN ('已完成','已审核')) AS completed,
                       SUM(task_status NOT IN ('已完成','已审核','已取消')) AS pending
                FROM tb_inspection_task GROUP BY inspection_type
                """));
        result.put("periodicTrend", periodicTrend(months));
        return result;
    }

    private List<Map<String, Object>> recentTasks(String type) {
        return jdbcTemplate.queryForList("""
                SELECT task_id,bridge_code,inspection_type,task_status,plan_start_date,plan_end_date,actual_end_date
                FROM tb_inspection_task WHERE inspection_type=? ORDER BY create_time DESC LIMIT 6
                """, type);
    }

    private List<Map<String, Object>> periodicTrend(int months) {
        LocalDate start = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L);
        LocalDate end = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        Map<String, Integer> completed = toMonthMap(jdbcTemplate.queryForList("""
                SELECT DATE_FORMAT(COALESCE(actual_end_date,plan_end_date),'%Y-%m') month_key,COUNT(*) count_value
                FROM tb_inspection_task
                WHERE inspection_type='periodic' AND task_status IN ('已完成','已审核')
                  AND COALESCE(actual_end_date,plan_end_date)>=? AND COALESCE(actual_end_date,plan_end_date)<?
                GROUP BY month_key
                """, start, end));
        Map<String, Integer> pending = toMonthMap(jdbcTemplate.queryForList("""
                SELECT DATE_FORMAT(plan_end_date,'%Y-%m') month_key,COUNT(*) count_value
                FROM tb_inspection_task
                WHERE inspection_type='periodic' AND task_status NOT IN ('已完成','已审核','已取消')
                  AND plan_end_date>=? AND plan_end_date<? GROUP BY month_key
                """, start, end));
        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<>();
        DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int index = 0; index < months; index++) {
            String key = start.plusMonths(index).format(keyFormat);
            result.add(Map.of("month", key, "completed", completed.getOrDefault(key, 0), "pending", pending.getOrDefault(key, 0)));
        }
        return result;
    }

    private Map<String, Integer> toMonthMap(List<Map<String, Object>> rows) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object key = row.get("month_key"), value = row.get("count_value");
            if (key != null && value instanceof Number number) result.put(String.valueOf(key), number.intValue());
        }
        return result;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
