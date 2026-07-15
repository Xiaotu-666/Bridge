INSERT INTO tb_role (role_id, role_code, role_name, role_desc, permission_set) VALUES
(1,'admin','系统管理员','系统配置与全部业务权限',JSON_ARRAY('*')),
(2,'engineer','桥梁工程师','桥梁建档、任务编制与报告生成',JSON_ARRAY('bridge-view','bridge-create','bridge-edit','matrix-view','task-view','task-create','task-edit','task-review','report-view','report-create','report-export')),
(3,'inspector','检查人员','执行初始检查与定期检查任务',JSON_ARRAY('task-view','task-accept','initial-view','initial-edit','periodic-view','periodic-edit','defect-view','defect-edit','attachment-upload')),
(4,'reviewer','审核人员','检查成果审核与归档',JSON_ARRAY('task-view','task-review','initial-view','initial-review','periodic-view','periodic-review','report-view','report-review')),
(5,'viewer','查询人员','只读查询与统计分析',JSON_ARRAY('bridge-view','initial-view','periodic-view','defect-view','report-view','statistics-view'));

INSERT INTO tb_user (user_id,user_name,login_account,password,role_id,department,user_status,force_pwd_change) VALUES
(1,'系统管理员','admin','__INIT_ADMIN123__',1,'信息中心',1,0),
(2,'张工程师','zhang','__INIT_ADMIN123__',2,'桥梁科',1,0),
(3,'李检查员','li','__INIT_ADMIN123__',3,'检测中心',1,0),
(4,'王审核员','wang','__INIT_ADMIN123__',4,'技术科',1,0),
(5,'赵查询员','zhao','__INIT_ADMIN123__',5,'管理处',1,0);

INSERT INTO tb_route (route_code,route_name,route_grade) VALUES ('G75','兰海高速','高速公路');
INSERT INTO tb_bridge_type (bridge_type_code,bridge_type_name) VALUES
('beam','梁式桥'),('arch','板拱/肋拱/箱形拱/双曲拱桥'),('rigid_arch','刚架拱/桁架拱桥'),
('composite_arch','钢-混凝土组合拱桥'),('cable_stayed','斜拉桥'),('suspension','悬索桥');
INSERT INTO tb_part (part_code,part_name,sort_order) VALUES
('deck','桥面系',1),('upper','上部结构',2),('lower','下部结构',3),('accessory','附属设施',4);

INSERT INTO tb_archive_item VALUES
('design_drawing','设计图纸'),('design_file','设计文件'),('asbuilt_drawing','竣工图纸'),
('construction_file','施工文件'),('acceptance_file','验收文件'),('approval_file','行政审批文件'),
('periodic_file','定期检查资料'),('special_file','特殊检查资料'),('maintenance_file','历次维修加固资料'),('other','其他档案');
INSERT INTO tb_check_category VALUES ('initial','初始检查'),('daily','日常巡查'),('frequent','经常检查'),('periodic','定期检查'),('special','特殊检查');
INSERT INTO tb_treatment_category VALUES ('repair','维修'),('reinforce','加固'),('rebuild','改造');
INSERT INTO tb_rating_level VALUES ('1','1类'),('2','2类'),('3','3类'),('4','4类'),('5','5类');
INSERT INTO tb_defect_degree VALUES ('slight','轻微'),('medium','中等'),('serious','严重'),('danger','危险');

INSERT INTO tb_bridge (bridge_code,route_code,bridge_type_code,bridge_name,road_management_org,management_unit,maintenance_level,bridge_length,deck_width,create_by)
VALUES ('G75-001','G75','beam','兰海高速示范桥','省公路管理局','第一养护中心','Ⅰ',368.50,24.50,2);

INSERT INTO tb_inspection_task (task_id,bridge_code,inspection_type,inspection_level,plan_start_date,plan_end_date,task_status,remarks,creator_id)
VALUES ('JC260001','G75-001','initial','Ⅰ',CURRENT_DATE(),DATE_ADD(CURRENT_DATE(),INTERVAL 7 DAY),'待分配','示范初始检查任务',2);
