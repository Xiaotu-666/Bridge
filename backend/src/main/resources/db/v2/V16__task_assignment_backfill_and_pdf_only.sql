SET NAMES utf8mb4;

INSERT IGNORE INTO tb_task_assignment
    (assignment_id, task_id, user_id, assignment_status, assign_time, accept_time)
SELECT CONCAT('ASG-BF-', history.history_id), history.task_id, history.operator_id,
       '进行中', history.operation_time, history.operation_time
FROM tb_task_status_history history
JOIN tb_user user_record ON user_record.user_id=history.operator_id
JOIN tb_role role_record ON role_record.role_id=user_record.role_id AND role_record.role_code='inspector'
WHERE history.to_status='进行中'
  AND history.opinion LIKE '%接受%'
  AND NOT EXISTS (
      SELECT 1 FROM tb_task_assignment assignment
      WHERE assignment.task_id=history.task_id AND assignment.user_id=history.operator_id
  );

DELETE FROM tb_report WHERE UPPER(COALESCE(file_format, ''))='HTML';
