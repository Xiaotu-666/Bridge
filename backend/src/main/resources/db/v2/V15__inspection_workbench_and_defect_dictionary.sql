SET NAMES utf8mb4;

ALTER TABLE tb_initial_inspection
    ADD COLUMN task_id VARCHAR(30) NULL AFTER initial_inspection_code,
    ADD INDEX idx_initial_task_id (task_id),
    ADD UNIQUE KEY uk_initial_task_id (task_id),
    ADD CONSTRAINT fk_initial_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id);

ALTER TABLE tb_periodic_inspection
    ADD COLUMN task_id VARCHAR(30) NULL AFTER periodic_inspection_code,
    ADD INDEX idx_periodic_task_id (task_id),
    ADD UNIQUE KEY uk_periodic_task_id (task_id),
    ADD CONSTRAINT fk_periodic_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id);

CREATE TABLE tb_defect_definition (
    defect_definition_code VARCHAR(30) PRIMARY KEY,
    defect_name VARCHAR(100) NOT NULL,
    inspection_scope VARCHAR(20) NOT NULL DEFAULT 'both',
    defect_nature VARCHAR(50),
    default_degree_code VARCHAR(32),
    default_range VARCHAR(200),
    default_advice VARCHAR(500),
    applicable_part_code VARCHAR(32),
    description VARCHAR(500),
    display_order INT NOT NULL DEFAULT 1,
    active_flag TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_defect_definition_degree FOREIGN KEY (default_degree_code) REFERENCES tb_defect_degree(defect_degree_code),
    CONSTRAINT fk_defect_definition_part FOREIGN KEY (applicable_part_code) REFERENCES tb_part(part_code),
    CONSTRAINT uk_defect_definition_name_scope UNIQUE (defect_name, inspection_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='初始检查和定期检查共用病害字典';

ALTER TABLE tb_initial_inspection_item
    ADD COLUMN defect_definition_code VARCHAR(30) NULL AFTER inspection_description,
    ADD CONSTRAINT fk_initial_item_defect_definition FOREIGN KEY (defect_definition_code)
        REFERENCES tb_defect_definition(defect_definition_code);

ALTER TABLE tb_component_inspection
    ADD COLUMN defect_definition_code VARCHAR(30) NULL AFTER special_check_required,
    ADD CONSTRAINT fk_component_defect_definition FOREIGN KEY (defect_definition_code)
        REFERENCES tb_defect_definition(defect_definition_code);

ALTER TABLE tb_defect
    ADD COLUMN periodic_inspection_code VARCHAR(30) NULL AFTER initial_inspection_code,
    ADD COLUMN defect_definition_code VARCHAR(30) NULL AFTER defect_part_code,
    ADD INDEX idx_defect_periodic (periodic_inspection_code),
    ADD CONSTRAINT fk_defect_periodic FOREIGN KEY (periodic_inspection_code)
        REFERENCES tb_periodic_inspection(periodic_inspection_code),
    ADD CONSTRAINT fk_defect_definition FOREIGN KEY (defect_definition_code)
        REFERENCES tb_defect_definition(defect_definition_code);

INSERT IGNORE INTO tb_defect_definition
    (defect_definition_code, defect_name, inspection_scope, defect_nature, default_degree_code, default_range, default_advice, display_order)
VALUES
    ('D001', '裂缝', 'both', '结构性缺损', 'slight', '局部', '记录裂缝位置、宽度和发展情况，必要时进行专项监测', 1),
    ('D002', '剥落', 'both', '材料缺损', 'medium', '局部', '清除松动部分并检查钢筋及混凝土保护层', 2),
    ('D003', '露筋', 'both', '钢筋外露', 'medium', '局部', '做好防锈处理并修复混凝土保护层', 3),
    ('D004', '锈蚀', 'both', '钢构件锈蚀', 'medium', '局部', '除锈后复核截面损失并补涂防护层', 4),
    ('D005', '渗水泛碱', 'both', '水损害', 'slight', '局部', '查明渗水来源，完善防排水和止水措施', 5),
    ('D006', '支座异常', 'periodic', '支座病害', 'medium', '单个支座', '复核支座位移、脱空和剪切情况，安排维修', 6),
    ('D007', '伸缩缝损坏', 'periodic', '附属设施病害', 'medium', '局部', '清理缝内杂物并修复或更换伸缩装置', 7),
    ('D008', '桥面铺装破损', 'both', '桥面系病害', 'slight', '局部', '修补破损铺装并恢复排水功能', 8),
    ('D009', '护栏损坏', 'periodic', '安全设施病害', 'medium', '局部', '设置临时防护并及时修复护栏', 9),
    ('D010', '基础冲刷', 'periodic', '水下基础病害', 'serious', '局部', '复核冲刷深度和基础稳定性，必要时采取防护措施', 10);
