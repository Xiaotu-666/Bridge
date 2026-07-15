-- 中文数据库展示层：保留英文物理标识以兼容后端，通过中文视图提供中文表名和中文属性名。
-- 版本：V9；应用程序继续访问 tb_* 物理表。
SET NAMES utf8mb4;

ALTER TABLE `tb_archive_item` COMMENT = '桥梁档案资料项（物理表：tb_archive_item）';
ALTER TABLE `tb_attachment` COMMENT = '照片档案文件（物理表：tb_attachment）';
ALTER TABLE `tb_backup_record` COMMENT = '数据库备份记录（物理表：tb_backup_record）';
ALTER TABLE `tb_bridge` COMMENT = '桥梁（物理表：tb_bridge）';
ALTER TABLE `tb_bridge_archive_record` COMMENT = '桥梁档案资料记录（物理表：tb_bridge_archive_record）';
ALTER TABLE `tb_bridge_specific_component` COMMENT = '桥梁具体部件（物理表：tb_bridge_specific_component）';
ALTER TABLE `tb_bridge_type` COMMENT = '桥梁类型（物理表：tb_bridge_type）';
ALTER TABLE `tb_bridge_type_component_config` COMMENT = '桥型部件配置（物理表：tb_bridge_type_component_config）';
ALTER TABLE `tb_bridge_type_initial_item_config` COMMENT = '桥型初检项目配置（物理表：tb_bridge_type_initial_item_config）';
ALTER TABLE `tb_check_category` COMMENT = '检测类别（物理表：tb_check_category）';
ALTER TABLE `tb_component` COMMENT = '部件（物理表：tb_component）';
ALTER TABLE `tb_component_inspection` COMMENT = '部件检查记录（物理表：tb_component_inspection）';
ALTER TABLE `tb_defect` COMMENT = '病害缺损（物理表：tb_defect）';
ALTER TABLE `tb_defect_degree` COMMENT = '缺损程度（物理表：tb_defect_degree）';
ALTER TABLE `tb_evaluation_history` COMMENT = '检测评定历史（物理表：tb_evaluation_history）';
ALTER TABLE `tb_initial_inspection` COMMENT = '初始检查记录（物理表：tb_initial_inspection）';
ALTER TABLE `tb_initial_inspection_item` COMMENT = '初始检查检测项目（物理表：tb_initial_inspection_item）';
ALTER TABLE `tb_initial_inspection_item_definition` COMMENT = '初始检查项目定义（物理表：tb_initial_inspection_item_definition）';
ALTER TABLE `tb_inspection_task` COMMENT = '检查任务（物理表：tb_inspection_task）';
ALTER TABLE `tb_operation_log` COMMENT = '操作日志（物理表：tb_operation_log）';
ALTER TABLE `tb_part` COMMENT = '部位（物理表：tb_part）';
ALTER TABLE `tb_periodic_inspection` COMMENT = '定期检查记录（物理表：tb_periodic_inspection）';
ALTER TABLE `tb_rating_level` COMMENT = '技术状况等级（物理表：tb_rating_level）';
ALTER TABLE `tb_report` COMMENT = '检查报告（物理表：tb_report）';
ALTER TABLE `tb_role` COMMENT = '角色（物理表：tb_role）';
ALTER TABLE `tb_route` COMMENT = '路线（物理表：tb_route）';
ALTER TABLE `tb_task_assignment` COMMENT = '任务分配（物理表：tb_task_assignment）';
ALTER TABLE `tb_task_status_history` COMMENT = '任务状态历史（物理表：tb_task_status_history）';
ALTER TABLE `tb_user` COMMENT = '用户（物理表：tb_user）';

