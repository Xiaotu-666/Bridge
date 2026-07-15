INSERT INTO tb_component_inspection
    (periodic_inspection_code, bridge_component_id, part_code, component_code,
     defect_type, defect_location, defect_range, defect_degree_code,
     worst_component, score, maintenance_advice, special_check_required)
SELECT periodic.periodic_inspection_code,
       component.bridge_component_id,
       component.part_code,
       component.component_code,
       CASE MOD(CRC32(CONCAT(periodic.periodic_inspection_code, component.component_serial)), 5)
           WHEN 0 THEN '墩身局部裂缝'
           WHEN 1 THEN '基础轻微冲刷'
           WHEN 2 THEN '表面蜂窝麻面'
           ELSE '未见明显缺损'
       END,
       component.location_desc,
       '局部',
       CASE MOD(CRC32(CONCAT(periodic.periodic_inspection_code, component.component_serial)), 5)
           WHEN 0 THEN 'medium'
           WHEN 1 THEN 'slight'
           WHEN 2 THEN 'slight'
           ELSE NULL
       END,
       IF(MOD(component.bridge_component_id, 7) = 0, component.component_serial, NULL),
       75 + MOD(CRC32(CONCAT(periodic.periodic_inspection_code, component.component_serial)), 26),
       '纳入桥墩专项跟踪，复核裂缝、冲刷及墩台变位情况',
       IF(MOD(component.bridge_component_id, 13) = 0, 1, 0)
FROM tb_periodic_inspection periodic
JOIN tb_bridge_specific_component component
  ON component.bridge_code = periodic.bridge_code
 AND component.component_code = 'C010'
 AND component.component_serial LIKE '%#%'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_component_inspection existing
    WHERE existing.periodic_inspection_code = periodic.periodic_inspection_code
      AND existing.bridge_component_id = component.bridge_component_id
);
