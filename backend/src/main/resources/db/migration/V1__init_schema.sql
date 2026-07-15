CREATE TABLE IF NOT EXISTS tb_user (
    user_id           VARCHAR(20)   NOT NULL PRIMARY KEY COMMENT '用户ID',
    user_name         VARCHAR(50)   NOT NULL COMMENT '用户姓名',
    login_account     VARCHAR(30)   NOT NULL COMMENT '登录账号',
    password          VARCHAR(128)  NOT NULL COMMENT '登录密码bcrypt',
    department        VARCHAR(100)  NULL COMMENT '所属部门',
    phone             VARCHAR(20)   NULL COMMENT '联系电话',
    email             VARCHAR(50)   NULL COMMENT '邮箱',
    user_status       TINYINT       DEFAULT 1 COMMENT '1启用/0停用',
    force_pwd_change  TINYINT       DEFAULT 1 COMMENT '是否强制修改密码',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_login_account (login_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS tb_role (
    role_code   VARCHAR(20) NOT NULL PRIMARY KEY COMMENT '角色代码',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_desc   VARCHAR(200) NULL COMMENT '角色描述',
    is_default  TINYINT DEFAULT 0 COMMENT '是否系统预置角色',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS tb_permission (
    perm_id         VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '权限ID',
    perm_code       VARCHAR(50) NOT NULL COMMENT '权限代码',
    perm_name       VARCHAR(50) NOT NULL COMMENT '权限名称',
    module_code     VARCHAR(20) NULL COMMENT '模块代码',
    operation_code  VARCHAR(10) NULL COMMENT '操作代码',
    UNIQUE INDEX uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS tb_user_role (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(20) NOT NULL,
    role_code  VARCHAR(20) NOT NULL,
    UNIQUE INDEX uk_user_role (user_id, role_code),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES tb_user(user_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_code) REFERENCES tb_role(role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS tb_role_permission (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code  VARCHAR(20) NOT NULL,
    perm_id    VARCHAR(30) NOT NULL,
    UNIQUE INDEX uk_role_perm (role_code, perm_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_code) REFERENCES tb_role(role_code),
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES tb_permission(perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

CREATE TABLE IF NOT EXISTS tb_dict_type (
    type_code   VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '字典类型编码',
    type_name   VARCHAR(50) NOT NULL COMMENT '字典类型名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

CREATE TABLE IF NOT EXISTS tb_dict_item (
    item_id     INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '字典项ID',
    type_code   VARCHAR(30) NOT NULL COMMENT '关联字典类型编码',
    item_code   VARCHAR(30) NOT NULL COMMENT '字典项编码',
    item_name   VARCHAR(50) NOT NULL COMMENT '字典项名称',
    sort_order  INT DEFAULT 0 COMMENT '排序号',
    is_active   TINYINT DEFAULT 1 COMMENT '是否启用',
    UNIQUE INDEX uk_dict_type_item (type_code, item_code),
    CONSTRAINT fk_dict_type FOREIGN KEY (type_code) REFERENCES tb_dict_type(type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项表';

CREATE TABLE IF NOT EXISTS tb_bridge (
    bridge_code             VARCHAR(20) NOT NULL PRIMARY KEY COMMENT '桥梁编码',
    bridge_name             VARCHAR(100) NOT NULL COMMENT '桥梁名称',
    route_code              VARCHAR(20) NULL COMMENT '路线编号',
    route_name              VARCHAR(100) NULL COMMENT '路线名称',
    route_grade             VARCHAR(20) NULL COMMENT '路线等级',
    pile_number             VARCHAR(50) NULL COMMENT '桥位桩号',
    functional_type         VARCHAR(20) NULL COMMENT '功能类型',
    across_name             VARCHAR(100) NULL COMMENT '被跨越道路名称',
    across_pile             VARCHAR(50) NULL COMMENT '被跨越道路桩号',
    bridge_category         VARCHAR(20) NULL COMMENT '桥梁分类',
    structure_type          VARCHAR(30) NOT NULL COMMENT '结构类型',
    structural_system       VARCHAR(50) NULL COMMENT '结构体系',
    bridge_holes            VARCHAR(100) NULL COMMENT '桥梁分孔',
    bridge_slope            VARCHAR(30) NULL COMMENT '桥梁坡度',
    horizontal_curve_radius DECIMAL(10,2) NULL COMMENT '平曲线半径',
    design_load             VARCHAR(30) NULL COMMENT '设计荷载等级',
    complete_date           DATE NULL COMMENT '竣工日期',
    acceptance_date         DATE NULL COMMENT '交工验收日期',
    design_unit             VARCHAR(100) NULL COMMENT '设计单位',
    constr_company          VARCHAR(100) NULL COMMENT '施工单位',
    super_company           VARCHAR(100) NULL COMMENT '监理单位',
    owner_unit              VARCHAR(100) NULL COMMENT '业主单位',
    manage_unit             VARCHAR(100) NULL COMMENT '管养单位',
    bridge_length           DECIMAL(10,2) NULL COMMENT '桥梁全长',
    bridge_width            DECIMAL(10,2) NULL COMMENT '桥面总宽',
    lane_width              DECIMAL(6,2) NULL COMMENT '车道宽度',
    sidewalk_width          DECIMAL(6,2) NULL COMMENT '人行道宽度',
    barrier_height          DECIMAL(6,2) NULL COMMENT '护栏高度',
    median_width            DECIMAL(6,2) NULL COMMENT '中央分隔带宽度',
    standard_clearance      DECIMAL(6,2) NULL COMMENT '标准净空',
    actual_clearance        DECIMAL(6,2) NULL COMMENT '实际净空',
    navigation_level        VARCHAR(20) NULL COMMENT '通航等级',
    nav_clearance_standard  DECIMAL(6,2) NULL COMMENT '桥下通航标准净空',
    nav_clearance_actual    DECIMAL(6,2) NULL COMMENT '桥下实际净空',
    approach_width          DECIMAL(6,2) NULL COMMENT '引道总宽',
    approach_alignment      VARCHAR(100) NULL COMMENT '引道线形',
    design_flood_freq       VARCHAR(50) NULL COMMENT '设计洪水频率',
    design_flood_level      DECIMAL(6,2) NULL COMMENT '设计洪水位',
    historical_flood_level  DECIMAL(6,2) NULL COMMENT '历史洪水位',
    seismic_level           VARCHAR(20) NULL COMMENT '抗震设防等级',
    deck_elevation          TEXT NULL COMMENT '桥面高程JSON',
    superstructure_type     VARCHAR(50) NULL COMMENT '上部结构形式',
    substructure_type       VARCHAR(50) NULL COMMENT '下部结构形式',
    foundation_type         VARCHAR(50) NULL COMMENT '基础形式',
    span_combination        VARCHAR(100) NULL COMMENT '跨径组合',
    deck_pavement_type      VARCHAR(50) NULL COMMENT '桥面铺装类型',
    expansion_joint_type    VARCHAR(50) NULL COMMENT '伸缩缝类型',
    arch_construction       VARCHAR(100) NULL COMMENT '拱上建筑',
    regulation_structure    VARCHAR(100) NULL COMMENT '调治构造物',
    navigation_mark_drainage VARCHAR(100) NULL COMMENT '航标及排水系统',
    maintenance_level       VARCHAR(2) NULL COMMENT '养护检查等级',
    constr_unit             VARCHAR(100) NULL COMMENT '建设单位名称',
    last_inspection_date    DATE NULL COMMENT '最近检查日期',
    front_photo_path        VARCHAR(500) NULL COMMENT '桥面正面照片路径',
    left_photo_path         VARCHAR(500) NULL COMMENT '左侧立面照片路径',
    right_photo_path        VARCHAR(500) NULL COMMENT '右侧立面照片路径',
    overall_photo_path      VARCHAR(500) NULL COMMENT '桥梁总体照片路径',
    is_deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bridge_name (bridge_name),
    INDEX idx_bridge_route (route_code),
    INDEX idx_bridge_structure (structure_type),
    INDEX idx_bridge_manage (manage_unit),
    INDEX idx_bridge_level (maintenance_level),
    INDEX idx_bridge_deleted (is_deleted),
    INDEX idx_bridge_category (bridge_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桥梁基本信息表';

CREATE TABLE IF NOT EXISTS tb_inspection_task (
    task_id            VARCHAR(20) NOT NULL PRIMARY KEY COMMENT '任务编号',
    bridge_code        VARCHAR(20) NOT NULL COMMENT '关联桥梁编码',
    inspection_type    VARCHAR(10) NOT NULL COMMENT '检查类型',
    inspection_level   VARCHAR(2) NULL COMMENT '检查等级',
    plan_start_date    DATE NULL COMMENT '计划开始日期',
    plan_end_date      DATE NULL COMMENT '计划结束日期',
    actual_start_date  DATE NULL COMMENT '实际开始日期',
    actual_end_date    DATE NULL COMMENT '实际结束日期',
    task_status        VARCHAR(10) NOT NULL DEFAULT '待分配' COMMENT '任务状态',
    remarks            VARCHAR(500) NULL COMMENT '备注',
    cancel_reason      VARCHAR(200) NULL COMMENT '取消原因',
    creator_id         VARCHAR(20) NOT NULL COMMENT '创建人用户ID',
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_bridge_type (bridge_code, inspection_type),
    INDEX idx_task_status (task_status),
    INDEX idx_task_creator (creator_id),
    CONSTRAINT fk_task_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查任务表';

CREATE TABLE IF NOT EXISTS tb_task_assignment (
    assignment_id  VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '分配记录ID',
    task_id        VARCHAR(20) NOT NULL COMMENT '任务编号',
    user_id        VARCHAR(20) NOT NULL COMMENT '用户ID',
    assign_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_task_user (task_id, user_id),
    CONSTRAINT fk_assign_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_assign_user FOREIGN KEY (user_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务-人员分配表';

CREATE TABLE IF NOT EXISTS tb_task_status_history (
    history_id     VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '历史记录ID',
    task_id        VARCHAR(20) NOT NULL COMMENT '任务编号',
    from_status    VARCHAR(10) NULL COMMENT '变更前状态',
    to_status      VARCHAR(10) NOT NULL COMMENT '变更后状态',
    change_reason  VARCHAR(200) NULL COMMENT '变更原因',
    operator_id    VARCHAR(20) NULL COMMENT '操作人',
    change_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_task (task_id),
    CONSTRAINT fk_history_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_history_operator FOREIGN KEY (operator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务状态历史表';

CREATE TABLE IF NOT EXISTS tb_inspection_data (
    data_id          VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '检测数据ID',
    task_id          VARCHAR(20) NOT NULL COMMENT '任务编号',
    component_code   VARCHAR(30) NOT NULL COMMENT '构件编码',
    component_name   VARCHAR(50) NULL COMMENT '构件名称',
    item_code        VARCHAR(20) NOT NULL COMMENT '检测项目编码',
    item_name        VARCHAR(50) NOT NULL COMMENT '检测项目名称',
    measured_value   VARCHAR(100) NULL COMMENT '实测值',
    unit             VARCHAR(10) NULL COMMENT '单位',
    is_mandatory     TINYINT DEFAULT 0 COMMENT '是否必填',
    data_status      VARCHAR(10) DEFAULT '草稿' COMMENT '数据状态',
    data_version     INT DEFAULT 1 COMMENT '数据版本号',
    data_source      VARCHAR(20) DEFAULT '现场检测' COMMENT '数据来源',
    deviation_note   VARCHAR(200) NULL COMMENT '偏差说明',
    inspector_id     VARCHAR(20) NULL COMMENT '检测人员',
    instrument_code  VARCHAR(30) NULL COMMENT '仪器编号',
    photo_path       VARCHAR(500) NULL COMMENT '检测照片路径',
    inspection_date  DATE NULL COMMENT '检测日期',
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_data_task_comp (task_id, component_code),
    INDEX idx_data_status (data_status),
    CONSTRAINT fk_data_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_data_inspector FOREIGN KEY (inspector_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测数据表';

CREATE TABLE IF NOT EXISTS tb_defect_record (
    defect_id           VARCHAR(20) NOT NULL PRIMARY KEY COMMENT '缺损编号',
    task_id             VARCHAR(20) NOT NULL COMMENT '任务编号',
    component_path      VARCHAR(200) NOT NULL COMMENT '构件层级路径',
    defect_type         VARCHAR(20) NOT NULL COMMENT '缺损类型',
    defect_degree       VARCHAR(10) NOT NULL COMMENT '缺损程度',
    scale_rating        INT NULL COMMENT '标度值',
    defect_description  TEXT NOT NULL COMMENT '缺损描述',
    crack_length        DECIMAL(8,1) NULL COMMENT '裂缝长度',
    crack_width         DECIMAL(6,2) NULL COMMENT '裂缝宽度',
    crack_direction     VARCHAR(10) NULL COMMENT '裂缝走向',
    is_penetrated       TINYINT DEFAULT 0 COMMENT '是否贯通',
    recorder_id         VARCHAR(20) NULL COMMENT '记录人员',
    record_date         DATETIME NOT NULL COMMENT '记录日期',
    is_deleted          TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_defect_task (task_id),
    INDEX idx_defect_type (defect_type),
    INDEX idx_defect_degree (defect_degree),
    INDEX idx_defect_deleted (is_deleted),
    CONSTRAINT fk_defect_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_defect_recorder FOREIGN KEY (recorder_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺损记录表';

CREATE TABLE IF NOT EXISTS tb_defect_photo (
    photo_id        VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '照片ID',
    defect_id       VARCHAR(20) NOT NULL COMMENT '缺损编号',
    photo_type      VARCHAR(10) NOT NULL COMMENT '照片类型',
    photo_path      VARCHAR(500) NOT NULL COMMENT '照片路径',
    thumbnail_path  VARCHAR(500) NULL COMMENT '缩略图路径',
    upload_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_photo_defect (defect_id),
    CONSTRAINT fk_photo_defect FOREIGN KEY (defect_id) REFERENCES tb_defect_record(defect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺损照片表';

CREATE TABLE IF NOT EXISTS tb_defect_coordinate (
    coordinate_id  VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '坐标记录ID',
    defect_id      VARCHAR(20) NOT NULL COMMENT '缺损编号',
    diagram_type   VARCHAR(20) NULL COMMENT '示意图类型',
    x_position     DECIMAL(5,2) NULL COMMENT 'X坐标百分比',
    y_position     DECIMAL(5,2) NULL COMMENT 'Y坐标百分比',
    INDEX idx_coord_defect (defect_id),
    CONSTRAINT fk_coord_defect FOREIGN KEY (defect_id) REFERENCES tb_defect_record(defect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺损标注坐标表';

CREATE TABLE IF NOT EXISTS tb_report (
    report_id         VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '报告ID',
    task_id           VARCHAR(20) NOT NULL COMMENT '任务编号',
    report_type       VARCHAR(20) NOT NULL COMMENT '报告类型',
    version_no        VARCHAR(10) NOT NULL COMMENT '版本号',
    file_format       VARCHAR(10) NULL COMMENT '文件格式',
    file_path         VARCHAR(500) NULL COMMENT '文件路径',
    report_status     VARCHAR(10) NOT NULL COMMENT '报告状态',
    generation_time   DATETIME NOT NULL COMMENT '生成时间',
    generator_id      VARCHAR(20) NULL COMMENT '生成人',
    change_summary    VARCHAR(300) NULL COMMENT '变更摘要',
    INDEX idx_report_task_type (task_id, report_type),
    INDEX idx_report_status (report_status),
    CONSTRAINT fk_report_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_report_generator FOREIGN KEY (generator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告表';

CREATE TABLE IF NOT EXISTS tb_archive_file (
    archive_id       VARCHAR(30) NOT NULL PRIMARY KEY COMMENT '档案ID',
    bridge_code      VARCHAR(20) NOT NULL COMMENT '桥梁编码',
    archive_name     VARCHAR(200) NOT NULL COMMENT '档案名称',
    archive_type     VARCHAR(30) NULL COMMENT '档案类型',
    file_path        VARCHAR(500) NOT NULL COMMENT '文件路径',
    upload_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upload_user_id   VARCHAR(20) NULL COMMENT '上传人',
    INDEX idx_archive_bridge (bridge_code),
    CONSTRAINT fk_archive_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    CONSTRAINT fk_archive_user FOREIGN KEY (upload_user_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案文件表';

CREATE TABLE IF NOT EXISTS tb_operation_log (
    log_id            INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id           VARCHAR(20) NULL COMMENT '操作人用户ID',
    user_name         VARCHAR(50) NULL COMMENT '操作人姓名',
    ip_address        VARCHAR(50) NULL COMMENT 'IP地址',
    module            VARCHAR(20) NULL COMMENT '操作模块',
    operation_type    VARCHAR(10) NULL COMMENT '操作类型',
    target_table      VARCHAR(50) NULL COMMENT '操作对象表名',
    target_id         VARCHAR(50) NULL COMMENT '操作对象记录ID',
    description       VARCHAR(500) NULL COMMENT '操作描述',
    operation_result  VARCHAR(10) NULL COMMENT '操作结果',
    operate_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_time (operate_time),
    INDEX idx_log_user (user_id),
    INDEX idx_log_module (module),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS tb_system_version (
    id             INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version_no     VARCHAR(30) NOT NULL,
    git_commit     VARCHAR(80) NULL,
    build_time     DATETIME NULL,
    repository_url VARCHAR(300) NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统版本表';

CREATE TABLE IF NOT EXISTS tb_update_package (
    update_id       VARCHAR(30) NOT NULL PRIMARY KEY,
    version_no      VARCHAR(30) NOT NULL,
    release_date    VARCHAR(30) NULL,
    release_notes   TEXT NULL,
    download_url    VARCHAR(1000) NOT NULL,
    sha256          VARCHAR(128) NULL,
    package_size    BIGINT NULL,
    local_path      VARCHAR(500) NULL,
    download_status VARCHAR(20) NOT NULL DEFAULT '待下载',
    operator_id     VARCHAR(20) NULL,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_update_version (version_no),
    INDEX idx_update_status (download_status),
    CONSTRAINT fk_update_operator FOREIGN KEY (operator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统更新包记录表';

CREATE OR REPLACE VIEW v_bridge_overview AS
SELECT
    b.*,
    t.task_id AS latest_task_id,
    t.inspection_type AS latest_inspection_type,
    t.task_status AS latest_task_status,
    t.inspection_date AS latest_inspection_date
FROM tb_bridge b
LEFT JOIN (
    SELECT bridge_code, task_id, inspection_type, task_status,
           COALESCE(actual_start_date, plan_start_date) AS inspection_date,
           ROW_NUMBER() OVER (PARTITION BY bridge_code ORDER BY create_time DESC) AS rn
    FROM tb_inspection_task
) t ON b.bridge_code = t.bridge_code AND t.rn = 1
WHERE b.is_deleted = 0;

CREATE OR REPLACE VIEW v_task_progress AS
SELECT bridge_code, inspection_type, task_status, COUNT(*) AS task_count
FROM tb_inspection_task
GROUP BY bridge_code, inspection_type, task_status;

CREATE OR REPLACE VIEW v_defect_summary AS
SELECT t.bridge_code, d.component_path, d.defect_type, d.defect_degree, COUNT(*) AS defect_count
FROM tb_defect_record d
JOIN tb_inspection_task t ON d.task_id = t.task_id
WHERE d.is_deleted = 0
GROUP BY t.bridge_code, d.component_path, d.defect_type, d.defect_degree;
