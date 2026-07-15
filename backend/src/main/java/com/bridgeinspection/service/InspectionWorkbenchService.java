package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.AuthenticatedUser;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the field-inspection workflow. The workbench deliberately keeps
 * initial B-table rows and periodic C-table component rows separate.
 */
@Service
public class InspectionWorkbenchService {
    private static final String INITIAL = "initial";
    private static final String PERIODIC = "periodic";
    private static final String DRAFT = "draft";
    private static final String PENDING = "pending";
    private static final String COMPLETED = "已完成";
    private static final String IN_PROGRESS = "进行中";

    private final JdbcTemplate jdbcTemplate;
    private final IdService idService;

    public InspectionWorkbenchService(JdbcTemplate jdbcTemplate, IdService idService) {
        this.jdbcTemplate = jdbcTemplate;
        this.idService = idService;
    }

    public List<Map<String, Object>> tasks(String type) {
        String normalized = normalizeType(type);
        List<Object> args = new ArrayList<>();
        args.add(normalized);
        StringBuilder sql = new StringBuilder("""
                SELECT t.task_id,t.bridge_code,b.bridge_name,bt.bridge_type_name,
                       t.inspection_type,t.inspection_level,t.plan_start_date,t.plan_end_date,
                       t.actual_start_date,t.actual_end_date,t.task_status,t.remarks,
                       COALESCE(i.initial_inspection_code,p.periodic_inspection_code) AS record_code,
                       COALESCE(i.status,p.status) AS record_status
                FROM tb_inspection_task t
                JOIN tb_bridge b ON b.bridge_code=t.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_initial_inspection i ON i.task_id=t.task_id
                LEFT JOIN tb_periodic_inspection p ON p.task_id=t.task_id
                WHERE t.inspection_type=?
                """);
        if (!isAdmin()) {
            sql.append(" AND EXISTS (SELECT 1 FROM tb_task_assignment a WHERE a.task_id=t.task_id AND a.user_id=?) ");
            args.add(currentUserId());
        }
        sql.append(" ORDER BY COALESCE(t.plan_end_date,'9999-12-31'),t.create_time DESC");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> task(String type, String taskId) {
        String normalized = normalizeType(type);
        Map<String, Object> task = accessibleTask(normalized, taskId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("bridge", bridge(String.valueOf(task.get("bridge_code"))));
        result.put("inspectionType", normalized);
        result.put("defectDefinitions", defectDefinitions(normalized));
        if (INITIAL.equals(normalized)) {
            result.put("record", initialRecord(taskId));
            result.put("rows", initialRows(taskId, String.valueOf(task.get("bridge_code"))));
        } else {
            ensurePeriodicComponents(String.valueOf(task.get("bridge_code")));
            result.put("record", periodicRecord(taskId));
            result.put("rows", periodicRows(taskId, String.valueOf(task.get("bridge_code"))));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> save(String type, String taskId, Map<String, Object> body, boolean finalize) {
        String normalized = normalizeType(type);
        Map<String, Object> task = accessibleTask(normalized, taskId);
        assertTaskEditable(task);
        Map<String, Object> record = body == null || !(body.get("record") instanceof Map<?, ?>)
                ? new LinkedHashMap<>() : castMap(body.get("record"));
        record.put("status", finalize ? PENDING : DRAFT);
        List<Map<String, Object>> rows = body == null ? List.of() : mapList(body.get("rows"));
        String recordCode;
        if (INITIAL.equals(normalized)) {
            recordCode = saveInitialRecord(task, record);
            for (Map<String, Object> row : rows) saveInitialRow(recordCode, row);
        } else {
            recordCode = savePeriodicRecord(task, record);
            for (Map<String, Object> row : rows) savePeriodicRow(recordCode, row);
        }
        if (finalize) completeTask(taskId);
        else startTask(taskId);
        Map<String, Object> result = task(normalized, taskId);
        result.put("savedRecordCode", recordCode);
        result.put("submitted", finalize);
        return result;
    }

    private String saveInitialRecord(Map<String, Object> task, Map<String, Object> record) {
        String taskId = String.valueOf(task.get("task_id"));
        String bridgeCode = String.valueOf(task.get("bridge_code"));
        Map<String, Object> existing = queryOptional("SELECT * FROM tb_initial_inspection WHERE task_id=?", taskId);
        String code = existing == null ? idService.next("QI") : string(existing.get("initial_inspection_code"));
        String date = value(record, "inspection_date", "inspectionDate");
        if (date.isBlank()) date = LocalDate.now().toString();
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO tb_initial_inspection
                    (initial_inspection_code,task_id,bridge_code,inspection_date,inspection_org,inspectors,
                     bridge_engineer,weather_temperature,main_span_structure,maximum_span,span_combination,
                     structure_form,construction_method,construction_rework,reinforcement_info,
                     missing_archive_drawings,defect_advice,status,effective_flag,record_form_no,create_by)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, code, taskId, bridgeCode, date, nullable(record, "inspection_org", "inspectionOrg"),
                    nullable(record, "inspectors"), nullable(record, "bridge_engineer", "bridgeEngineer"),
                    nullable(record, "weather_temperature", "weatherTemperature"), nullable(record, "main_span_structure", "mainSpanStructure"),
                    decimalOrNull(record, "maximum_span", "maximumSpan"), nullable(record, "span_combination", "spanCombination"),
                    nullable(record, "structure_form", "structureForm"), nullable(record, "construction_method", "constructionMethod"),
                    nullable(record, "construction_rework", "constructionRework"), nullable(record, "reinforcement_info", "reinforcementInfo"),
                    nullable(record, "missing_archive_drawings", "missingArchiveDrawings"), nullable(record, "defect_advice", "defectAdvice"),
                    nullable(record, "status"), numberOrDefault(record, 1, "effective_flag", "effectiveFlag"),
                    "B-" + date.replace("-", "") + "-" + bridgeCode + "-" + code, userIdInt());
        } else {
            jdbcTemplate.update("""
                    UPDATE tb_initial_inspection SET inspection_date=?,inspection_org=?,inspectors=?,bridge_engineer=?,
                    weather_temperature=?,main_span_structure=?,maximum_span=?,span_combination=?,structure_form=?,
                    construction_method=?,construction_rework=?,reinforcement_info=?,missing_archive_drawings=?,
                    defect_advice=?,status=?,effective_flag=?,update_time=CURRENT_TIMESTAMP WHERE task_id=?
                    """, date, nullable(record, "inspection_org", "inspectionOrg"), nullable(record, "inspectors"),
                    nullable(record, "bridge_engineer", "bridgeEngineer"), nullable(record, "weather_temperature", "weatherTemperature"),
                    nullable(record, "main_span_structure", "mainSpanStructure"), decimalOrNull(record, "maximum_span", "maximumSpan"),
                    nullable(record, "span_combination", "spanCombination"), nullable(record, "structure_form", "structureForm"),
                    nullable(record, "construction_method", "constructionMethod"), nullable(record, "construction_rework", "constructionRework"),
                    nullable(record, "reinforcement_info", "reinforcementInfo"), nullable(record, "missing_archive_drawings", "missingArchiveDrawings"),
                    nullable(record, "defect_advice", "defectAdvice"), nullable(record, "status"),
                    numberOrDefault(record, 1, "effective_flag", "effectiveFlag"), taskId);
        }
        return code;
    }

    private void saveInitialRow(String inspectionCode, Map<String, Object> row) {
        String itemCode = value(row, "item_code", "itemCode");
        if (itemCode.isBlank()) return;
        String defectCode = nullable(row, "defect_definition_code", "defectDefinitionCode");
        Integer id = queryId("SELECT item_record_id FROM tb_initial_inspection_item WHERE initial_inspection_code=? AND item_code=?", inspectionCode, itemCode);
        if (id == null) {
            jdbcTemplate.update("""
                    INSERT INTO tb_initial_inspection_item
                    (initial_inspection_code,item_code,applicable_flag,trigger_result,measured_value,inspection_description,defect_definition_code)
                    VALUES (?,?,?,?,?,?,?)
                    """, inspectionCode, itemCode, numberOrDefault(row, 1, "applicable_flag", "applicableFlag"),
                    nullable(row, "trigger_result", "triggerResult"), nullable(row, "measured_value", "measuredValue"),
                    nullable(row, "inspection_description", "inspectionDescription"), defectCode);
        } else {
            jdbcTemplate.update("""
                    UPDATE tb_initial_inspection_item SET applicable_flag=?,trigger_result=?,measured_value=?,
                    inspection_description=?,defect_definition_code=? WHERE item_record_id=?
                    """, numberOrDefault(row, 1, "applicable_flag", "applicableFlag"),
                    nullable(row, "trigger_result", "triggerResult"), nullable(row, "measured_value", "measuredValue"),
                    nullable(row, "inspection_description", "inspectionDescription"), defectCode, id);
        }
        if (defectCode != null) upsertInitialDefect(inspectionCode, row, defectCode);
    }

    private String savePeriodicRecord(Map<String, Object> task, Map<String, Object> record) {
        String taskId = String.valueOf(task.get("task_id"));
        String bridgeCode = String.valueOf(task.get("bridge_code"));
        Map<String, Object> existing = queryOptional("SELECT * FROM tb_periodic_inspection WHERE task_id=?", taskId);
        String code = existing == null ? idService.next("QP") : string(existing.get("periodic_inspection_code"));
        String date = value(record, "inspection_date", "inspectionDate");
        if (date.isBlank()) date = LocalDate.now().toString();
        String rating = nullable(record, "rating_level_code", "ratingLevelCode");
        String tableCode = tableCode(String.valueOf(bridge(String.valueOf(task.get("bridge_code"))).get("bridge_type_code")));
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO tb_periodic_inspection
                    (periodic_inspection_code,task_id,form_table_code,bridge_code,inspection_date,last_inspection_date,
                     last_maintenance_date,weather_temperature,rating_level_code,cleanliness,maintenance_status,
                     next_inspection_date,recorder,principal,status,create_by)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, code, taskId, tableCode, bridgeCode, date, nullable(record, "last_inspection_date", "lastInspectionDate"),
                    nullable(record, "last_maintenance_date", "lastMaintenanceDate"), nullable(record, "weather_temperature", "weatherTemperature"),
                    rating, nullable(record, "cleanliness"), nullable(record, "maintenance_status", "maintenanceStatus"),
                    nullable(record, "next_inspection_date", "nextInspectionDate"), nullable(record, "recorder"), nullable(record, "principal"),
                    nullable(record, "status"), userIdInt());
        } else {
            jdbcTemplate.update("""
                    UPDATE tb_periodic_inspection SET inspection_date=?,last_inspection_date=?,last_maintenance_date=?,
                    weather_temperature=?,rating_level_code=?,cleanliness=?,maintenance_status=?,next_inspection_date=?,
                    recorder=?,principal=?,status=?,update_time=CURRENT_TIMESTAMP WHERE task_id=?
                    """, date, nullable(record, "last_inspection_date", "lastInspectionDate"), nullable(record, "last_maintenance_date", "lastMaintenanceDate"),
                    nullable(record, "weather_temperature", "weatherTemperature"), rating, nullable(record, "cleanliness"),
                    nullable(record, "maintenance_status", "maintenanceStatus"), nullable(record, "next_inspection_date", "nextInspectionDate"),
                    nullable(record, "recorder"), nullable(record, "principal"), nullable(record, "status"), taskId);
        }
        return code;
    }

