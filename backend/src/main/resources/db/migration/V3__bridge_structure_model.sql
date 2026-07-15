CREATE TABLE IF NOT EXISTS tb_bridge_type (
    type_code VARCHAR(30) NOT NULL PRIMARY KEY COMMENT 'Bridge type code',
    type_name VARCHAR(80) NOT NULL COMMENT 'Bridge type name',
    structure_group VARCHAR(30) NULL COMMENT 'Structure group',
    description VARCHAR(500) NULL COMMENT 'Description',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bridge_type_name (type_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bridge type';

CREATE TABLE IF NOT EXISTS tb_bridge_position (
    position_code VARCHAR(30) NOT NULL PRIMARY KEY COMMENT 'Position code',
    position_name VARCHAR(80) NOT NULL COMMENT 'Position name',
    description VARCHAR(500) NULL COMMENT 'Description',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bridge_position_name (position_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bridge position';

CREATE TABLE IF NOT EXISTS tb_bridge_component (
    component_code VARCHAR(30) NOT NULL PRIMARY KEY COMMENT 'Component code',
    component_name VARCHAR(80) NOT NULL COMMENT 'Component name',
    position_code VARCHAR(30) NOT NULL COMMENT 'Position code',
    description VARCHAR(500) NULL COMMENT 'Description',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_component_position FOREIGN KEY (position_code) REFERENCES tb_bridge_position(position_code),
    INDEX idx_component_position (position_code),
    UNIQUE KEY uk_component_position_name (position_code, component_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bridge component';

CREATE TABLE IF NOT EXISTS tb_bridge_type_component (
    config_id VARCHAR(40) NOT NULL PRIMARY KEY COMMENT 'Configuration id',
    type_code VARCHAR(30) NOT NULL COMMENT 'Bridge type code',
    component_code VARCHAR(30) NOT NULL COMMENT 'Component code',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    required_flag TINYINT NOT NULL DEFAULT 1 COMMENT 'Required flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_type_component_type FOREIGN KEY (type_code) REFERENCES tb_bridge_type(type_code),
    CONSTRAINT fk_type_component_component FOREIGN KEY (component_code) REFERENCES tb_bridge_component(component_code),
    UNIQUE KEY uk_type_component (type_code, component_code),
    INDEX idx_type_component_type (type_code), INDEX idx_type_component_component (component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bridge type component configuration';

CREATE TABLE IF NOT EXISTS tb_bridge_instance_component (
    instance_component_id VARCHAR(40) NOT NULL PRIMARY KEY COMMENT 'Instance component id',
    bridge_code VARCHAR(20) NOT NULL COMMENT 'Bridge code',
    component_code VARCHAR(30) NOT NULL COMMENT 'Component code',
    component_name VARCHAR(100) NOT NULL COMMENT 'Instance component name',
    location_desc VARCHAR(200) NULL COMMENT 'Location', material VARCHAR(50) NULL COMMENT 'Material',
    quantity INT NOT NULL DEFAULT 1 COMMENT 'Quantity', technical_status VARCHAR(30) NULL COMMENT 'Technical status',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_instance_component_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    CONSTRAINT fk_instance_component_component FOREIGN KEY (component_code) REFERENCES tb_bridge_component(component_code),
    INDEX idx_instance_component_bridge (bridge_code), INDEX idx_instance_component_code (component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bridge instance component';

CREATE TABLE IF NOT EXISTS tb_component_inspection_record (
    component_record_id VARCHAR(40) NOT NULL PRIMARY KEY COMMENT 'Inspection record id',
    task_id VARCHAR(20) NOT NULL COMMENT 'Task id', instance_component_id VARCHAR(40) NOT NULL COMMENT 'Instance component id',
    inspection_time DATETIME NOT NULL COMMENT 'Inspection time', inspector_id VARCHAR(20) NOT NULL COMMENT 'Inspector id',
    defect_type VARCHAR(30) NULL COMMENT 'Defect type', defect_degree VARCHAR(20) NULL COMMENT 'Defect degree',
    location_desc VARCHAR(200) NULL COMMENT 'Defect location', defect_range VARCHAR(100) NULL COMMENT 'Defect range',
    photo_path VARCHAR(500) NULL COMMENT 'Photo path', conclusion VARCHAR(500) NULL COMMENT 'Conclusion',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_component_record_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_component_record_instance FOREIGN KEY (instance_component_id) REFERENCES tb_bridge_instance_component(instance_component_id),
    CONSTRAINT fk_component_record_inspector FOREIGN KEY (inspector_id) REFERENCES tb_user(user_id),
    INDEX idx_component_record_task (task_id), INDEX idx_component_record_instance (instance_component_id), INDEX idx_component_record_time (inspection_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Component inspection record';

INSERT IGNORE INTO tb_bridge_type (type_code, type_name, structure_group, description) VALUES
('beam','梁式桥','梁桥','以梁作为主要承重结构'),
('arch','拱桥','拱桥','以拱圈或拱肋作为主要承重结构'),
('rigid_frame','刚构桥','刚构桥','梁与墩台固结形成刚构体系'),
('cable_stayed','斜拉桥','缆索承重桥','以斜拉索和主梁共同受力'),
('suspension','悬索桥','缆索承重桥','以主缆作为主要承重构件');

INSERT IGNORE INTO tb_bridge_position (position_code, position_name, description, sort_order) VALUES
('superstructure','上部结构','承受车辆荷载并传递至支承结构',1),
('substructure','下部结构','桥墩、桥台及基础等结构',2),
('deck_system','桥面系','桥面铺装、伸缩缝、排水等设施',3),
('accessory','附属设施','护栏、标志、照明及防护设施',4);

INSERT IGNORE INTO tb_bridge_component (component_code, component_name, position_code, description) VALUES
('main_girder','主梁','superstructure','主要受力梁体'),('bearing','支座','superstructure','连接上部结构与墩台'),
('pier','桥墩','substructure','中间支承结构'),('abutment','桥台','substructure','桥梁端部支承结构'),
('foundation','基础','substructure','将荷载传递至地基'),('deck_pavement','桥面铺装','deck_system','桥面行车铺装层'),
('expansion_joint','伸缩缝','deck_system','适应梁体变形'),('drainage','排水设施','deck_system','桥面排水系统'),
('guardrail','护栏','accessory','桥面防撞与防护设施');

INSERT IGNORE INTO tb_bridge_type_component (config_id,type_code,component_code,sort_order,required_flag) VALUES
('CFG-BEAM-001','beam','main_girder',1,1),('CFG-BEAM-002','beam','bearing',2,1),('CFG-BEAM-003','beam','pier',3,1),('CFG-BEAM-004','beam','abutment',4,1),('CFG-BEAM-005','beam','foundation',5,1),('CFG-BEAM-006','beam','deck_pavement',6,1),('CFG-BEAM-007','beam','expansion_joint',7,1),('CFG-BEAM-008','beam','drainage',8,1),('CFG-BEAM-009','beam','guardrail',9,1);

INSERT IGNORE INTO tb_bridge_instance_component (instance_component_id,bridge_code,component_code,component_name,location_desc,material,quantity,technical_status) VALUES
('BIC-G75-001-001','G75-001','main_girder','1#跨主梁','第1跨','预应力混凝土',5,'正常'),
('BIC-G75-001-002','G75-001','pier','1#桥墩','K12+400附近','钢筋混凝土',1,'正常'),
('BIC-G75-001-003','G75-001','deck_pavement','全桥桥面铺装','桥面全幅','沥青混凝土',1,'正常');
