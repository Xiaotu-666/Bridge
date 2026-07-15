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
                "structures", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_structure_detail WHERE bridge_code=? ORDER BY structure_group,structure_type,serial_no", bridgeCode),
                "cables", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_cable_detail WHERE bridge_code=? ORDER BY cable_type,serial_no", bridgeCode)
        );
    }

    @Transactional
    public Map<String, Object> replace(String bridgeCode, Map<String, Object> payload) {
        ensureBridge(bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_span_detail WHERE bridge_code=?", bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_structure_detail WHERE bridge_code=?", bridgeCode);
        jdbcTemplate.update("DELETE FROM tb_bridge_cable_detail WHERE bridge_code=?", bridgeCode);
        for (Map<String, Object> row : rows(payload.get("spans"))) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_span_detail
                    (bridge_code,span_no,span_length,structure_form,material_type,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?)
                    """, bridgeCode, row.get("span_no"), empty(row.get("span_length")), row.get("structure_form"),
                    row.get("material_type"), row.get("location_desc"), row.get("remark"));
        }
        for (Map<String, Object> row : rows(payload.get("structures"))) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_structure_detail
                    (bridge_code,structure_group,structure_type,serial_no,form,material_type,quantity,force_value,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, bridgeCode, required(row, "structure_group", "结构分组"), required(row, "structure_type", "结构类型"),
                    required(row, "serial_no", "结构序号"), row.get("form"), row.get("material_type"), empty(row.get("quantity")),
                    empty(row.get("force_value")), row.get("location_desc"), row.get("remark"));
        }
        for (Map<String, Object> row : rows(payload.get("cables"))) {
            jdbcTemplate.update("""
                    INSERT INTO tb_bridge_cable_detail
                    (bridge_code,cable_type,serial_no,force_value,material_type,location_desc,remark)
                    VALUES (?,?,?,?,?,?,?)
                    """, bridgeCode, required(row, "cable_type", "索类型"), required(row, "serial_no", "索编号"),
                    empty(row.get("force_value")), row.get("material_type"), row.get("location_desc"), row.get("remark"));
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

    private void ensureBridge(String bridgeCode) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_bridge WHERE bridge_code=?", Integer.class, bridgeCode);
        if (count == null || count == 0) throw new BusinessException("桥梁不存在");
    }
}