DROP VIEW IF EXISTS `桥梁档案资料项`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁档案资料项` AS
SELECT
    `archive_item_code` AS `档案项编码`,
    `archive_item_name` AS `档案项名称`
FROM `tb_archive_item`;

DROP VIEW IF EXISTS `照片档案文件`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `照片档案文件` AS
SELECT
    `file_id` AS `文件编号`,
    `bridge_code` AS `桥梁编号`,
    `archive_record_id` AS `档案记录编号`,
    `initial_inspection_code` AS `初始检查编号`,
    `component_inspection_id` AS `部件检查记录编号`,
    `defect_id` AS `病害编号`,
    `file_name` AS `文件名称`,
    `stored_file_name` AS `存储文件名`,
    `storage_path` AS `存储路径`,
    `file_type` AS `文件类型`,
    `file_size` AS `文件大小`,
    `file_description` AS `文件说明`,
    `upload_by` AS `上传人`,
    `upload_time` AS `上传时间`
FROM `tb_attachment`;

DROP VIEW IF EXISTS `数据库备份记录`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `数据库备份记录` AS
SELECT
    `backup_id` AS `备份编号`,
    `file_name` AS `文件名称`,
    `file_path` AS `文件路径`,
    `file_size` AS `文件大小`,
    `sha256` AS `文件校验值`,
    `execute_by` AS `执行人编号`,
    `backup_status` AS `备份状态`,
    `restore_status` AS `恢复状态`,
    `error_message` AS `错误信息`,
    `create_time` AS `创建时间`,
    `restore_time` AS `恢复时间`
FROM `tb_backup_record`;

DROP VIEW IF EXISTS `桥梁`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁` AS
SELECT
    `bridge_code` AS `桥梁编号`,
    `route_code` AS `路线编号`,
    `bridge_type_code` AS `桥梁类型编码`,
    `bridge_name` AS `桥梁名称`,
    `administrative_code` AS `行政区划代码`,
    `pile_number` AS `桥位桩号`,
    `road_management_org` AS `公路管理机构名称`,
    `function_type` AS `功能类型`,
    `crossed_road_name` AS `被跨越道路或通道名称`,
    `crossed_road_pile` AS `被跨越道路或通道桩号`,
    `maintenance_level` AS `养护检查等级`,
    `design_load` AS `设计荷载`,
    `bridge_slope` AS `桥梁坡度`,
    `curve_radius` AS `桥梁平曲线半径`,
    `built_year` AS `建成年份`,
    `design_unit` AS `设计单位`,
    `construction_unit` AS `施工单位`,
    `supervision_unit` AS `监理单位`,
    `owner_unit` AS `业主单位`,
    `management_unit` AS `管养单位`,
    `bridge_length` AS `桥梁全长`,
    `deck_width` AS `桥面总宽`,
    `lane_width` AS `车道宽度`,
    `sidewalk_width` AS `人行道宽度`,
    `barrier_height` AS `护栏或防撞墙高度`,
    `median_width` AS `中央分隔带宽度`,
    `standard_clearance` AS `桥面标准净空`,
    `actual_clearance` AS `桥面实际净空`,
    `navigation_standard` AS `桥下通航等级及标准净空`,
    `navigation_actual` AS `桥下实际净空`,
    `approach_width` AS `引道总宽`,
    `approach_alignment` AS `引道线形或曲线半径`,
    `design_flood` AS `设计洪水频率及其水位`,
    `historical_flood` AS `历史洪水位`,
    `seismic_coefficient` AS `地震动峰值加速度系数`,
    `span_combination` AS `桥梁分联及跨径组合`,
    `structural_system` AS `结构体系`,
    `archive_form` AS `档案形式`,
    `archive_date` AS `建档日期`,
    `notes` AS `需要说明事项`,
    `bridge_engineer` AS `桥梁工程师`,
    `card_filler` AS `填卡人`,
    `card_date` AS `填卡日期`,
    `status` AS `状态`,
    `create_by` AS `创建人`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_bridge`;

DROP VIEW IF EXISTS `桥梁档案资料记录`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁档案资料记录` AS
SELECT
    `archive_record_id` AS `档案记录编号`,
    `bridge_code` AS `桥梁编号`,
    `archive_item_code` AS `档案项编码`,
    `completeness_status` AS `齐全状态`,
    `description` AS `说明`,
    `create_time` AS `创建时间`
FROM `tb_bridge_archive_record`;

