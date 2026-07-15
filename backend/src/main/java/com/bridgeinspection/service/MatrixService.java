package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MatrixService {
    private final JdbcTemplate jdbcTemplate;

    public MatrixService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> componentMatrix(String bridgeTypeCode) {
        return jdbcTemplate.queryForList("""
                SELECT c.config_id, c.bridge_type_code, bt.bridge_type_name, c.part_code, p.part_name,
                       c.component_code, cp.component_name, c.display_order
                FROM tb_bridge_type_component_config c
                JOIN tb_bridge_type bt ON bt.bridge_type_code = c.bridge_type_code
                JOIN tb_part p ON p.part_code = c.part_code
                JOIN tb_component cp ON cp.component_code = c.component_code
                WHERE c.active_flag = 1 AND (? IS NULL OR c.bridge_type_code = ?)
                ORDER BY bt.bridge_type_code, p.sort_order, c.display_order
                """, bridgeTypeCode, bridgeTypeCode);
    }

    public List<Map<String, Object>> initialItemMatrix(String bridgeTypeCode) {
        return jdbcTemplate.queryForList("""
                SELECT c.config_id, c.bridge_type_code, bt.bridge_type_name, c.item_code, i.item_name,
                       i.unit, i.item_category, c.requirement_type, c.trigger_condition, c.display_order
                FROM tb_bridge_type_initial_item_config c
                JOIN tb_bridge_type bt ON bt.bridge_type_code = c.bridge_type_code
                JOIN tb_initial_inspection_item_definition i ON i.item_code = c.item_code
                WHERE (? IS NULL OR c.bridge_type_code = ?)
                ORDER BY bt.bridge_type_code, c.display_order
                """, bridgeTypeCode, bridgeTypeCode);
    }

    public Map<String, Object> componentConfiguration(String componentCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT cfg.bridge_type_code, bt.bridge_type_name, cfg.part_code, p.part_name, cfg.display_order
                FROM tb_bridge_type_component_config cfg
                JOIN tb_bridge_type bt ON bt.bridge_type_code=cfg.bridge_type_code
                JOIN tb_part p ON p.part_code=cfg.part_code
                WHERE cfg.component_code=? AND cfg.active_flag=1
                ORDER BY bt.bridge_type_name
                """, componentCode);
        String partCode = rows.isEmpty() ? null : String.valueOf(rows.get(0).get("part_code"));
        Object order = rows.isEmpty() ? 1 : rows.get(0).get("display_order");
        return Map.of("partCode", partCode == null ? "" : partCode, "displayOrder", order,
                "bridgeTypeCodes", rows.stream().map(row -> row.get("bridge_type_code")).toList(), "rows", rows);
    }

    public Map<String, Object> initialItemConfiguration(String itemCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT cfg.bridge_type_code,bt.bridge_type_name,cfg.requirement_type,
                       cfg.trigger_condition,cfg.display_order
                FROM tb_bridge_type_initial_item_config cfg
                JOIN tb_bridge_type bt ON bt.bridge_type_code=cfg.bridge_type_code
                WHERE cfg.item_code=? ORDER BY bt.bridge_type_name
                """, itemCode);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("bridgeTypeCodes", rows.stream().map(row -> row.get("bridge_type_code")).toList());
        result.put("requirementType", rows.isEmpty() ? "required" : rows.get(0).get("requirement_type"));
        result.put("triggerCondition", rows.isEmpty() ? "" : rows.get(0).get("trigger_condition"));
        result.put("displayOrder", rows.isEmpty() ? 1 : rows.get(0).get("display_order"));
        result.put("rows", rows);
        return result;
    }

    @Transactional
    public Map<String, Object> saveInitialItemConfiguration(String itemCode, Map<String, Object> payload) {
        Object selected = payload.get("bridgeTypeCodes");
        List<String> bridgeTypes = selected instanceof List<?> list
                ? list.stream().map(String::valueOf).filter(value -> !value.isBlank()).distinct().toList() : List.of();
        if (bridgeTypes.isEmpty()) throw new BusinessException("请至少选择一种适用桥型");
        String requirement = String.valueOf(payload.getOrDefault("requirementType", "required"));
        if (!List.of("required", "conditional").contains(requirement)) throw new BusinessException("检查要求只能为必检或条件触发");
        String trigger = requirement.equals("conditional") ? String.valueOf(payload.getOrDefault("triggerCondition", "")).trim() : null;
        if (requirement.equals("conditional") && trigger.isBlank()) throw new BusinessException("条件触发项目必须填写触发条件");
        int displayOrder;
        try { displayOrder = Integer.parseInt(String.valueOf(payload.getOrDefault("displayOrder", "1"))); }
        catch (NumberFormatException ex) { throw new BusinessException("矩阵顺序必须为整数"); }
        jdbcTemplate.update("DELETE FROM tb_bridge_type_initial_item_config WHERE item_code=?", itemCode);
        for (String bridgeType : bridgeTypes) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_type_initial_item_config
                    (bridge_type_code,item_code,requirement_type,trigger_condition,display_order)
                    VALUES (?,?,?,?,?)
                    """, bridgeType, itemCode, requirement, trigger, displayOrder);
            jdbcTemplate.update("""
                    INSERT IGNORE INTO tb_initial_inspection_item
                    (initial_inspection_code,item_code,applicable_flag)
                    SELECT inspection.initial_inspection_code,?,1
                    FROM tb_initial_inspection inspection
                    JOIN tb_bridge bridge ON bridge.bridge_code=inspection.bridge_code
                    WHERE bridge.bridge_type_code=?
                    """, itemCode, bridgeType);
        }
        return initialItemConfiguration(itemCode);
    }

    @Transactional
    public Map<String, Object> saveComponentConfiguration(String componentCode, Map<String, Object> payload) {
        String partCode = String.valueOf(payload.getOrDefault("partCode", "")).trim();
        int displayOrder;
        try { displayOrder = Integer.parseInt(String.valueOf(payload.getOrDefault("displayOrder", "1"))); }
        catch (NumberFormatException ex) { throw new BusinessException("矩阵顺序必须为整数"); }
        Object selected = payload.get("bridgeTypeCodes");
        List<String> bridgeTypes = selected instanceof List<?> list
                ? list.stream().map(String::valueOf).filter(value -> !value.isBlank()).distinct().toList() : List.of();
        if (partCode.isBlank()) throw new BusinessException("请选择所属部位");
        if (bridgeTypes.isEmpty()) throw new BusinessException("请至少选择一种适用桥型");
        jdbcTemplate.update("UPDATE tb_bridge_type_component_config SET active_flag=0 WHERE component_code=?", componentCode);
        for (String bridgeType : bridgeTypes) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_type_component_config
                    (bridge_type_code,part_code,component_code,display_order,active_flag)
                    VALUES (?,?,?,?,1)
                    ON DUPLICATE KEY UPDATE display_order=VALUES(display_order),active_flag=1
                    """, bridgeType, partCode, componentCode, displayOrder);
        }
        return componentConfiguration(componentCode);
    }

    @Transactional
    public int generateBridgeComponents(String bridgeCode) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_bridge WHERE bridge_code = ?", Integer.class, bridgeCode);
        if (exists == null || exists == 0) throw new BusinessException("桥梁不存在");
        return jdbcTemplate.update("""
                INSERT IGNORE INTO tb_bridge_specific_component
                (bridge_code, config_id, part_code, component_code, component_serial, quantity, custom_flag)
                SELECT b.bridge_code, c.config_id, c.part_code, c.component_code,
                       CONCAT('STD-', LPAD(c.display_order, 2, '0')), 1, 0
                FROM tb_bridge b
                JOIN tb_bridge_type_component_config c ON c.bridge_type_code = b.bridge_type_code
                WHERE b.bridge_code = ? AND c.active_flag = 1
                """, bridgeCode);
    }

    @Transactional
    public int generateInitialItems(String inspectionCode) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_initial_inspection WHERE initial_inspection_code = ?", Integer.class, inspectionCode);
        if (exists == null || exists == 0) throw new BusinessException("初始检查记录不存在");
        return jdbcTemplate.update("""
                INSERT IGNORE INTO tb_initial_inspection_item
                (initial_inspection_code, item_code, applicable_flag)
                SELECT i.initial_inspection_code, c.item_code, 1
                FROM tb_initial_inspection i
                JOIN tb_bridge b ON b.bridge_code = i.bridge_code
                JOIN tb_bridge_type_initial_item_config c ON c.bridge_type_code = b.bridge_type_code
                WHERE i.initial_inspection_code = ?
                """, inspectionCode);
    }
}
