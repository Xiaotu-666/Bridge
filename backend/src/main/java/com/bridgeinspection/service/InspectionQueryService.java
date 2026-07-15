package com.bridgeinspection.service;

import com.bridgeinspection.common.PageResult;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InspectionQueryService {
    private final JdbcTemplate jdbcTemplate;

    public InspectionQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> inspections(Map<String, String> params) {
        int page = positive(params.get("page"), 1);
        int size = Math.min(positive(params.get("size"), 10), 100);
        String union = """
                SELECT 'initial' inspection_type,i.initial_inspection_code inspection_code,i.task_id,
                       i.bridge_code,b.bridge_name,bt.bridge_type_name,i.inspection_date,i.status,
                       'B' form_table_code,i.record_form_no,NULL rating_level_code,
                       (SELECT COUNT(*) FROM tb_initial_inspection_item r WHERE r.initial_inspection_code=i.initial_inspection_code) result_count,
                       (SELECT COUNT(*) FROM tb_defect d WHERE d.initial_inspection_code=i.initial_inspection_code) defect_count
                FROM tb_initial_inspection i JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                UNION ALL
                SELECT 'periodic' inspection_type,p.periodic_inspection_code inspection_code,p.task_id,
                       p.bridge_code,b.bridge_name,bt.bridge_type_name,p.inspection_date,p.status,
                       p.form_table_code,p.record_form_no,p.rating_level_code,
                       (SELECT COUNT(*) FROM tb_component_inspection r WHERE r.periodic_inspection_code=p.periodic_inspection_code) result_count,
                       (SELECT COUNT(*) FROM tb_defect d WHERE d.periodic_inspection_code=p.periodic_inspection_code
                           OR d.component_inspection_id IN (SELECT ci.component_inspection_id FROM tb_component_inspection ci WHERE ci.periodic_inspection_code=p.periodic_inspection_code)) defect_count
                FROM tb_periodic_inspection p JOIN tb_bridge b ON b.bridge_code=p.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                """;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        exact(where, args, "inspection_type", params.get("inspectionType"));
        exact(where, args, "bridge_code", params.get("bridgeCode"));
        like(where, args, "bridge_name", params.get("bridgeName"));
        String keyword = params.get("keyword");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (inspection_code LIKE ? OR task_id LIKE ? OR bridge_code LIKE ? OR bridge_name LIKE ?)");
            for (int i = 0; i < 4; i++) args.add("%" + keyword.trim() + "%");
        }
        String base = " FROM (" + union + ") result" + where;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + base, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT *" + base + " ORDER BY inspection_date DESC,inspection_code DESC LIMIT ? OFFSET ?", listArgs.toArray());
        return new PageResult<>(rows, total == null ? 0 : total, page, size);
    }

    public PageResult<Map<String, Object>> defects(Map<String, String> params) {
        int page = positive(params.get("page"), 1);
        int size = Math.min(positive(params.get("size"), 10), 100);
        String base = """
                FROM tb_defect d
                JOIN tb_bridge b ON b.bridge_code=d.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_defect_definition dd ON dd.defect_definition_code=d.defect_definition_code
                LEFT JOIN tb_component_inspection ci ON ci.component_inspection_id=d.component_inspection_id
                LEFT JOIN tb_initial_inspection ii ON ii.initial_inspection_code=d.initial_inspection_code
                LEFT JOIN tb_periodic_inspection pi ON pi.periodic_inspection_code=COALESCE(d.periodic_inspection_code,ci.periodic_inspection_code)
                LEFT JOIN tb_inspection_task initial_task ON initial_task.task_id=ii.task_id
                LEFT JOIN tb_inspection_task periodic_task ON periodic_task.task_id=pi.task_id
                LEFT JOIN tb_part part ON part.part_code=d.defect_part_code
                LEFT JOIN tb_component component ON component.component_code=ci.component_code
                """;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        exact(where, args, "d.bridge_code", params.get("bridgeCode"));
        exact(where, args, "d.defect_definition_code", params.get("defectDefinitionCode"));
        String type = params.get("inspectionType");
        if ("initial".equals(type)) where.append(" AND d.initial_inspection_code IS NOT NULL");
        if ("periodic".equals(type)) where.append(" AND (d.periodic_inspection_code IS NOT NULL OR ci.periodic_inspection_code IS NOT NULL)");
        var currentUser = SecurityUtils.currentUserOrNull();
        if ("managed".equals(params.get("scope")) && currentUser != null && currentUser.roles().contains("inspector")) {
            where.append(" AND EXISTS (SELECT 1 FROM tb_task_assignment assignment WHERE assignment.task_id=COALESCE(initial_task.task_id,periodic_task.task_id) AND assignment.user_id=?)");
            args.add(currentUser.userId());
        }
        String keyword = params.get("keyword");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (d.bridge_code LIKE ? OR b.bridge_name LIKE ? OR d.defect_type LIKE ? OR dd.defect_name LIKE ? OR d.description LIKE ?)");
            for (int i = 0; i < 5; i++) args.add("%" + keyword.trim() + "%");
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + base + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.*,b.bridge_name,bt.bridge_type_name,dd.defect_name AS dictionary_defect_name,part.part_name,component.component_name,
                       CASE WHEN d.initial_inspection_code IS NOT NULL THEN 'initial'
                            WHEN d.periodic_inspection_code IS NOT NULL OR ci.periodic_inspection_code IS NOT NULL THEN 'periodic'
                            ELSE 'manual' END inspection_type,
                       COALESCE(d.initial_inspection_code,d.periodic_inspection_code,ci.periodic_inspection_code) inspection_code,
                       COALESCE(pi.inspection_date,ii.inspection_date) inspection_date,
                       COALESCE(initial_task.task_status,periodic_task.task_status) task_status,
                       COALESCE(ii.status,pi.status) record_status
                """ + base + where + " ORDER BY d.create_time DESC,d.defect_id DESC LIMIT ? OFFSET ?", listArgs.toArray());
        return new PageResult<>(rows, total == null ? 0 : total, page, size);
    }

    private void exact(StringBuilder where, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) { where.append(" AND ").append(column).append("=?"); args.add(value.trim()); }
    }

    private void like(StringBuilder where, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) { where.append(" AND ").append(column).append(" LIKE ?"); args.add("%" + value.trim() + "%"); }
    }

    private int positive(String value, int fallback) {
        try { return Math.max(Integer.parseInt(value), 1); } catch (Exception ex) { return fallback; }
    }
}
