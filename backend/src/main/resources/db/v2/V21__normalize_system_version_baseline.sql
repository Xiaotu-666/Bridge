CREATE TABLE IF NOT EXISTS tb_system_version (
    id             INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version_no     VARCHAR(30) NOT NULL,
    git_commit     VARCHAR(80) NULL,
    build_time     DATETIME NULL,
    repository_url VARCHAR(300) NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_system_version_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统版本更新记录';

INSERT INTO tb_system_version (version_no, repository_url)
SELECT 'V1.0', 'https://github.com/Xiaotu-666/Bridge'
WHERE NOT EXISTS (SELECT 1 FROM tb_system_version);
