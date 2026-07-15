-- 桥梁检查系统综合升级：坐标、路线、版本化检查、结构明细和照片分类
SET NAMES utf8mb4;

ALTER TABLE tb_bridge
    ADD COLUMN coordinate_source VARCHAR(20) NOT NULL DEFAULT 'WGS84' COMMENT '坐标来源：WGS84/GCJ02' AFTER latitude,
    ADD COLUMN raw_longitude DECIMAL(10, 6) NULL COMMENT '原始经度' AFTER coordinate_source,
    ADD COLUMN raw_latitude DECIMAL(10, 6) NULL COMMENT '原始纬度' AFTER raw_longitude;

UPDATE tb_bridge
SET raw_longitude = longitude, raw_latitude = latitude
WHERE raw_longitude IS NULL AND longitude IS NOT NULL AND latitude IS NOT NULL;

ALTER TABLE tb_route
    ADD COLUMN route_geometry LONGTEXT NULL COMMENT '高德 GCJ-02 路线坐标串' AFTER route_grade,
    ADD COLUMN route_distance DECIMAL(12, 2) NULL COMMENT '规划路线长度（米）' AFTER route_geometry,
    ADD COLUMN origin_longitude DECIMAL(10, 6) NULL AFTER route_distance,
    ADD COLUMN origin_latitude DECIMAL(10, 6) NULL AFTER origin_longitude,
    ADD COLUMN destination_longitude DECIMAL(10, 6) NULL AFTER origin_latitude,
    ADD COLUMN destination_latitude DECIMAL(10, 6) NULL AFTER destination_longitude,
    ADD COLUMN waypoints_json LONGTEXT NULL COMMENT '途经点 JSON' AFTER destination_latitude;

ALTER TABLE tb_part
    ADD UNIQUE KEY uk_part_sort_order (sort_order);

ALTER TABLE tb_initial_inspection
    DROP INDEX uk_initial_inspection_bridge,
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 AFTER initial_inspection_code,
    ADD COLUMN effective_flag TINYINT NOT NULL DEFAULT 1 AFTER version_no,
    ADD COLUMN record_form_no VARCHAR(60) NULL COMMENT '初始检查表专属编号' AFTER effective_flag,
    ADD UNIQUE KEY uk_initial_record_form_no (record_form_no),
    ADD INDEX idx_initial_bridge_effective (bridge_code, effective_flag, inspection_date);

ALTER TABLE tb_periodic_inspection
    ADD COLUMN form_table_code VARCHAR(10) NOT NULL DEFAULT 'C-7' COMMENT 'C-1至C-7模板编号' AFTER periodic_inspection_code,
    ADD COLUMN record_form_no VARCHAR(60) NULL COMMENT '定期检查表专属编号' AFTER form_table_code,
    ADD UNIQUE KEY uk_periodic_record_form_no (record_form_no);

ALTER TABLE tb_attachment
    ADD COLUMN photo_category VARCHAR(40) NULL COMMENT '桥梁照片分类' AFTER file_description,
    ADD INDEX idx_attachment_bridge_category (bridge_code, photo_category);

CREATE TABLE tb_bridge_span_detail (
    span_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bridge_code VARCHAR(30) NOT NULL,
    span_no INT NOT NULL,
    span_length DECIMAL(10, 2) NULL,
    structure_form VARCHAR(100) NULL,
    material_type VARCHAR(100) NULL,
    location_desc VARCHAR(200) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_span_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    UNIQUE KEY uk_span_no (bridge_code, span_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁分孔结构明细';

CREATE TABLE tb_bridge_structure_detail (
    structure_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bridge_code VARCHAR(30) NOT NULL,
    structure_group VARCHAR(30) NOT NULL COMMENT '结构体系/上部/桥面系/下部/基础/支座/附属设施',
    structure_type VARCHAR(40) NOT NULL,
    serial_no VARCHAR(40) NOT NULL,
    form VARCHAR(200) NULL,
    material_type VARCHAR(100) NULL,
    quantity DECIMAL(10, 2) NULL,
    force_value DECIMAL(12, 3) NULL,
    location_desc VARCHAR(200) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_structure_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    UNIQUE KEY uk_structure_serial (bridge_code, structure_group, structure_type, serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁结构分组明细';

CREATE TABLE tb_bridge_cable_detail (
    cable_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bridge_code VARCHAR(30) NOT NULL,
    cable_type VARCHAR(20) NOT NULL COMMENT '斜拉索/吊杆/系杆',
    serial_no VARCHAR(40) NOT NULL,
    force_value DECIMAL(12, 3) NULL,
    material_type VARCHAR(100) NULL,
    location_desc VARCHAR(200) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cable_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    UNIQUE KEY uk_cable_serial (bridge_code, cable_type, serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁缆索结构明细';

CREATE TABLE tb_bridge_photo_category (
    category_code VARCHAR(40) PRIMARY KEY,
    category_name VARCHAR(80) NOT NULL UNIQUE,
    display_order INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁照片分类字典';

INSERT IGNORE INTO tb_bridge_photo_category(category_code, category_name, display_order) VALUES
('overall','桥梁总体照片',1), ('front','桥梁正面照片',2),
('left_elevation','桥梁左侧立面照片',3), ('right_elevation','桥梁右侧立面照片',4),
('deck','桥面照片',5), ('pier_foundation','桥墩及基础照片',6), ('other','其他桥梁照片',7);
