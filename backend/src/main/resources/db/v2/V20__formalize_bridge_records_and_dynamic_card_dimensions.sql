-- 正式化桥梁档案：清除已知演示业务数据，补齐规范卡片的动态列模型。
SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_demo_bridge_code (
    bridge_code VARCHAR(30) PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_demo_bridge_code (bridge_code) VALUES
('G42-001'),('G42-002'),('G42-003'),('G42-004'),('G42-005'),
('G50-001'),('G50-002'),('G50-003'),('G50-004'),('G50-005'),
('G60-001'),('G60-002'),('G60-003'),('G60-004'),('G60-005'),
('G65-001'),('G65-002'),('G65-003'),('G65-004'),('G65-005'),
('G75-001'),('G75-002'),('G75-003'),('G75-004'),('G75-005'),
('S10-001'),('S10-002'),('S10-003'),('S10-004'),('S10-005');

-- 先删除所有由演示桥派生的记录，避免保留孤立的报告、附件和审核结果。
DELETE report_row
FROM tb_report report_row
LEFT JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=report_row.initial_inspection_code
LEFT JOIN tb_periodic_inspection periodic_record ON periodic_record.periodic_inspection_code=report_row.periodic_inspection_code
LEFT JOIN tb_inspection_task task_record ON task_record.task_id=report_row.task_id
LEFT JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=COALESCE(initial_record.bridge_code,periodic_record.bridge_code,task_record.bridge_code)
WHERE demo_bridge.bridge_code IS NOT NULL;

DELETE attachment_row
FROM tb_attachment attachment_row
LEFT JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=attachment_row.initial_inspection_code
LEFT JOIN tb_component_inspection component_record ON component_record.component_inspection_id=attachment_row.component_inspection_id
LEFT JOIN tb_periodic_inspection periodic_record ON periodic_record.periodic_inspection_code=component_record.periodic_inspection_code
LEFT JOIN tb_defect defect_record ON defect_record.defect_id=attachment_row.defect_id
LEFT JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=COALESCE(attachment_row.bridge_code,initial_record.bridge_code,periodic_record.bridge_code,defect_record.bridge_code)
WHERE demo_bridge.bridge_code IS NOT NULL;

DELETE archive_row FROM tb_inspection_archive archive_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=archive_row.bridge_code;
DELETE defect_row FROM tb_defect defect_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=defect_row.bridge_code;
DELETE component_initial_row
FROM tb_initial_component_inspection component_initial_row
JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=component_initial_row.initial_inspection_code
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=initial_record.bridge_code;
DELETE component_periodic_row
FROM tb_component_inspection component_periodic_row
JOIN tb_periodic_inspection periodic_record ON periodic_record.periodic_inspection_code=component_periodic_row.periodic_inspection_code
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=periodic_record.bridge_code;
DELETE initial_item_row
FROM tb_initial_inspection_item initial_item_row
JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=initial_item_row.initial_inspection_code
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=initial_record.bridge_code;
DELETE assignment_row
FROM tb_task_assignment assignment_row
JOIN tb_inspection_task task_record ON task_record.task_id=assignment_row.task_id
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=task_record.bridge_code;
DELETE history_row
FROM tb_task_status_history history_row
JOIN tb_inspection_task task_record ON task_record.task_id=history_row.task_id
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=task_record.bridge_code;
DELETE periodic_record
FROM tb_periodic_inspection periodic_record
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=periodic_record.bridge_code;
DELETE initial_record
FROM tb_initial_inspection initial_record
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=initial_record.bridge_code;
DELETE task_record
FROM tb_inspection_task task_record
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=task_record.bridge_code;
DELETE evaluation_row
FROM tb_evaluation_history evaluation_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=evaluation_row.bridge_code;
DELETE archive_record_row
FROM tb_bridge_archive_record archive_record_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=archive_record_row.bridge_code;
DELETE component_row
FROM tb_bridge_specific_component component_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=component_row.bridge_code;
DELETE span_row
FROM tb_bridge_span_detail span_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=span_row.bridge_code;
DELETE structure_row
FROM tb_bridge_structure_detail structure_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=structure_row.bridge_code;
DELETE cable_row
FROM tb_bridge_cable_detail cable_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=cable_row.bridge_code;
DELETE bridge_row
FROM tb_bridge bridge_row
JOIN tmp_demo_bridge_code demo_bridge ON demo_bridge.bridge_code=bridge_row.bridge_code;

ALTER TABLE tb_bridge
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/verified/rejected' AFTER status,
    ADD COLUMN source_data_origin VARCHAR(100) NULL COMMENT '数据来源描述' AFTER verification_status,
    ADD COLUMN completion_date DATE NULL COMMENT '竣工时间' AFTER built_year;

UPDATE tb_bridge
SET verification_status='pending', source_data_origin='bridge_type_output.xlsx'
WHERE bridge_code LIKE 'IMP-%';

ALTER TABLE tb_bridge_structure_detail
    ADD COLUMN display_order INT NOT NULL DEFAULT 0 AFTER serial_no;

ALTER TABLE tb_bridge_cable_detail
    ADD COLUMN display_order INT NOT NULL DEFAULT 0 AFTER serial_no;

CREATE TABLE tb_bridge_measurement_point (
    measurement_point_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bridge_code VARCHAR(30) NOT NULL,
    point_category VARCHAR(40) NOT NULL DEFAULT 'deck_elevation' COMMENT '当前用于桥面高程，后续可扩展',
    point_no VARCHAR(40) NOT NULL,
    point_name VARCHAR(100) NULL,
    display_order INT NOT NULL DEFAULT 0,
    benchmark_elevation DECIMAL(12,3) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_measurement_point_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    UNIQUE KEY uk_measurement_point (bridge_code, point_category, point_no),
    INDEX idx_measurement_point_order (bridge_code, point_category, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁测点定义，作为A表34和初始检查测值的动态列来源';

CREATE TABLE tb_initial_measurement_value (
    measurement_value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    initial_inspection_code VARCHAR(30) NOT NULL,
    item_code VARCHAR(20) NOT NULL,
    measurement_point_id BIGINT NULL,
    bridge_component_id BIGINT NULL,
    display_order INT NOT NULL DEFAULT 0,
    measured_value VARCHAR(500) NULL,
    unit VARCHAR(30) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_initial_measurement_initial FOREIGN KEY (initial_inspection_code) REFERENCES tb_initial_inspection(initial_inspection_code),
    CONSTRAINT fk_initial_measurement_item FOREIGN KEY (item_code) REFERENCES tb_initial_inspection_item_definition(item_code),
    CONSTRAINT fk_initial_measurement_point FOREIGN KEY (measurement_point_id) REFERENCES tb_bridge_measurement_point(measurement_point_id),
    CONSTRAINT fk_initial_measurement_component FOREIGN KEY (bridge_component_id) REFERENCES tb_bridge_specific_component(bridge_component_id),
    INDEX idx_initial_measurement_record (initial_inspection_code, item_code, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='初始检查重复测值，支持测点、桥墩和索力等多列结果';

CREATE TABLE tb_bridge_management_change (
    change_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bridge_code VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    management_unit VARCHAR(120) NOT NULL,
    change_note VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_management_change_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    INDEX idx_management_change_bridge_time (bridge_code, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管养单位变更历史，对应A表H区';

ALTER TABLE tb_report
    ADD COLUMN bridge_code VARCHAR(30) NULL AFTER report_id,
    ADD INDEX idx_report_bridge_type (bridge_code, report_type, generation_time),
    ADD CONSTRAINT fk_report_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code);

UPDATE tb_report report_row
LEFT JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=report_row.initial_inspection_code
LEFT JOIN tb_periodic_inspection periodic_record ON periodic_record.periodic_inspection_code=report_row.periodic_inspection_code
LEFT JOIN tb_inspection_task task_record ON task_record.task_id=report_row.task_id
SET report_row.bridge_code=COALESCE(initial_record.bridge_code,periodic_record.bridge_code,task_record.bridge_code)
WHERE report_row.bridge_code IS NULL;

DROP TEMPORARY TABLE tmp_demo_bridge_code;
