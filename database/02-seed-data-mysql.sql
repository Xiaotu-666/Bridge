-- ============================================================
-- 公路桥梁初始检查信息系统 — 种子数据
-- 数据库平台: MySQL 8.0
-- 来源: 概要设计 §3.2.2 逻辑模型设计 & §3.2.3 物理模型设计
-- ============================================================

USE bridge_inspection;

-- ============================================================
-- 1. 字典类型
-- ============================================================
INSERT INTO tb_dict_type (type_code, type_name) VALUES
('route_grade',       '路线等级'),
('bridge_category',   '桥梁分类'),
('structure_type',    '结构类型（桥型）'),
('inspection_type',   '检查类型'),
('inspection_level',  '检查等级'),
('maintenance_level', '养护检查等级'),
('task_status',       '任务状态'),
('defect_type',       '缺损类型'),
('defect_degree',     '缺损程度'),
('report_type',       '报告类型'),
('report_status',     '报告状态'),
('archive_type',      '档案类型'),
('data_status',       '数据状态'),
('data_source',       '数据来源'),
('user_role',         '用户角色'),
('operation_type',    '操作类型'),
('operation_module',  '操作模块'),
('bridge_form',       '桥型编码');


-- ============================================================
-- 2. 字典项
-- ============================================================

-- 路线等级
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('route_grade', 'expressway', '高速公路',   1),
('route_grade', 'class1',     '一级公路',   2),
('route_grade', 'class2',     '二级公路',   3),
('route_grade', 'class3',     '三级公路',   4),
('route_grade', 'class4',     '四级公路',   5);

-- 桥梁分类
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('bridge_category', 'super_large', '特大桥', 1),
('bridge_category', 'large',      '大桥',   2),
('bridge_category', 'medium',     '中桥',   3),
('bridge_category', 'small',      '小桥',   4);

-- 结构类型（桥型判别器 — 决定适用的检测项模板）
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('structure_type', 'beam',           '梁式桥',               1),
('structure_type', 'arch',           '拱桥',                 2),
('structure_type', 'rigid_frame',    '刚架拱桥/桁架拱桥',    3),
('structure_type', 'steel_concrete', '钢-混凝土组合拱桥',    4),
('structure_type', 'cable_stayed',   '斜拉桥',               5),
('structure_type', 'suspension',     '悬索桥',               6);

-- 检查类型
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('inspection_type', 'initial', '初始检查', 1),
('inspection_type', 'regular', '定期检查', 2);

-- 检查等级
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('inspection_level', '1', 'Ⅰ级', 1),
('inspection_level', '2', 'Ⅱ级', 2),
('inspection_level', '3', 'Ⅲ级', 3);

-- 养护检查等级
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('maintenance_level', 'Ⅰ', 'Ⅰ级', 1),
('maintenance_level', 'Ⅱ', 'Ⅱ级', 2),
('maintenance_level', 'Ⅲ', 'Ⅲ级', 3);

-- 任务状态
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('task_status', 'pending',     '待分配', 1),
('task_status', 'in_progress', '进行中', 2),
('task_status', 'completed',   '已完成', 3),
('task_status', 'reviewed',    '已审核', 4),
('task_status', 'cancelled',   '已取消', 5);

-- 缺损类型
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('defect_type', 'crack',       '裂缝',     1),
('defect_type', 'spalling',    '剥落',     2),
('defect_type', 'corrosion',   '锈蚀',     3),
('defect_type', 'deformation', '变形',     4),
('defect_type', 'displacement','位移',     5),
('defect_type', 'seepage',     '渗水',     6),
('defect_type', 'honeycomb',   '蜂窝麻面', 7),
('defect_type', 'other',       '其他',     8);

-- 缺损程度
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('defect_degree', 'slight',  '轻微', 1),
('defect_degree', 'medium',  '中等', 2),
('defect_degree', 'serious', '严重', 3),
('defect_degree', 'danger',  '危险', 4);

