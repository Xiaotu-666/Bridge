package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InspectionReviewService {
    private static final String INITIAL = "initial";
    private static final String PERIODIC = "periodic";

    private final JdbcTemplate jdbcTemplate;
    private final TaskService taskService;

    public InspectionReviewService(JdbcTemplate jdbcTemplate, TaskService taskService) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskService = taskService;
    }

    public Map<String, Object> list(String requestedType, String requestedState) {
        String type = normalizeType(requestedType);
        String state = normalizeState(requestedState);
        String statusFilter = "all".equals(state) ? "" : " AND i.status=?";
        Object[] args = "all".equals(state) ? new Object[]{} : new Object[]{state};

        String sql = INITIAL.equals(type) ? """
                SELECT i.initial_inspection_code AS inspection_code,i.task_id,i.bridge_code,b.bridge_name,
                       COALESCE(bt.bridge_type_name,'其他') bridge_type_name,i.inspection_date,
                       COALESCE(i.record_form_no,'B表') form_code,i.status,t.task_status,
                       i.inspectors AS inspector_name,i.reviewer_id,u.user_name reviewer_name,
                       i.review_opinion,i.review_time,i.archive_time,a.archive_id,i.update_time,i.create_time,
                       (SELECT COUNT(*) FROM tb_initial_inspection_item r
                        WHERE r.initial_inspection_code=i.initial_inspection_code) detail_count,
                       (SELECT COUNT(*) FROM tb_defect d
                        WHERE d.initial_inspection_code=i.initial_inspection_code) defect_count,
                       (SELECT COUNT(*) FROM tb_report r
                        WHERE r.initial_inspection_code=i.initial_inspection_code OR r.task_id=i.task_id) report_count
                FROM tb_initial_inspection i
                JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_inspection_task t ON t.task_id=i.task_id
                LEFT JOIN tb_user u ON u.user_id=i.reviewer_id
                LEFT JOIN tb_inspection_archive a ON a.inspection_type='initial'
                    AND a.record_code=i.initial_inspection_code
                WHERE 1=1
                """ + statusFilter + " ORDER BY COALESCE(i.review_time,i.update_time,i.create_time) DESC" : """
                SELECT i.periodic_inspection_code AS inspection_code,i.task_id,i.bridge_code,b.bridge_name,
                       COALESCE(bt.bridge_type_name,'其他') bridge_type_name,i.inspection_date,
                       COALESCE(i.record_form_no,i.form_table_code,'C-7') form_code,i.form_table_code,
                       i.status,t.task_status,i.recorder AS inspector_name,i.reviewer_id,u.user_name reviewer_name,
                       i.review_opinion,i.review_time,i.archive_time,a.archive_id,i.update_time,i.create_time,
                       (SELECT COUNT(*) FROM tb_component_inspection r
                        WHERE r.periodic_inspection_code=i.periodic_inspection_code) detail_count,
                       (SELECT COUNT(*) FROM tb_defect d
                        WHERE d.periodic_inspection_code=i.periodic_inspection_code
                           OR d.component_inspection_id IN (
                               SELECT ci.component_inspection_id FROM tb_component_inspection ci
                               WHERE ci.periodic_inspection_code=i.periodic_inspection_code)) defect_count,
                       (SELECT COUNT(*) FROM tb_report r
                        WHERE r.periodic_inspection_code=i.periodic_inspection_code OR r.task_id=i.task_id) report_count
                FROM tb_periodic_inspection i
                JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_inspection_task t ON t.task_id=i.task_id
                LEFT JOIN tb_user u ON u.user_id=i.reviewer_id
                LEFT JOIN tb_inspection_archive a ON a.inspection_type='periodic'
                    AND a.record_code=i.periodic_inspection_code
                WHERE 1=1
                """ + statusFilter + " ORDER BY COALESCE(i.review_time,i.update_time,i.create_time) DESC";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, args);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("pending", count(type, "pending"));
        counts.put("archived", count(type, "archived"));
        counts.put("rejected", count(type, "rejected"));
        return Map.of("records", records, "counts", counts);
    }

    public Map<String, Object> detail(String requestedType, String recordCode) {
        String type = normalizeType(requestedType);
        Map<String, Object> record = record(type, recordCode);
        List<Map<String, Object>> rows;
        List<Map<String, Object>> defects;
        List<Map<String, Object>> attachments;

        if (INITIAL.equals(type)) {
            rows = jdbcTemplate.queryForList("""
                    SELECT r.item_record_id,r.item_code,d.item_category,d.item_name,d.unit,
                           r.applicable_flag,r.trigger_result,r.measured_value,r.inspection_description,
                           r.defect_definition_code,dd.defect_name
                    FROM tb_initial_inspection_item r
                    JOIN tb_initial_inspection_item_definition d ON d.item_code=r.item_code
                    LEFT JOIN tb_defect_definition dd ON dd.defect_definition_code=r.defect_definition_code
                    WHERE r.initial_inspection_code=?
                    ORDER BY d.item_category,r.item_record_id
                    """, recordCode);
            defects = jdbcTemplate.queryForList("""
                    SELECT d.*,p.part_name,dd.defect_name AS dictionary_defect_name
                    FROM tb_defect d LEFT JOIN tb_part p ON p.part_code=d.defect_part_code
                    LEFT JOIN tb_defect_definition dd ON dd.defect_definition_code=d.defect_definition_code
                    WHERE d.initial_inspection_code=? ORDER BY d.defect_id
                    """, recordCode);
            attachments = jdbcTemplate.queryForList("""
                    SELECT file_id,file_name,file_type,file_size,file_description,photo_category,upload_time
                    FROM tb_attachment WHERE initial_inspection_code=? ORDER BY upload_time
                    """, recordCode);
        } else {
            rows = jdbcTemplate.queryForList("""
                    SELECT r.component_inspection_id,r.bridge_component_id,r.part_code,p.part_name,
                           r.component_code,c.component_name,bc.component_serial,bc.location_desc,
                           r.score,r.defect_type,r.defect_location,r.defect_range,r.defect_degree_code,
                           r.worst_component,r.maintenance_advice,r.special_check_required,
                           r.defect_definition_code,dd.defect_name
                    FROM tb_component_inspection r
                    LEFT JOIN tb_part p ON p.part_code=r.part_code
                    LEFT JOIN tb_component c ON c.component_code=r.component_code
                    LEFT JOIN tb_bridge_specific_component bc ON bc.bridge_component_id=r.bridge_component_id
                    LEFT JOIN tb_defect_definition dd ON dd.defect_definition_code=r.defect_definition_code
                    WHERE r.periodic_inspection_code=?
                    ORDER BY p.sort_order,bc.component_serial,r.component_inspection_id
                    """, recordCode);
            defects = jdbcTemplate.queryForList("""
                    SELECT d.*,p.part_name,c.component_name,dd.defect_name AS dictionary_defect_name
                    FROM tb_defect d
                    LEFT JOIN tb_component_inspection ci ON ci.component_inspection_id=d.component_inspection_id
                    LEFT JOIN tb_part p ON p.part_code=d.defect_part_code
                    LEFT JOIN tb_component c ON c.component_code=ci.component_code
                    LEFT JOIN tb_defect_definition dd ON dd.defect_definition_code=d.defect_definition_code
                    WHERE d.periodic_inspection_code=? OR ci.periodic_inspection_code=?
                    ORDER BY d.defect_id
                    """, recordCode, recordCode);
            attachments = jdbcTemplate.queryForList("""
                    SELECT DISTINCT a.file_id,a.file_name,a.file_type,a.file_size,a.file_description,
                           a.photo_category,a.upload_time
                    FROM tb_attachment a
                    JOIN tb_component_inspection ci ON ci.component_inspection_id=a.component_inspection_id
                    WHERE ci.periodic_inspection_code=? ORDER BY a.upload_time
                    """, recordCode);
        }

        List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
                SELECT report_id,report_type,version_no,file_format,file_path,report_status,generation_time,change_summary
                FROM tb_report
                WHERE (initial_inspection_code=? OR periodic_inspection_code=? OR task_id=?)
                  AND UPPER(COALESCE(file_format,''))='PDF'
                ORDER BY generation_time DESC
                """, INITIAL.equals(type) ? recordCode : null,
                PERIODIC.equals(type) ? recordCode : null, record.get("task_id"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("rows", rows);
        result.put("defects", defects);
        result.put("reports", reports);
        result.put("attachments", attachments);
        return result;
    }

    @Transactional
    public void approve(String requestedType, String recordCode, String opinion) {
        String type = normalizeType(requestedType);
        Map<String, Object> record = pendingRecord(type, recordCode);
        int reviewerId = currentUserId();
        String finalOpinion = blank(opinion) ? "审核通过，同意归档" : opinion.trim();
        String table = table(type);
        String codeColumn = codeColumn(type);

        jdbcTemplate.update("UPDATE " + table + " SET status='archived',reviewer_id=?,review_opinion=?," +
                        "review_time=CURRENT_TIMESTAMP,archive_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP WHERE " + codeColumn + "=?",
                reviewerId, finalOpinion, recordCode);
        String taskId = value(record.get("task_id"));
        if (!taskId.isBlank()) {
            taskService.review(taskId, finalOpinion);
            jdbcTemplate.update("UPDATE tb_inspection_task SET reviewer_id=? WHERE task_id=?", reviewerId, taskId);
        }
        jdbcTemplate.update("""
                INSERT INTO tb_inspection_archive
                    (inspection_type,record_code,bridge_code,task_id,archived_by,archived_time,review_opinion)
                VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,?)
                ON DUPLICATE KEY UPDATE archived_by=VALUES(archived_by),archived_time=CURRENT_TIMESTAMP,
                    review_opinion=VALUES(review_opinion),task_id=VALUES(task_id)
                """, type, recordCode, record.get("bridge_code"), nullable(taskId), reviewerId, finalOpinion);
        jdbcTemplate.update("""
                UPDATE tb_report SET report_status='已归档',reviewer_id=?
                WHERE initial_inspection_code=? OR periodic_inspection_code=? OR task_id=?
                """, reviewerId, INITIAL.equals(type) ? recordCode : null,
                PERIODIC.equals(type) ? recordCode : null, nullable(taskId));
    }

    @Transactional
    public void reject(String requestedType, String recordCode, String reason) {
        if (blank(reason)) throw new BusinessException("请填写打回原因");
        String type = normalizeType(requestedType);
        Map<String, Object> record = pendingRecord(type, recordCode);
        int reviewerId = currentUserId();
        jdbcTemplate.update("UPDATE " + table(type) + " SET status='rejected',reviewer_id=?,review_opinion=?," +
                        "review_time=CURRENT_TIMESTAMP,archive_time=NULL,update_time=CURRENT_TIMESTAMP WHERE " + codeColumn(type) + "=?",
                reviewerId, reason.trim(), recordCode);
        String taskId = value(record.get("task_id"));
        if (!taskId.isBlank()) {
            taskService.reject(taskId, reason.trim());
            jdbcTemplate.update("UPDATE tb_inspection_task SET reviewer_id=? WHERE task_id=?", reviewerId, taskId);
            jdbcTemplate.update("UPDATE tb_task_assignment SET assignment_status='进行中',complete_time=NULL WHERE task_id=? AND assignment_status='已完成'", taskId);
        }
        jdbcTemplate.update("DELETE FROM tb_inspection_archive WHERE inspection_type=? AND record_code=?", type, recordCode);
        jdbcTemplate.update("""
                UPDATE tb_report SET report_status='待修改',reviewer_id=?
                WHERE initial_inspection_code=? OR periodic_inspection_code=? OR task_id=?
                """, reviewerId, INITIAL.equals(type) ? recordCode : null,
                PERIODIC.equals(type) ? recordCode : null, nullable(taskId));
    }

    private Map<String, Object> pendingRecord(String type, String recordCode) {
        Map<String, Object> record = record(type, recordCode);
        if (!"pending".equals(value(record.get("status")))) {
            throw new BusinessException("只有待审核的检查表可以执行审核操作");
        }
        return record;
    }

    private Map<String, Object> record(String type, String recordCode) {
        String sql = INITIAL.equals(type) ? """
                SELECT i.*,i.initial_inspection_code AS inspection_code,
                       COALESCE(i.record_form_no,'B表') form_code,b.bridge_name,b.bridge_type_code,
                       COALESCE(bt.bridge_type_name,'其他') bridge_type_name,b.route_code,r.route_name,
                       t.task_status,u.user_name reviewer_name,a.archive_id
                FROM tb_initial_inspection i JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_route r ON r.route_code=b.route_code
                LEFT JOIN tb_inspection_task t ON t.task_id=i.task_id
                LEFT JOIN tb_user u ON u.user_id=i.reviewer_id
                LEFT JOIN tb_inspection_archive a ON a.inspection_type='initial'
                    AND a.record_code=i.initial_inspection_code
                WHERE i.initial_inspection_code=?
                """ : """
                SELECT i.*,i.periodic_inspection_code AS inspection_code,
                       COALESCE(i.record_form_no,i.form_table_code,'C-7') form_code,
                       b.bridge_name,b.bridge_type_code,COALESCE(bt.bridge_type_name,'其他') bridge_type_name,
                       b.route_code,r.route_name,t.task_status,u.user_name reviewer_name,a.archive_id
                FROM tb_periodic_inspection i JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_route r ON r.route_code=b.route_code
                LEFT JOIN tb_inspection_task t ON t.task_id=i.task_id
                LEFT JOIN tb_user u ON u.user_id=i.reviewer_id
                LEFT JOIN tb_inspection_archive a ON a.inspection_type='periodic'
                    AND a.record_code=i.periodic_inspection_code
                WHERE i.periodic_inspection_code=?
                """;
        try {
            return new LinkedHashMap<>(jdbcTemplate.queryForMap(sql, recordCode));
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("检查记录不存在");
        }
    }

    private long count(String type, String state) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table(type) + " WHERE status=?", Long.class, state);
        return count == null ? 0 : count;
    }

    private String normalizeType(String type) {
        if (INITIAL.equalsIgnoreCase(type)) return INITIAL;
        if (PERIODIC.equalsIgnoreCase(type)) return PERIODIC;
        throw new BusinessException("检查类型只能是 initial 或 periodic");
    }

    private String normalizeState(String state) {
        if (blank(state)) return "pending";
        String normalized = state.trim().toLowerCase();
        if (List.of("pending", "archived", "rejected", "all").contains(normalized)) return normalized;
        throw new BusinessException("审核状态不正确");
    }

    private String table(String type) {
        return INITIAL.equals(type) ? "tb_initial_inspection" : "tb_periodic_inspection";
    }

    private String codeColumn(String type) {
        return INITIAL.equals(type) ? "initial_inspection_code" : "periodic_inspection_code";
    }

    private int currentUserId() {
        try {
            return Integer.parseInt(SecurityUtils.currentUserId());
        } catch (Exception ex) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Object nullable(String value) {
        return blank(value) ? null : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