DROP VIEW IF EXISTS `桥梁具体部件`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁具体部件` AS
SELECT
    `bridge_component_id` AS `桥梁部件编号`,
    `bridge_code` AS `桥梁编号`,
    `config_id` AS `配置编号`,
    `part_code` AS `部位编码`,
    `component_code` AS `部件编码`,
    `component_serial` AS `部件序号`,
    `location_desc` AS `所在位置`,
    `material_type` AS `材料类型`,
    `dimension_spec` AS `尺寸规格`,
    `quantity` AS `数量`,
    `force_value` AS `索力或内力`,
    `elevation_displacement` AS `高程或变位`,
    `custom_flag` AS `是否自定义`,
    `custom_reason` AS `自定义原因`,
    `status` AS `状态`,
    `remark` AS `备注`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_bridge_specific_component`;

DROP VIEW IF EXISTS `桥梁类型`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥梁类型` AS
SELECT
    `bridge_type_code` AS `桥梁类型编码`,
    `bridge_type_name` AS `桥梁类型名称`
FROM `tb_bridge_type`;

DROP VIEW IF EXISTS `桥型部件配置`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥型部件配置` AS
SELECT
    `config_id` AS `配置编号`,
    `bridge_type_code` AS `桥梁类型编码`,
    `part_code` AS `部位编码`,
    `component_code` AS `部件编码`,
    `display_order` AS `显示序号`,
    `active_flag` AS `是否启用`
FROM `tb_bridge_type_component_config`;

DROP VIEW IF EXISTS `桥型初检项目配置`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `桥型初检项目配置` AS
SELECT
    `config_id` AS `配置编号`,
    `bridge_type_code` AS `桥梁类型编码`,
    `item_code` AS `检测项目编码`,
    `requirement_type` AS `检查要求`,
    `trigger_condition` AS `触发条件`,
    `display_order` AS `显示序号`
FROM `tb_bridge_type_initial_item_config`;

DROP VIEW IF EXISTS `检测类别`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `检测类别` AS
SELECT
    `check_category_code` AS `检测类别编码`,
    `check_category_name` AS `检测类别名称`
FROM `tb_check_category`;

DROP VIEW IF EXISTS `部件`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `部件` AS
SELECT
    `component_code` AS `部件编码`,
    `component_name` AS `部件名称`,
    `description` AS `说明`,
    `active_flag` AS `是否启用`
FROM `tb_component`;

DROP VIEW IF EXISTS `部件检查记录`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `部件检查记录` AS
SELECT
    `component_inspection_id` AS `部件检查记录编号`,
    `periodic_inspection_code` AS `定期检查编号`,
    `bridge_component_id` AS `桥梁部件编号`,
    `part_code` AS `部位编码`,
    `component_code` AS `部件编码`,
    `defect_type` AS `缺损类型`,
    `defect_location` AS `缺损位置`,
    `defect_range` AS `缺损范围`,
    `defect_degree_code` AS `缺损程度编码`,
    `worst_component` AS `最不利构件`,
    `score` AS `评分`,
    `maintenance_advice` AS `养护建议`,
    `special_check_required` AS `是否需要特殊检查`,
    `create_time` AS `创建时间`
FROM `tb_component_inspection`;

DROP VIEW IF EXISTS `病害缺损`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `病害缺损` AS
SELECT
    `defect_id` AS `病害编号`,
    `bridge_code` AS `桥梁编号`,
    `initial_inspection_code` AS `初始检查编号`,
    `component_inspection_id` AS `部件检查记录编号`,
    `defect_part_code` AS `缺损部位编码`,
    `defect_type` AS `缺损类型`,
    `defect_nature` AS `缺损性质`,
    `defect_range` AS `缺损范围`,
    `defect_quantity` AS `缺损数量`,
    `defect_degree_code` AS `缺损程度编码`,
    `description` AS `说明`,
    `create_time` AS `创建时间`
FROM `tb_defect`;

DROP VIEW IF EXISTS `缺损程度`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `缺损程度` AS
SELECT
    `defect_degree_code` AS `缺损程度编码`,
    `defect_degree_name` AS `缺损程度名称`
