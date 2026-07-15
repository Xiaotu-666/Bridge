ALTER TABLE tb_initial_inspection
    ADD UNIQUE KEY uk_initial_inspection_bridge (bridge_code);

CREATE TABLE tb_initial_component_inspection (
    initial_component_inspection_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    initial_inspection_code VARCHAR(30) NOT NULL,
    bridge_component_id BIGINT NOT NULL,
    item_code VARCHAR(20) NOT NULL,
    measured_value TEXT,
    inspection_description TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ici_initial FOREIGN KEY (initial_inspection_code)
        REFERENCES tb_initial_inspection(initial_inspection_code),
    CONSTRAINT fk_ici_bridge_component FOREIGN KEY (bridge_component_id)
        REFERENCES tb_bridge_specific_component(bridge_component_id),
    CONSTRAINT fk_ici_item FOREIGN KEY (item_code)
        REFERENCES tb_initial_inspection_item_definition(item_code),
    UNIQUE KEY uk_ici_component_item (initial_inspection_code, bridge_component_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='初始检查桥墩及具体部件检测记录';

INSERT INTO tb_initial_component_inspection
    (initial_inspection_code, bridge_component_id, item_code, measured_value, inspection_description)
SELECT i.initial_inspection_code, c.bridge_component_id, item.item_code,
       CASE item.item_code
           WHEN 'I02' THEN CONCAT(185 + MOD(CRC32(CONCAT(i.initial_inspection_code,c.component_serial,'I02')), 900) / 100, ' m')
           WHEN 'I03' THEN CONCAT(MOD(CRC32(CONCAT(i.initial_inspection_code,c.component_serial,'I03')), 18) / 100, '°')
           ELSE '基础外观完整，未见明显冲刷和异常位移'
       END,
       CONCAT(c.component_serial, '独立检测记录')
FROM tb_initial_inspection i
JOIN tb_bridge_specific_component c
  ON c.bridge_code = i.bridge_code
 AND c.component_code = 'C010'
 AND c.component_serial LIKE '%#%'
CROSS JOIN (SELECT 'I02' item_code UNION ALL SELECT 'I03' UNION ALL SELECT 'I43') item;

CREATE OR REPLACE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `初始检查桥墩检测记录` AS
SELECT initial_component_inspection_id AS `桥墩检测记录编号`,
       initial_inspection_code AS `初始检查编号`,
       bridge_component_id AS `桥梁具体部件编号`,
       item_code AS `检测项目编码`,
       measured_value AS `检测结果`,
       inspection_description AS `检测说明`,
       create_time AS `创建时间`
FROM tb_initial_component_inspection;