-- 报告类型
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('report_type', 'bridge_card',    '基本状况卡片',     1),
('report_type', 'initial_record', '初始检查记录表',   2),
('report_type', 'regular_record', '定期检查记录表',   3),
('report_type', 'comprehensive',  '综合报告',         4);

-- 报告状态
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('report_status', 'draft',    '草稿',   1),
('report_status', 'review',   '待审核', 2),
('report_status', 'archived', '已归档', 3);

-- 档案类型
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('archive_type', 'acceptance',    '交工验收资料', 1),
('archive_type', 'design_dwg',    '设计图纸',     2),
('archive_type', 'asbuilt_dwg',   '竣工图纸',     3),
('archive_type', 'other',         '其他',         4);

-- 数据状态
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('data_status', 'draft',     '草稿',   1),
('data_status', 'submitted', '已提交', 2);

-- 数据来源
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('data_source', 'field',    '现场检测',       1),
('data_source', 'imported', '交工验收导入',   2);

-- 用户角色
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('user_role', 'admin',    '系统管理员',   1),
('user_role', 'engineer', '桥梁工程师',   2),
('user_role', 'inspector','检查员',       3),
('user_role', 'reviewer', '审核人员',     4),
('user_role', 'viewer',   '普通查询用户', 5);

-- 操作类型
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('operation_type', 'login',  '登录', 1),
('operation_type', 'logout', '登出', 2),
('operation_type', 'create', '新增', 3),
('operation_type', 'update', '修改', 4),
('operation_type', 'delete', '删除', 5),
('operation_type', 'review', '审核', 6),
('operation_type', 'export', '导出', 7),
('operation_type', 'backup', '备份', 8),
('operation_type', 'restore','恢复', 9);

-- 操作模块
INSERT INTO tb_dict_item (type_code, item_code, item_name, sort_order) VALUES
('operation_module', 'bridge',       '桥梁信息管理',   1),
('operation_module', 'task',         '检查任务管理',   2),
('operation_module', 'data',         '检查数据采集',   3),
('operation_module', 'defect',       '缺损记录管理',   4),
('operation_module', 'report',       '报告生成',       5),
('operation_module', 'system',       '系统管理',       6);


-- ============================================================
-- 3. 系统预置角色（概要设计 §3.1.1）
-- ============================================================
INSERT INTO tb_role (role_code, role_name, role_desc, is_default) VALUES
('admin',     '系统管理员',   '拥有全部功能权限和操作权限',           1),
('engineer',  '桥梁工程师',   '桥梁信息管理、任务管理、报告生成',     1),
('inspector', '检查员',       '任务接受、数据采集、缺损记录',         1),
('reviewer',  '审核人员',     '数据审核、报告审核',                 1),
('viewer',    '普通查询用户', '各模块只读查询',                     1);


-- ============================================================
-- 4. 权限数据（概要设计 §3.1.1 二 — 模块代码-操作代码格式）
-- ============================================================
-- 桥梁信息管理 (bridge)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P001', 'bridge-view',   '查看桥梁信息', 'bridge', '查看'),
('P002', 'bridge-create', '新增桥梁信息', 'bridge', '新增'),
('P003', 'bridge-edit',   '修改桥梁信息', 'bridge', '修改'),
('P004', 'bridge-delete', '删除桥梁信息', 'bridge', '删除'),
('P005', 'bridge-export', '导出桥梁信息', 'bridge', '导出');

-- 检查任务管理 (task)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P006', 'task-view',   '查看检查任务', 'task', '查看'),
('P007', 'task-create', '创建检查任务', 'task', '新增'),
('P008', 'task-edit',   '修改检查任务', 'task', '修改'),
('P009', 'task-delete', '删除检查任务', 'task', '删除'),
('P010', 'task-review', '审核检查任务', 'task', '审核');

-- 检查数据采集 (data)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P011', 'data-view',   '查看检测数据', 'data', '查看'),
('P012', 'data-create', '录入检测数据', 'data', '新增'),
('P013', 'data-edit',   '修改检测数据', 'data', '修改');

