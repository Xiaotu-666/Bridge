CREATE TABLE tb_role (
  role_id INT AUTO_INCREMENT PRIMARY KEY, role_code VARCHAR(30) NOT NULL UNIQUE,
  role_name VARCHAR(50) NOT NULL UNIQUE, role_desc VARCHAR(200), permission_set JSON NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_user (
  user_id INT AUTO_INCREMENT PRIMARY KEY, user_name VARCHAR(50) NOT NULL,
  login_account VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL,
  role_id INT NOT NULL, department VARCHAR(100), phone VARCHAR(50), email VARCHAR(100),
  user_status TINYINT NOT NULL DEFAULT 1, force_pwd_change TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, last_login_time DATETIME,
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES tb_role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_route (
  route_code VARCHAR(20) PRIMARY KEY, route_name VARCHAR(100) NOT NULL,
  route_grade VARCHAR(20), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge_type (
  bridge_type_code VARCHAR(32) PRIMARY KEY, bridge_type_name VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_part (
  part_code VARCHAR(32) PRIMARY KEY, part_name VARCHAR(50) NOT NULL UNIQUE, sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_component (
  component_code VARCHAR(32) PRIMARY KEY, component_name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500), active_flag TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge_type_component_config (
  config_id INT AUTO_INCREMENT PRIMARY KEY, bridge_type_code VARCHAR(32) NOT NULL,
  part_code VARCHAR(32) NOT NULL, component_code VARCHAR(32) NOT NULL,
  display_order INT NOT NULL DEFAULT 0, active_flag TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT fk_btc_type FOREIGN KEY (bridge_type_code) REFERENCES tb_bridge_type(bridge_type_code),
  CONSTRAINT fk_btc_part FOREIGN KEY (part_code) REFERENCES tb_part(part_code),
  CONSTRAINT fk_btc_component FOREIGN KEY (component_code) REFERENCES tb_component(component_code),
  UNIQUE KEY uk_btc_matrix (bridge_type_code, part_code, component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge (
  bridge_code VARCHAR(30) PRIMARY KEY, route_code VARCHAR(20) NOT NULL,
  bridge_type_code VARCHAR(32) NOT NULL, bridge_name VARCHAR(100) NOT NULL,
  administrative_code VARCHAR(20), pile_number VARCHAR(50), road_management_org VARCHAR(100) NOT NULL,
  function_type VARCHAR(30), crossed_road_name VARCHAR(100), crossed_road_pile VARCHAR(50),
  maintenance_level VARCHAR(10), design_load VARCHAR(50), bridge_slope VARCHAR(50),
  curve_radius VARCHAR(50), built_year SMALLINT, design_unit VARCHAR(100),
  construction_unit VARCHAR(100), supervision_unit VARCHAR(100), owner_unit VARCHAR(100),
  management_unit VARCHAR(100), bridge_length DECIMAL(10,2), deck_width DECIMAL(10,2),
  lane_width DECIMAL(10,2), sidewalk_width DECIMAL(10,2), barrier_height DECIMAL(5,2),
  median_width DECIMAL(10,2), standard_clearance VARCHAR(50), actual_clearance VARCHAR(50),
  navigation_standard VARCHAR(50), navigation_actual VARCHAR(50), approach_width DECIMAL(10,2),
  approach_alignment VARCHAR(100), design_flood VARCHAR(100), historical_flood VARCHAR(100),
  seismic_coefficient VARCHAR(50), span_combination VARCHAR(200), structural_system VARCHAR(100),
  archive_form VARCHAR(20), archive_date DATE, notes TEXT, bridge_engineer VARCHAR(50),
  card_filler VARCHAR(50), card_date DATE, status TINYINT NOT NULL DEFAULT 1, create_by INT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_bridge_route FOREIGN KEY (route_code) REFERENCES tb_route(route_code),
  CONSTRAINT fk_bridge_type FOREIGN KEY (bridge_type_code) REFERENCES tb_bridge_type(bridge_type_code),
  CONSTRAINT fk_bridge_creator FOREIGN KEY (create_by) REFERENCES tb_user(user_id),
  INDEX idx_bridge_route (route_code), INDEX idx_bridge_type (bridge_type_code), INDEX idx_bridge_name (bridge_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge_specific_component (
  bridge_component_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  config_id INT, part_code VARCHAR(32) NOT NULL, component_code VARCHAR(32) NOT NULL,
  component_serial VARCHAR(30) NOT NULL, location_desc VARCHAR(100), material_type VARCHAR(50),
  dimension_spec VARCHAR(100), quantity INT NOT NULL DEFAULT 1, force_value DECIMAL(10,2),
  elevation_displacement DECIMAL(10,3), custom_flag TINYINT NOT NULL DEFAULT 0,
  custom_reason VARCHAR(300), status TINYINT NOT NULL DEFAULT 1, remark TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_bsc_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_bsc_config FOREIGN KEY (config_id) REFERENCES tb_bridge_type_component_config(config_id),
  CONSTRAINT fk_bsc_part FOREIGN KEY (part_code) REFERENCES tb_part(part_code),
  CONSTRAINT fk_bsc_component FOREIGN KEY (component_code) REFERENCES tb_component(component_code),
  UNIQUE KEY uk_bsc_instance (bridge_code, part_code, component_code, component_serial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_archive_item (
  archive_item_code VARCHAR(32) PRIMARY KEY, archive_item_name VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge_archive_record (
  archive_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  archive_item_code VARCHAR(32) NOT NULL, completeness_status VARCHAR(10), description TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_archive_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_archive_item FOREIGN KEY (archive_item_code) REFERENCES tb_archive_item(archive_item_code),
  UNIQUE KEY uk_bridge_archive_item (bridge_code, archive_item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_check_category (check_category_code VARCHAR(32) PRIMARY KEY, check_category_name VARCHAR(50) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE tb_treatment_category (treatment_category_code VARCHAR(32) PRIMARY KEY, treatment_category_name VARCHAR(50) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE tb_rating_level (rating_level_code VARCHAR(32) PRIMARY KEY, rating_level_name VARCHAR(50) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE tb_defect_degree (defect_degree_code VARCHAR(32) PRIMARY KEY, defect_degree_name VARCHAR(50) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_evaluation_history (
  evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  evaluation_date DATE NOT NULL, check_category_code VARCHAR(32), rating_result VARCHAR(200),
  special_conclusion VARCHAR(200), treatment_strategy VARCHAR(200), next_check_date DATE,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eval_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_eval_category FOREIGN KEY (check_category_code) REFERENCES tb_check_category(check_category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_maintenance_record (
  maintenance_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  treatment_date DATE NOT NULL, treatment_category_code VARCHAR(32), reason TEXT, scope TEXT,
  cost DECIMAL(12,2), fund_source VARCHAR(100), quality_rating VARCHAR(50), owner_unit VARCHAR(100),
  design_unit VARCHAR(100), construction_unit VARCHAR(100), supervision_unit VARCHAR(100),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_maintenance_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_maintenance_category FOREIGN KEY (treatment_category_code) REFERENCES tb_treatment_category(treatment_category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_initial_inspection_item_definition (
  item_code VARCHAR(20) PRIMARY KEY, item_name VARCHAR(100) NOT NULL,
  unit VARCHAR(30), item_category VARCHAR(50), description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_bridge_type_initial_item_config (
  config_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_type_code VARCHAR(32) NOT NULL,
  item_code VARCHAR(20) NOT NULL, requirement_type VARCHAR(20) NOT NULL,
  trigger_condition VARCHAR(200), display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_btici_type FOREIGN KEY (bridge_type_code) REFERENCES tb_bridge_type(bridge_type_code),
  CONSTRAINT fk_btici_item FOREIGN KEY (item_code) REFERENCES tb_initial_inspection_item_definition(item_code),
  UNIQUE KEY uk_btici_matrix (bridge_type_code, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_initial_inspection (
  initial_inspection_code VARCHAR(30) PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  inspection_date DATE NOT NULL, inspection_org VARCHAR(100), inspectors VARCHAR(100),
  bridge_engineer VARCHAR(50), weather_temperature VARCHAR(100), main_span_structure VARCHAR(100),
  maximum_span DECIMAL(10,2), span_combination VARCHAR(200), structure_form VARCHAR(200),
  construction_method VARCHAR(200), construction_rework TEXT, reinforcement_info TEXT,
  missing_archive_drawings TEXT, defect_advice TEXT, status VARCHAR(20) NOT NULL DEFAULT 'draft',
  create_by INT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_initial_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_initial_creator FOREIGN KEY (create_by) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_initial_inspection_item (
  item_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, initial_inspection_code VARCHAR(30) NOT NULL,
  item_code VARCHAR(20) NOT NULL, applicable_flag TINYINT NOT NULL DEFAULT 1,
  trigger_result VARCHAR(200), measured_value TEXT, inspection_description TEXT,
  CONSTRAINT fk_initial_item_record FOREIGN KEY (initial_inspection_code) REFERENCES tb_initial_inspection(initial_inspection_code),
  CONSTRAINT fk_initial_item_definition FOREIGN KEY (item_code) REFERENCES tb_initial_inspection_item_definition(item_code),
  UNIQUE KEY uk_initial_item (initial_inspection_code, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_periodic_inspection (
  periodic_inspection_code VARCHAR(30) PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  inspection_date DATE NOT NULL, last_inspection_date DATE, last_maintenance_date DATE,
  weather_temperature VARCHAR(100), rating_level_code VARCHAR(32), cleanliness VARCHAR(100),
  maintenance_status VARCHAR(200), next_inspection_date DATE, recorder VARCHAR(50), principal VARCHAR(50),
  status VARCHAR(20) NOT NULL DEFAULT 'draft', create_by INT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_periodic_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_periodic_rating FOREIGN KEY (rating_level_code) REFERENCES tb_rating_level(rating_level_code),
  CONSTRAINT fk_periodic_creator FOREIGN KEY (create_by) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_component_inspection (
  component_inspection_id BIGINT AUTO_INCREMENT PRIMARY KEY, periodic_inspection_code VARCHAR(30) NOT NULL,
  bridge_component_id BIGINT, part_code VARCHAR(32) NOT NULL, component_code VARCHAR(32) NOT NULL,
  defect_type VARCHAR(100), defect_location VARCHAR(200), defect_range VARCHAR(200),
  defect_degree_code VARCHAR(32), worst_component VARCHAR(50), score DECIMAL(5,1),
  maintenance_advice TEXT, special_check_required TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ci_periodic FOREIGN KEY (periodic_inspection_code) REFERENCES tb_periodic_inspection(periodic_inspection_code),
  CONSTRAINT fk_ci_specific FOREIGN KEY (bridge_component_id) REFERENCES tb_bridge_specific_component(bridge_component_id),
  CONSTRAINT fk_ci_part FOREIGN KEY (part_code) REFERENCES tb_part(part_code),
  CONSTRAINT fk_ci_component FOREIGN KEY (component_code) REFERENCES tb_component(component_code),
  CONSTRAINT fk_ci_degree FOREIGN KEY (defect_degree_code) REFERENCES tb_defect_degree(defect_degree_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_defect (
  defect_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL,
  initial_inspection_code VARCHAR(30), component_inspection_id BIGINT, defect_part_code VARCHAR(32),
  defect_type VARCHAR(50), defect_nature VARCHAR(50), defect_range VARCHAR(200),
  defect_quantity VARCHAR(50), defect_degree_code VARCHAR(32), description TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_defect_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_defect_initial FOREIGN KEY (initial_inspection_code) REFERENCES tb_initial_inspection(initial_inspection_code),
  CONSTRAINT fk_defect_component FOREIGN KEY (component_inspection_id) REFERENCES tb_component_inspection(component_inspection_id),
  CONSTRAINT fk_defect_part FOREIGN KEY (defect_part_code) REFERENCES tb_part(part_code),
  CONSTRAINT fk_defect_degree FOREIGN KEY (defect_degree_code) REFERENCES tb_defect_degree(defect_degree_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_attachment (
  file_id BIGINT AUTO_INCREMENT PRIMARY KEY, bridge_code VARCHAR(30), archive_record_id BIGINT,
  initial_inspection_code VARCHAR(30), component_inspection_id BIGINT, defect_id BIGINT,
  file_name VARCHAR(255) NOT NULL, stored_file_name VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL, file_type VARCHAR(50), file_size BIGINT,
  file_description VARCHAR(255), upload_by INT, upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attachment_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_attachment_archive FOREIGN KEY (archive_record_id) REFERENCES tb_bridge_archive_record(archive_record_id),
  CONSTRAINT fk_attachment_initial FOREIGN KEY (initial_inspection_code) REFERENCES tb_initial_inspection(initial_inspection_code),
  CONSTRAINT fk_attachment_ci FOREIGN KEY (component_inspection_id) REFERENCES tb_component_inspection(component_inspection_id),
  CONSTRAINT fk_attachment_defect FOREIGN KEY (defect_id) REFERENCES tb_defect(defect_id),
  CONSTRAINT fk_attachment_user FOREIGN KEY (upload_by) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_inspection_task (
  task_id VARCHAR(30) PRIMARY KEY, bridge_code VARCHAR(30) NOT NULL, inspection_type VARCHAR(20) NOT NULL,
  inspection_level VARCHAR(10), plan_start_date DATE, plan_end_date DATE, actual_start_date DATE,
  actual_end_date DATE, task_status VARCHAR(20) NOT NULL DEFAULT '待分配', remarks VARCHAR(500),
  cancel_reason VARCHAR(300), creator_id INT NOT NULL, reviewer_id INT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
  CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES tb_user(user_id),
  CONSTRAINT fk_task_reviewer FOREIGN KEY (reviewer_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_task_assignment (
  assignment_id VARCHAR(40) PRIMARY KEY, task_id VARCHAR(30) NOT NULL, user_id INT NOT NULL,
  assignment_status VARCHAR(20) NOT NULL DEFAULT '待分配', assign_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accept_time DATETIME, complete_time DATETIME,
  CONSTRAINT fk_assignment_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
  CONSTRAINT fk_assignment_user FOREIGN KEY (user_id) REFERENCES tb_user(user_id),
  UNIQUE KEY uk_task_user (task_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_task_status_history (
  history_id VARCHAR(40) PRIMARY KEY, task_id VARCHAR(30) NOT NULL, from_status VARCHAR(20),
  to_status VARCHAR(20) NOT NULL, operator_id INT NOT NULL, operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  opinion VARCHAR(500), CONSTRAINT fk_history_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
  CONSTRAINT fk_history_operator FOREIGN KEY (operator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_report (
  report_id VARCHAR(40) PRIMARY KEY, task_id VARCHAR(30), initial_inspection_code VARCHAR(30),
  periodic_inspection_code VARCHAR(30), report_type VARCHAR(30) NOT NULL, version_no VARCHAR(20) NOT NULL,
  file_format VARCHAR(20), file_path VARCHAR(500), report_status VARCHAR(20) NOT NULL DEFAULT '草稿',
  generation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, generator_id INT NOT NULL,
  reviewer_id INT, change_summary VARCHAR(500),
  CONSTRAINT fk_report_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
  CONSTRAINT fk_report_initial FOREIGN KEY (initial_inspection_code) REFERENCES tb_initial_inspection(initial_inspection_code),
  CONSTRAINT fk_report_periodic FOREIGN KEY (periodic_inspection_code) REFERENCES tb_periodic_inspection(periodic_inspection_code),
  CONSTRAINT fk_report_generator FOREIGN KEY (generator_id) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_operation_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT, user_name VARCHAR(50), ip_address VARCHAR(64),
  module VARCHAR(50), operation_type VARCHAR(50), target_table VARCHAR(100), target_id VARCHAR(100),
  description VARCHAR(500), operation_result VARCHAR(20), operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES tb_user(user_id), INDEX idx_log_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_backup_record (
  backup_id BIGINT AUTO_INCREMENT PRIMARY KEY, file_name VARCHAR(255) NOT NULL, file_path VARCHAR(500) NOT NULL,
  file_size BIGINT, sha256 VARCHAR(64), execute_by INT, backup_status VARCHAR(20) NOT NULL,
  restore_status VARCHAR(20), error_message VARCHAR(1000), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  restore_time DATETIME, CONSTRAINT fk_backup_user FOREIGN KEY (execute_by) REFERENCES tb_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
