DELETE a FROM tb_attachment a
JOIN tb_bridge_archive_record r ON r.archive_record_id = a.archive_record_id
WHERE r.archive_item_code = 'maintenance_file';
DELETE FROM tb_bridge_archive_record WHERE archive_item_code = 'maintenance_file';
DELETE FROM tb_archive_item WHERE archive_item_code = 'maintenance_file';
DROP TABLE IF EXISTS tb_maintenance_record;
DROP TABLE IF EXISTS tb_treatment_category;
