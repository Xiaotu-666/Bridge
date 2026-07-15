ALTER TABLE tb_bridge
    ADD COLUMN location_address VARCHAR(255) NULL COMMENT '桥梁详细地址' AFTER pile_number,
    ADD COLUMN longitude DECIMAL(10, 6) NULL COMMENT '高德坐标经度' AFTER location_address,
    ADD COLUMN latitude DECIMAL(10, 6) NULL COMMENT '高德坐标纬度' AFTER longitude;

UPDATE tb_bridge
SET location_address = CONCAT('重庆市桥梁示范点 ', bridge_name),
    longitude = 106.250000 + MOD(CRC32(bridge_code), 620000) / 1000000,
    latitude = 29.250000 + MOD(CRC32(CONCAT(bridge_code, '-lat')), 650000) / 1000000
WHERE longitude IS NULL OR latitude IS NULL;

UPDATE tb_bridge SET location_address='重庆市渝中区千厮门嘉陵江大桥附近', longitude=106.573480, latitude=29.570700 WHERE bridge_code='G75-001';
UPDATE tb_bridge SET location_address='重庆市江北区东水门长江大桥附近', longitude=106.590120, latitude=29.554520 WHERE bridge_code='G50-001';
UPDATE tb_bridge SET location_address='重庆市南岸区朝天门长江大桥附近', longitude=106.609870, latitude=29.575460 WHERE bridge_code='G65-001';

CREATE OR REPLACE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁` AS
SELECT
    bridge_code AS `桥梁编号`, route_code AS `路线编号`, bridge_type_code AS `桥梁类型编码`, bridge_name AS `桥梁名称`,
    administrative_code AS `行政区划代码`, pile_number AS `桥位桩号`, location_address AS `桥梁详细地址`,
    longitude AS `经度`, latitude AS `纬度`, road_management_org AS `公路管理机构名称`, function_type AS `功能类型`,
    crossed_road_name AS `被跨越道路或通道名称`, crossed_road_pile AS `被跨越道路或通道桩号`, maintenance_level AS `养护检查等级`,
    design_load AS `设计荷载`, bridge_slope AS `桥梁坡度`, curve_radius AS `桥梁平曲线半径`, built_year AS `建成年份`,
    design_unit AS `设计单位`, construction_unit AS `施工单位`, supervision_unit AS `监理单位`, owner_unit AS `业主单位`,
    management_unit AS `管养单位`, bridge_length AS `桥梁全长`, deck_width AS `桥面总宽`, lane_width AS `车道宽度`,
    sidewalk_width AS `人行道宽度`, barrier_height AS `护栏或防撞墙高度`, median_width AS `中央分隔带宽度`,
    standard_clearance AS `桥面标准净空`, actual_clearance AS `桥面实际净空`, navigation_standard AS `桥下通航等级及标准净空`,
    navigation_actual AS `桥下实际净空`, approach_width AS `引道总宽`, approach_alignment AS `引道线形或曲线半径`,
    design_flood AS `设计洪水频率及其水位`, historical_flood AS `历史洪水位`, seismic_coefficient AS `地震动峰值加速度系数`,
    span_combination AS `桥梁分联及跨径组合`, structural_system AS `结构体系`, archive_form AS `档案形式`, archive_date AS `建档日期`,
    notes AS `需要说明事项`, bridge_engineer AS `桥梁工程师`, card_filler AS `填卡人`, card_date AS `填卡日期`,
    status AS `状态`, create_by AS `创建人`, create_time AS `创建时间`, update_time AS `更新时间`
FROM tb_bridge;