-- 缺损记录管理 (defect)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P014', 'defect-view',   '查看缺损记录', 'defect', '查看'),
('P015', 'defect-create', '新增缺损记录', 'defect', '新增'),
('P016', 'defect-edit',   '修改缺损记录', 'defect', '修改'),
('P017', 'defect-delete', '删除缺损记录', 'defect', '删除');

-- 报告生成 (report)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P018', 'report-view',   '查看报告', 'report', '查看'),
('P019', 'report-create', '生成报告', 'report', '新增'),
('P020', 'report-review', '审核报告', 'report', '审核'),
('P021', 'report-export', '导出报告', 'report', '导出');

-- 系统管理 (system)
INSERT INTO tb_permission (perm_id, perm_code, perm_name, module_code, operation_code) VALUES
('P022', 'system-user',   '用户管理', 'system', '修改'),
('P023', 'system-role',   '角色权限管理', 'system', '修改'),
('P024', 'system-dict',   '数据字典维护', 'system', '修改'),
('P025', 'system-backup', '数据备份恢复', 'system', '修改'),
('P026', 'system-log',    '操作日志查看', 'system', '查看');


-- ============================================================
-- 5. 角色-权限分配
-- ============================================================

-- 系统管理员：全部权限
INSERT INTO tb_role_permission (role_code, perm_id)
SELECT 'admin', perm_id FROM tb_permission;

-- 桥梁工程师：桥梁管理 + 任务管理 + 报告管理（不含删除）
INSERT INTO tb_role_permission (role_code, perm_id) VALUES
('engineer', 'P001'), ('engineer', 'P002'), ('engineer', 'P003'), ('engineer', 'P005'),
('engineer', 'P006'), ('engineer', 'P007'), ('engineer', 'P008'),
('engineer', 'P010'),
('engineer', 'P018'), ('engineer', 'P019'), ('engineer', 'P021');

-- 检查员：任务接受 + 数据采集 + 缺损记录
INSERT INTO tb_role_permission (role_code, perm_id) VALUES
('inspector', 'P006'),
('inspector', 'P011'), ('inspector', 'P012'), ('inspector', 'P013'),
('inspector', 'P014'), ('inspector', 'P015'), ('inspector', 'P016');

-- 审核人员：审核
INSERT INTO tb_role_permission (role_code, perm_id) VALUES
('reviewer', 'P006'), ('reviewer', 'P010'),
('reviewer', 'P011'), ('reviewer', 'P014'),
('reviewer', 'P018'), ('reviewer', 'P020');

-- 普通查询用户：全部只读
INSERT INTO tb_role_permission (role_code, perm_id) VALUES
('viewer', 'P001'), ('viewer', 'P006'), ('viewer', 'P011'),
('viewer', 'P014'), ('viewer', 'P018'), ('viewer', 'P026');


-- ============================================================
-- 6. 默认用户（概要设计 §3.2.3 — 密码: admin123）
-- ============================================================
-- 密码 bcrypt 哈希值 (明文: admin123, 强度 10 rounds)
-- 实际部署时替换为真实 bcrypt 值
INSERT INTO tb_user (user_id, user_name, login_account, password, department, user_status, force_pwd_change) VALUES
('U001', '系统管理员',   'admin',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '信息中心',   1, 0),
('U002', '张工程师',     'zhang',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '养护科',     1, 1),
('U003', '李检查员',     'li',        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '检测中心',   1, 1),
('U004', '王审核员',     'wang',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '质安科',     1, 1),
('U005', '赵访客',       'zhao',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '办公室',     1, 1);


-- ============================================================
-- 7. 用户-角色分配
-- ============================================================
INSERT INTO tb_user_role (user_id, role_code) VALUES
('U001', 'admin'),
('U002', 'engineer'),
('U003', 'inspector'),
('U004', 'reviewer'),
('U005', 'viewer');
