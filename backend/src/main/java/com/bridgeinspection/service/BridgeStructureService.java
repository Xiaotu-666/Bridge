package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BridgeStructureService {
    private final JdbcTemplate jdbcTemplate;

    public BridgeStructureService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Map<String, Object> get(String bridgeCode) {
        ensureBridge(bridgeCode);
        return Map.of(
                "spans", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_span_detail WHERE bridge_code=? ORDER BY span_no", bridgeCode),
                "structures", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_structure_detail WHERE bridge_code=? ORDER BY structure_group,display_order,structure_type,serial_no", bridgeCode),
                "cables", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_cable_detail WHERE bridge_code=? ORDER BY cable_type,display_order,serial_no", bridgeCode),
                "measurementPoints", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_measurement_point WHERE bridge_code=? ORDER BY point_category,display_order,point_no", bridgeCode)
        );
    }

    @Transactional
    public Map<String, Object> replace(String bridgeCode, Map<String, Object> payload) {
        ensureBridge(bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_span_detail WHERE bridge_code=?", bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_structure_detail WHERE bridge_code=?", bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_cable_detail WHERE bridge_code=?", bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_measurement_point WHERE bridge_code=?", bridgeCode);
        for (Map<String, Object> row : rows(payload.get("spans"))) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_span_detail
                    (bridge_code,span_no,span_length,structure_form,material_type,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?)
                    """, bridgeCode, row.get("span_no"), empty(row.get("span_length")), row.get("structure_form"),
                    row.get("material_type"), row.get("location_desc"), row.get("remark"));
        }
        List<Map<String, Object>> structures = rows(payload.get("structures"));
        for (int index = 0; index < structures.size(); index++) {
            Map<String, Object> row = structures.get(index);
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_structure_detail
                    (bridge_code,structure_group,structure_type,serial_no,display_order,form,material_type,quantity,force_value,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, bridgeCode, required(row, "structure_group", "结构分组"), required(row, "structure_type", "结构类型"),
                    required(row, "serial_no", "结构序号"), order(row, index + 1), row.get("form"), row.get("material_type"), empty(row.get("quantity")),
                    empty(row.get("force_value")), row.get("location_desc"), row.get("remark"));
        }
        List<Map<String, Object>> cables = rows(payload.get("cables"));
        for (int index = 0; index < cables.size(); index++) {
            Map<String, Object> row = cables.get(index);
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_cable_detail
                    (bridge_code,cable_type,serial_no,display_order,force_value,material_type,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, bridgeCode, required(row, "cable_type", "索类型"), required(row, "serial_no", "索编号"),
                    order(row, index + 1), empty(row.get("force_value")), row.get("material_type"), row.get("location_desc"), row.get("remark"));
        }
        List<Map<String, Object>> measurementPoints = rows(payload.get("measurementPoints"));
        for (int index = 0; index < measurementPoints.size(); index++) {
            Map<String, Object> row = measurementPoints.get(index);
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_measurement_point
                    (bridge_code,point_category,point_no,point_name,display_order,benchmark_elevation,remark)
                    VALUES (?,?,?,?,?,?,?)
                    """, bridgeCode, valueOrDefault(row.get("point_category"), "deck_elevation"),
                    required(row, "point_no", "测点编号"), row.get("point_name"), order(row, index + 1),
                    empty(row.get("benchmark_elevation")), row.get("remark"));
        }
        return get(bridgeCode);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }

    private Object required(Map<String, Object> row, String key, String label) {
        Object value = row.get(key);
        if (value == null || String.valueOf(value).isBlank()) throw new BusinessException(label + "不能为空");
        return value;
    }

    private Object empty(Object value) { return value == null || String.valueOf(value).isBlank() ? null : value; }

    private int order(Map<String, Object> row, int fallback) {
        Object value = row.get("display_order");
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String valueOrDefault(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private void ensureBridge(String bridgeCode) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_bridge WHERE bridge_code=?", Integer.class, bridgeCode);
        if (count == null || count == 0) throw new BusinessException("桥梁不存在");
    }
}