FROM `tb_defect_degree`;

DROP VIEW IF EXISTS `检测评定历史`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `检测评定历史` AS
SELECT
    `evaluation_id` AS `评定记录编号`,
    `bridge_code` AS `桥梁编号`,
    `evaluation_date` AS `评定日期`,
    `check_category_code` AS `检测类别编码`,
    `rating_result` AS `技术状况评定结果`,
    `special_conclusion` AS `特殊检查结论`,
    `treatment_strategy` AS `处治对策`,
    `next_check_date` AS `下次检测日期`,
    `create_time` AS `创建时间`
FROM `tb_evaluation_history`;

DROP VIEW IF EXISTS `初始检查记录`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `初始检查记录` AS
SELECT
    `initial_inspection_code` AS `初始检查编号`,
    `bridge_code` AS `桥梁编号`,
    `inspection_date` AS `检查日期`,
    `inspection_org` AS `检查机构`,
    `inspectors` AS `检查人员`,
    `bridge_engineer` AS `桥梁工程师`,
    `weather_temperature` AS `检查时气候及环境温度`,
    `main_span_structure` AS `主跨结构`,
    `maximum_span` AS `最大跨径`,
    `span_combination` AS `桥梁分联及跨径组合`,
    `structure_form` AS `上下部结构形式`,
    `construction_method` AS `施工方法`,
    `construction_rework` AS `施工返工维修或加固情况`,
    `reinforcement_info` AS `加固改造情况`,
    `missing_archive_drawings` AS `档案不全时维修加固图情况`,
    `defect_advice` AS `病害描述及养护建议`,
    `status` AS `状态`,
    `create_by` AS `创建人`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_initial_inspection`;

DROP VIEW IF EXISTS `初始检查检测项目`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `初始检查检测项目` AS
SELECT
    `item_record_id` AS `检测项目记录编号`,
    `initial_inspection_code` AS `初始检查编号`,
    `item_code` AS `检测项目编码`,
    `applicable_flag` AS `是否适用`,
    `trigger_result` AS `触发判断结果`,
    `measured_value` AS `检测结果`,
    `inspection_description` AS `检测说明`
FROM `tb_initial_inspection_item`;

DROP VIEW IF EXISTS `初始检查项目定义`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `初始检查项目定义` AS
SELECT
    `item_code` AS `检测项目编码`,
    `item_name` AS `检测项目名称`,
    `unit` AS `计量单位`,
    `item_category` AS `项目类别`,
    `description` AS `说明`
FROM `tb_initial_inspection_item_definition`;

DROP VIEW IF EXISTS `检查任务`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `检查任务` AS
SELECT
    `task_id` AS `任务编号`,
    `bridge_code` AS `桥梁编号`,
    `inspection_type` AS `检查类型`,
    `inspection_level` AS `检查等级`,
    `plan_start_date` AS `计划开始日期`,
    `plan_end_date` AS `计划结束日期`,
    `actual_start_date` AS `实际开始日期`,
    `actual_end_date` AS `实际结束日期`,
    `task_status` AS `任务状态`,
    `remarks` AS `任务备注`,
    `cancel_reason` AS `取消原因`,
    `creator_id` AS `创建人编号`,
    `reviewer_id` AS `审核人编号`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_inspection_task`;

DROP VIEW IF EXISTS `操作日志`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `操作日志` AS
SELECT
    `log_id` AS `日志编号`,
    `user_id` AS `用户编号`,
    `user_name` AS `用户姓名`,
    `ip_address` AS `IP地址`,
    `module` AS `功能模块`,
    `operation_type` AS `操作类型`,
    `target_table` AS `目标表名`,
    `target_id` AS `目标记录编号`,
    `description` AS `说明`,
    `operation_result` AS `操作结果`,
    `operate_time` AS `操作时间`
FROM `tb_operation_log`;

DROP VIEW IF EXISTS `部位`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `部位` AS
SELECT
    `part_code` AS `部位编码`,
    `part_name` AS `部位名称`,
    `sort_order` AS `排序序号`
