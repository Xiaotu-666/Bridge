UPDATE tb_bridge SET
road_management_org='省公路管理局',
function_type='公路桥',
crossed_road_name='地方道路',
design_load='公路-I级',
design_unit='交通规划设计院',
construction_unit='路桥建设集团',
supervision_unit='交通工程监理公司',
owner_unit='省交通投资集团',
management_unit='桥梁管理中心',
navigation_standard='三级航道/7m',
approach_alignment='直线引道',
design_flood='1/100，设计水位185.30m',
historical_flood='1981年洪水位184.70m',
archive_form='电子档案',
notes='用于系统功能演示和界面测试',
bridge_engineer='张工程师',card_filler='李检查员'
WHERE bridge_code<>'G75-001';

UPDATE tb_bridge SET maintenance_level=CASE MOD(CRC32(bridge_code),3) WHEN 0 THEN 'Ⅰ' WHEN 1 THEN 'Ⅱ' ELSE 'Ⅲ' END;

UPDATE tb_bridge SET span_combination=CONCAT('3×',ROUND(bridge_length/3,0),'m') WHERE span_combination LIKE '%?%';
