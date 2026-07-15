INSERT INTO tb_bridge_specific_component
    (bridge_code, config_id, part_code, component_code, component_serial, location_desc,
     material_type, dimension_spec, quantity, custom_flag, status, remark)
SELECT b.bridge_code,
       cfg.config_id,
       cfg.part_code,
       cfg.component_code,
       CONCAT(seq.n, '#墩'),
       CONCAT('第', seq.n, '桥跨下部结构'),
       '钢筋混凝土',
       CONCAT('示范墩柱 ', 2.0 + MOD(CRC32(CONCAT(b.bridge_code, seq.n)), 25) / 10, 'm'),
       1,
       0,
       1,
       '按桥梁跨径和桥长生成的桥墩实例，可在桥梁具体部件中继续维护'
FROM tb_bridge b
JOIN tb_bridge_type_component_config cfg
  ON cfg.bridge_type_code = b.bridge_type_code AND cfg.component_code = 'C010'
JOIN (
    SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) seq ON seq.n <= LEAST(8, GREATEST(3, CEIL(COALESCE(b.bridge_length, 240) / 90)))
WHERE NOT EXISTS (
    SELECT 1 FROM tb_bridge_specific_component existing
    WHERE existing.bridge_code = b.bridge_code
      AND existing.component_code = 'C010'
      AND existing.component_serial = CONCAT(seq.n, '#墩')
);