    private void savePeriodicRow(String inspectionCode, Map<String, Object> row) {
        Integer componentId = integerOrNull(row, "bridge_component_id", "bridgeComponentId");
        if (componentId == null) return;
        String defectCode = nullable(row, "defect_definition_code", "defectDefinitionCode");
        Integer id = queryId("SELECT component_inspection_id FROM tb_component_inspection WHERE periodic_inspection_code=? AND bridge_component_id=?", inspectionCode, componentId);
        Object[] values = {
                nullable(row, "part_code", "partCode"), nullable(row, "component_code", "componentCode"),
                nullable(row, "defect_type", "defectType"), nullable(row, "defect_location", "defectLocation"),
                nullable(row, "defect_range", "defectRange"), nullable(row, "defect_degree_code", "defectDegreeCode"),
                nullable(row, "worst_component", "worstComponent"), decimalOrNull(row, "score"),
                nullable(row, "maintenance_advice", "maintenanceAdvice"), numberOrDefault(row, 0, "special_check_required", "specialCheckRequired"), defectCode
        };
        if (id == null) {
            Object[] insertValues = new Object[13];
            insertValues[0] = inspectionCode;
            insertValues[1] = componentId;
            System.arraycopy(values, 0, insertValues, 2, values.length);
            jdbcTemplate.update("""
                    INSERT INTO tb_component_inspection
                    (periodic_inspection_code,bridge_component_id,part_code,component_code,defect_type,defect_location,
                     defect_range,defect_degree_code,worst_component,score,maintenance_advice,special_check_required,defect_definition_code)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, insertValues);
        } else {
            jdbcTemplate.update("""
                    UPDATE tb_component_inspection SET part_code=?,component_code=?,defect_type=?,defect_location=?,
                    defect_range=?,defect_degree_code=?,worst_component=?,score=?,maintenance_advice=?,
                    special_check_required=?,defect_definition_code=? WHERE component_inspection_id=?
                    """, values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], values[9], values[10], id);
        }
        if (defectCode != null) upsertPeriodicDefect(inspectionCode, componentId, row, defectCode);
    }

    private void upsertInitialDefect(String inspectionCode, Map<String, Object> row, String definitionCode) {
        Map<String, Object> definition = definition(definitionCode);
        Integer existing = queryId("SELECT defect_id FROM tb_defect WHERE initial_inspection_code=? AND defect_definition_code=? AND component_inspection_id IS NULL LIMIT 1", inspectionCode, definitionCode);
        Object[] values = defectValues(definition, row);
        String partCode = nullable(row, "defect_part_code", "partCode");
        if (partCode == null) partCode = nullable(definition, "applicable_part_code");
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO tb_defect
                    (bridge_code,initial_inspection_code,defect_definition_code,defect_part_code,defect_type,defect_nature,
                     defect_range,defect_quantity,defect_degree_code,description)
                    SELECT bridge_code,?,?,?,?,?,?,?,?,? FROM tb_initial_inspection WHERE initial_inspection_code=?
                    """, inspectionCode, definitionCode, partCode, values[0], values[1], values[2], values[3], values[4], values[5], inspectionCode);
        } else {
            jdbcTemplate.update("UPDATE tb_defect SET defect_part_code=?,defect_type=?,defect_nature=?,defect_range=?,defect_quantity=?,defect_degree_code=?,description=? WHERE defect_id=?",
                    partCode, values[0], values[1], values[2], values[3], values[4], values[5], existing);
        }
    }