FROM `tb_part`;

DROP VIEW IF EXISTS `定期检查记录`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `定期检查记录` AS
SELECT
    `periodic_inspection_code` AS `定期检查编号`,
    `bridge_code` AS `桥梁编号`,
    `inspection_date` AS `检查日期`,
    `last_inspection_date` AS `上次检查日期`,
    `last_maintenance_date` AS `上次修复日期`,
    `weather_temperature` AS `检查时气候及环境温度`,
    `rating_level_code` AS `技术状况等级编码`,
    `cleanliness` AS `全桥清洁状况`,
    `maintenance_status` AS `预防及修复状况`,
    `next_inspection_date` AS `下次检查日期`,
    `recorder` AS `记录人`,
    `principal` AS `负责人`,
    `status` AS `状态`,
    `create_by` AS `创建人`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_periodic_inspection`;

DROP VIEW IF EXISTS `技术状况等级`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `技术状况等级` AS
SELECT
    `rating_level_code` AS `技术状况等级编码`,
    `rating_level_name` AS `技术状况等级名称`
FROM `tb_rating_level`;

DROP VIEW IF EXISTS `检查报告`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `检查报告` AS
SELECT
    `report_id` AS `报告编号`,
    `task_id` AS `任务编号`,
    `initial_inspection_code` AS `初始检查编号`,
    `periodic_inspection_code` AS `定期检查编号`,
    `report_type` AS `报告类型`,
    `version_no` AS `版本号`,
    `file_format` AS `文件格式`,
    `file_path` AS `文件路径`,
    `report_status` AS `报告状态`,
    `generation_time` AS `生成时间`,
    `generator_id` AS `生成人编号`,
    `reviewer_id` AS `审核人编号`,
    `change_summary` AS `变更摘要`
FROM `tb_report`;

DROP VIEW IF EXISTS `角色`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `角色` AS
SELECT
    `role_id` AS `角色编号`,
    `role_code` AS `角色编码`,
    `role_name` AS `角色名称`,
    `role_desc` AS `角色说明`,
    `permission_set` AS `权限集合`,
    `create_time` AS `创建时间`
FROM `tb_role`;

DROP VIEW IF EXISTS `路线`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `路线` AS
SELECT
    `route_code` AS `路线编号`,
    `route_name` AS `路线名称`,
    `route_grade` AS `路线等级`,
    `create_time` AS `创建时间`,
    `update_time` AS `更新时间`
FROM `tb_route`;

DROP VIEW IF EXISTS `任务分配`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `任务分配` AS
SELECT
    `assignment_id` AS `分配编号`,
    `task_id` AS `任务编号`,
    `user_id` AS `用户编号`,
    `assignment_status` AS `分配状态`,
    `assign_time` AS `分配时间`,
    `accept_time` AS `接受时间`,
    `complete_time` AS `完成时间`
FROM `tb_task_assignment`;

DROP VIEW IF EXISTS `任务状态历史`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `任务状态历史` AS
SELECT
    `history_id` AS `历史记录编号`,
    `task_id` AS `任务编号`,
    `from_status` AS `原状态`,
    `to_status` AS `目标状态`,
    `operator_id` AS `操作人编号`,
    `operation_time` AS `状态变更时间`,
    `opinion` AS `审核意见`
FROM `tb_task_status_history`;

DROP VIEW IF EXISTS `用户`;
CREATE ALGORITHM = MERGE SQL SECURITY INVOKER VIEW `用户` AS
SELECT
    `user_id` AS `用户编号`,
    `user_name` AS `用户姓名`,
    `login_account` AS `登录账号`,
    `password` AS `密码密文`,
    `role_id` AS `角色编号`,
    `department` AS `单位部门`,
    `phone` AS `联系电话`,
    `email` AS `电子邮箱`,
    `user_status` AS `用户状态`,
    `force_pwd_change` AS `是否强制修改密码`,
    `create_time` AS `创建时间`,
    `last_login_time` AS `最后登录时间`
FROM `tb_user`;

