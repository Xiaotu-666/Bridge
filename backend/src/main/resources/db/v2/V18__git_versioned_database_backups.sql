SET NAMES utf8mb4;

ALTER TABLE tb_backup_record
    ADD COLUMN backup_type VARCHAR(30) NOT NULL DEFAULT 'database' COMMENT '备份类型' AFTER backup_id,
    ADD COLUMN version_message VARCHAR(300) NULL COMMENT '版本说明' AFTER backup_type,
    ADD COLUMN git_commit_hash VARCHAR(64) NULL COMMENT 'Git提交哈希' AFTER sha256,
    ADD COLUMN git_branch VARCHAR(100) NULL COMMENT 'Git分支' AFTER git_commit_hash,
    ADD INDEX idx_backup_git_commit (git_commit_hash),
    ADD INDEX idx_backup_create_time (create_time);
