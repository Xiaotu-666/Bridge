SET NAMES utf8mb4;

ALTER TABLE tb_initial_inspection
    ADD COLUMN reviewer_id INT NULL COMMENT '审核人员编号' AFTER status,
    ADD COLUMN review_opinion VARCHAR(500) NULL COMMENT '审核意见' AFTER reviewer_id,
    ADD COLUMN review_time DATETIME NULL COMMENT '审核时间' AFTER review_opinion,
    ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER review_time,
    ADD INDEX idx_initial_review_status (status, review_time),
    ADD CONSTRAINT fk_initial_reviewer FOREIGN KEY (reviewer_id) REFERENCES tb_user(user_id);

ALTER TABLE tb_periodic_inspection
    ADD COLUMN reviewer_id INT NULL COMMENT '审核人员编号' AFTER status,
    ADD COLUMN review_opinion VARCHAR(500) NULL COMMENT '审核意见' AFTER reviewer_id,
    ADD COLUMN review_time DATETIME NULL COMMENT '审核时间' AFTER review_opinion,
    ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER review_time,
    ADD INDEX idx_periodic_review_status (status, review_time),
    ADD CONSTRAINT fk_periodic_reviewer FOREIGN KEY (reviewer_id) REFERENCES tb_user(user_id);

CREATE TABLE tb_inspection_archive (
    archive_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_type VARCHAR(20) NOT NULL COMMENT 'initial 或 periodic',
    record_code VARCHAR(30) NOT NULL COMMENT '检查记录编号',
    bridge_code VARCHAR(30) NOT NULL,
    task_id VARCHAR(30) NULL,
    archived_by INT NOT NULL,
    archived_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_opinion VARCHAR(500) NULL,
    CONSTRAINT uk_inspection_archive_record UNIQUE (inspection_type, record_code),
    CONSTRAINT fk_inspection_archive_bridge FOREIGN KEY (bridge_code) REFERENCES tb_bridge(bridge_code),
    CONSTRAINT fk_inspection_archive_task FOREIGN KEY (task_id) REFERENCES tb_inspection_task(task_id),
    CONSTRAINT fk_inspection_archive_user FOREIGN KEY (archived_by) REFERENCES tb_user(user_id),
    INDEX idx_inspection_archive_bridge (bridge_code, archived_time),
    INDEX idx_inspection_archive_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核通过的初始检查和定期检查档案';

-- Existing approved records are treated as archived so old data uses the same state model.
UPDATE tb_initial_inspection SET status='archived', archive_time=COALESCE(archive_time, update_time, create_time)
WHERE status='approved';

UPDATE tb_periodic_inspection SET status='archived', archive_time=COALESCE(archive_time, update_time, create_time)
WHERE status='approved';

INSERT IGNORE INTO tb_inspection_archive
    (inspection_type, record_code, bridge_code, task_id, archived_by, archived_time, review_opinion)
SELECT 'initial', i.initial_inspection_code, i.bridge_code, i.task_id,
       COALESCE(i.reviewer_id, t.reviewer_id, i.create_by, t.creator_id, 1),
       COALESCE(i.archive_time, i.update_time, i.create_time), '历史归档数据'
FROM tb_initial_inspection i
LEFT JOIN tb_inspection_task t ON t.task_id=i.task_id
WHERE i.status='archived';

INSERT IGNORE INTO tb_inspection_archive
    (inspection_type, record_code, bridge_code, task_id, archived_by, archived_time, review_opinion)
SELECT 'periodic', p.periodic_inspection_code, p.bridge_code, p.task_id,
       COALESCE(p.reviewer_id, t.reviewer_id, p.create_by, t.creator_id, 1),
       COALESCE(p.archive_time, p.update_time, p.create_time), '历史归档数据'
FROM tb_periodic_inspection p
LEFT JOIN tb_inspection_task t ON t.task_id=p.task_id
WHERE p.status='archived';