    private void upsertPeriodicDefect(String inspectionCode, Integer componentId, Map<String, Object> row, String definitionCode) {
        Map<String, Object> definition = definition(definitionCode);
        Integer componentInspectionId = queryId("SELECT component_inspection_id FROM tb_component_inspection WHERE periodic_inspection_code=? AND bridge_component_id=?", inspectionCode, componentId);
        if (componentInspectionId == null) return;
        Integer existing = queryId("SELECT defect_id FROM tb_defect WHERE periodic_inspection_code=? AND defect_definition_code=? AND component_inspection_id=? LIMIT 1", inspectionCode, definitionCode, componentInspectionId);
        Object[] values = defectValues(definition, row);
        String partCode = nullable(row, "part_code", "partCode");
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO tb_defect
                    (bridge_code,periodic_inspection_code,component_inspection_id,defect_definition_code,defect_part_code,
                     defect_type,defect_nature,defect_range,defect_quantity,defect_degree_code,description)
                    SELECT p.bridge_code,?,?,?,?,?,?,?,?,?,? FROM tb_periodic_inspection p WHERE p.periodic_inspection_code=?
                    """, inspectionCode, componentInspectionId, definitionCode, partCode, values[0], values[1], values[2], values[3], values[4], values[5], inspectionCode);
        } else {
            jdbcTemplate.update("UPDATE tb_defect SET defect_part_code=?,defect_type=?,defect_nature=?,defect_range=?,defect_quantity=?,defect_degree_code=?,description=? WHERE defect_id=?",
                    partCode, values[0], values[1], values[2], values[3], values[4], values[5], existing);
        }
    }

    private Object[] defectValues(Map<String, Object> definition, Map<String, Object> row) {
        String name = string(definition.get("defect_name"));
        String nature = nullable(row, "defect_nature", "defectNature");
        if (nature == null) nature = string(definition.get("defect_nature"));
        String range = nullable(row, "defect_range", "defectRange");
        if (range == null) range = string(definition.get("default_range"));
        String quantity = nullable(row, "defect_quantity", "defectQuantity");
        String degree = nullable(row, "defect_degree_code", "defectDegreeCode");
        if (degree == null) degree = string(definition.get("default_degree_code"));
        String description = nullable(row, "description", "inspection_description", "inspectionDescription");
        if (description == null) description = string(definition.get("default_advice"));
        return new Object[]{name, nature, range, quantity, degree, description, string(definition.get("applicable_part_code"))};
    }

    private void ensurePeriodicComponents(String bridgeCode) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO tb_bridge_specific_component
                (bridge_code,config_id,part_code,component_code,component_serial,quantity,custom_flag)
                SELECT ?,cfg.config_id,cfg.part_code,cfg.component_code,CONCAT('STD-',cfg.config_id),1,0
                FROM tb_bridge b JOIN tb_bridge_type_component_config cfg ON cfg.bridge_type_code=b.bridge_type_code
                WHERE b.bridge_code=? AND cfg.active_flag=1
                """, bridgeCode, bridgeCode);
    }

    private List<Map<String, Object>> initialRows(String taskId, String bridgeCode) {
        return jdbcTemplate.queryForList("""
                SELECT c.item_code,d.item_name,d.unit,d.item_category,d.description AS item_definition_description,
                       c.requirement_type,c.trigger_condition,c.display_order,
                       COALESCE(r.applicable_flag,1) applicable_flag,r.trigger_result,r.measured_value,
                       r.inspection_description,r.defect_definition_code
                FROM tb_bridge b
                JOIN tb_bridge_type_initial_item_config c ON c.bridge_type_code=b.bridge_type_code
                JOIN tb_initial_inspection_item_definition d ON d.item_code=c.item_code
                LEFT JOIN tb_initial_inspection i ON i.task_id=?
                LEFT JOIN tb_initial_inspection_item r ON r.initial_inspection_code=i.initial_inspection_code AND r.item_code=c.item_code
                WHERE b.bridge_code=? ORDER BY c.display_order,d.item_code
                """, taskId, bridgeCode);
    }

    private List<Map<String, Object>> periodicRows(String taskId, String bridgeCode) {
        return jdbcTemplate.queryForList("""
                SELECT c.bridge_component_id,c.part_code,p.part_name,c.component_code,co.component_name,
                       c.component_serial,c.location_desc,ci.score,ci.defect_type,ci.defect_location,ci.defect_range,
                       ci.defect_degree_code,ci.worst_component,ci.maintenance_advice,ci.special_check_required,
                       ci.defect_definition_code
                FROM tb_bridge_specific_component c
                JOIN tb_part p ON p.part_code=c.part_code
                JOIN tb_component co ON co.component_code=c.component_code
                LEFT JOIN tb_periodic_inspection i ON i.task_id=?
                LEFT JOIN tb_component_inspection ci ON ci.periodic_inspection_code=i.periodic_inspection_code
                    AND ci.bridge_component_id=c.bridge_component_id
                WHERE c.bridge_code=? ORDER BY p.sort_order,c.component_serial
                """, taskId, bridgeCode);
    }

    private Map<String, Object> initialRecord(String taskId) {
        Map<String, Object> row = queryOptional("SELECT * FROM tb_initial_inspection WHERE task_id=?", taskId);
        return row == null ? new LinkedHashMap<>() : row;
    }

    private Map<String, Object> periodicRecord(String taskId) {
        Map<String, Object> row = queryOptional("SELECT * FROM tb_periodic_inspection WHERE task_id=?", taskId);
        return row == null ? new LinkedHashMap<>() : row;
    }

    private List<Map<String, Object>> defectDefinitions(String type) {
        return jdbcTemplate.queryForList("""
                SELECT d.defect_definition_code,d.defect_name,d.inspection_scope,d.defect_nature,
                       d.default_degree_code,d.default_range,d.default_advice,d.applicable_part_code
                FROM tb_defect_definition d
                WHERE d.active_flag=1 AND (d.inspection_scope=? OR d.inspection_scope='both')
                ORDER BY d.display_order,d.defect_definition_code
                """, type);
    }

    private Map<String, Object> definition(String code) {
        Map<String, Object> row = queryOptional("SELECT * FROM tb_defect_definition WHERE defect_definition_code=? AND active_flag=1", code);
        if (row == null) throw new BusinessException("病害字典项目不存在或已停用");
        return row;
    }

    private Map<String, Object> accessibleTask(String type, String taskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT t.*,b.bridge_name,b.bridge_type_code,bt.bridge_type_name
                FROM tb_inspection_task t JOIN tb_bridge b ON b.bridge_code=t.bridge_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                WHERE t.task_id=? AND t.inspection_type=?
                """, taskId, type);
        if (rows.isEmpty()) throw new BusinessException("检查任务不存在");
        if (!isAdmin()) {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_task_assignment WHERE task_id=? AND user_id=?", Integer.class, taskId, currentUserId());
            if (count == null || count == 0) throw new BusinessException(403, "该任务未分配给当前检查人员");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    private void assertTaskEditable(Map<String, Object> task) {
        String status = string(task.get("task_status"));
        if (List.of("已完成", "已审核", "已取消").contains(status)) {
            throw new BusinessException("任务已完成，检查记录不能继续编辑");
        }
    }

    private Map<String, Object> bridge(String bridgeCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT b.bridge_code,b.bridge_name,b.bridge_type_code,bt.bridge_type_name,b.route_code,
                       r.route_name FROM tb_bridge b LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                LEFT JOIN tb_route r ON r.route_code=b.route_code WHERE b.bridge_code=?
                """, bridgeCode);
        if (rows.isEmpty()) throw new BusinessException("桥梁不存在");
        return new LinkedHashMap<>(rows.get(0));
    }

    private void startTask(String taskId) {
        jdbcTemplate.update("UPDATE tb_inspection_task SET task_status=?,actual_start_date=COALESCE(actual_start_date,CURDATE()),update_time=CURRENT_TIMESTAMP WHERE task_id=?", IN_PROGRESS, taskId);
        jdbcTemplate.update("UPDATE tb_task_assignment SET assignment_status='进行中',accept_time=COALESCE(accept_time,CURRENT_TIMESTAMP) WHERE task_id=? AND user_id=?", taskId, currentUserId());
    }

    private void completeTask(String taskId) {
        jdbcTemplate.update("UPDATE tb_inspection_task SET task_status=?,actual_end_date=CURDATE(),update_time=CURRENT_TIMESTAMP WHERE task_id=?", COMPLETED, taskId);
        jdbcTemplate.update("UPDATE tb_task_assignment SET assignment_status=?,complete_time=CURRENT_TIMESTAMP WHERE task_id=? AND user_id=?", COMPLETED, taskId, currentUserId());
    }

    private String normalizeType(String type) {
        if (INITIAL.equalsIgnoreCase(type)) return INITIAL;
        if (PERIODIC.equalsIgnoreCase(type)) return PERIODIC;
        throw new BusinessException("检查类型只能是 initial 或 periodic");
    }

    private boolean isAdmin() {
        AuthenticatedUser user = SecurityUtils.currentUserOrNull();
        return user != null && user.roles().contains("admin");
    }

    private String currentUserId() {
        String id = SecurityUtils.currentUserId();
        if (id == null) throw new BusinessException(401, "请先登录");
        return id;
    }

    private Integer userIdInt() {
        try { return Integer.valueOf(currentUserId()); } catch (NumberFormatException ex) { return null; }
    }

    private String tableCode(String bridgeType) {
        return switch (bridgeType) {
            case "beam" -> "C-1"; case "arch" -> "C-2"; case "rigid_arch" -> "C-3";
            case "composite_arch" -> "C-4"; case "cable_stayed" -> "C-5"; case "suspension" -> "C-6";
            default -> "C-7";
        };
    }

    private Map<String, Object> queryOptional(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : new LinkedHashMap<>(rows.get(0));
    }

    private Integer queryId(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        if (rows.isEmpty() || rows.get(0).values().isEmpty()) return null;
        Object value = rows.get(0).values().iterator().next();
        return value == null ? null : ((Number) value).intValue();
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(this::castMap).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) { return new LinkedHashMap<>((Map<String, Object>) value); }

    private String value(Map<String, Object> row, String... keys) {
        for (String key : keys) if (row.containsKey(key) && row.get(key) != null) return String.valueOf(row.get(key)).trim();
        return "";
    }

    private String nullable(Map<String, Object> row, String... keys) {
        String value = value(row, keys); return value.isBlank() ? null : value;
    }

    private Integer numberOrDefault(Map<String, Object> row, int fallback, String... keys) {
        String value = value(row, keys); if (value.isBlank()) return fallback;
        try { return Integer.valueOf(value); } catch (NumberFormatException ex) { return fallback; }
    }

    private Integer integerOrNull(Map<String, Object> row, String... keys) {
        String value = value(row, keys); if (value.isBlank()) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException ex) { return null; }
    }

    private Object decimalOrNull(Map<String, Object> row, String... keys) {
        String value = value(row, keys); if (value.isBlank()) return null;
        try { return new java.math.BigDecimal(value); } catch (NumberFormatException ex) { return null; }
    }

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
}
