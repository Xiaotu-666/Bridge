package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.AuthenticatedUser;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {
    private final JdbcTemplate jdbcTemplate;
    private final IdService idService;

    public TaskService(JdbcTemplate jdbcTemplate, IdService idService) {
        this.jdbcTemplate = jdbcTemplate;
        this.idService = idService;
    }

    @Transactional
    public void assign(String taskId, List<String> userIds) {
        ensureTask(taskId);
        for (String userId : userIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_task_assignment WHERE task_id=? AND user_id=?",
                    Integer.class, taskId, userId);
            if (count != null && count > 0) continue;
            jdbcTemplate.update("""
                    INSERT INTO tb_task_assignment (assignment_id,task_id,user_id)
                    VALUES (?,?,?)
                    """, idService.next("ASG"), taskId, userId);
        }
        addHistory(taskId, status(taskId), status(taskId), "分配检查人员");
    }

    @Transactional
    public void accept(String taskId) {
        if (List.of("已完成", "已审核", "已取消").contains(status(taskId))) {
            throw new BusinessException("已完成、已审核或已取消的任务不能重新接受");
        }
        ensureCurrentInspectorAssignment(taskId, true);
        changeStatus(taskId, "进行中", "检查人员接受任务", Map.of("actual_start_date", LocalDate.now()));
        jdbcTemplate.update("""
                UPDATE tb_task_assignment SET assignment_status='进行中',
                    accept_time=COALESCE(accept_time,CURRENT_TIMESTAMP)
                WHERE task_id=? AND user_id=?
                """, taskId, SecurityUtils.currentUserId());
    }

    @Transactional
    public void complete(String taskId) {
        if (!"进行中".equals(status(taskId))) {
            throw new BusinessException("只有进行中的任务可以完成");
        }
        ensureCurrentInspectorAssignment(taskId, false);
        changeStatus(taskId, "已完成", "检查数据提交完成", Map.of("actual_end_date", LocalDate.now()));
        jdbcTemplate.update("""
                UPDATE tb_task_assignment SET assignment_status='已完成',complete_time=CURRENT_TIMESTAMP
                WHERE task_id=? AND user_id=?
                """, taskId, SecurityUtils.currentUserId());
    }

    @Transactional
    public void review(String taskId, String opinion) {
        changeStatus(taskId, "已审核", opinion == null || opinion.isBlank() ? "审核通过" : opinion, Map.of());
    }

    @Transactional
    public void reject(String taskId, String reason) {
        changeStatus(taskId, "进行中", reason == null || reason.isBlank() ? "审核驳回" : reason, Map.of());
    }

    @Transactional
    public void cancel(String taskId, String reason) {
        String from = status(taskId);
        if (!List.of("待分配", "进行中").contains(from)) {
            throw new BusinessException("只有待分配或进行中的任务可以取消");
        }
        jdbcTemplate.update("""
                UPDATE tb_inspection_task
                SET task_status='已取消',cancel_reason=?,update_time=CURRENT_TIMESTAMP
                WHERE task_id=?
                """, reason, taskId);
        addHistory(taskId, from, "已取消", reason == null || reason.isBlank() ? "取消任务" : reason);
    }

    private void changeStatus(String taskId, String toStatus, String reason, Map<String, Object> extra) {
        String from = status(taskId);
        StringBuilder sql = new StringBuilder("UPDATE tb_inspection_task SET task_status=?,update_time=CURRENT_TIMESTAMP");
        List<Object> args = new ArrayList<>();
        args.add(toStatus);
        extra.forEach((key, value) -> {
            sql.append(", `").append(key).append("`=?");
            args.add(value);
        });
        sql.append(" WHERE task_id=?");
        args.add(taskId);
        jdbcTemplate.update(sql.toString(), args.toArray());
        addHistory(taskId, from, toStatus, reason);
    }

    private String status(String taskId) {
        ensureTask(taskId);
        return jdbcTemplate.queryForObject(
                "SELECT task_status FROM tb_inspection_task WHERE task_id=?", String.class, taskId);
    }

    private void ensureTask(String taskId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_inspection_task WHERE task_id=?", Integer.class, taskId);
        if (count == null || count == 0) throw new BusinessException("检查任务不存在");
    }

    private void ensureCurrentInspectorAssignment(String taskId, boolean createWhenMissing) {
        AuthenticatedUser user = SecurityUtils.currentUserOrNull();
        if (user == null || !user.roles().contains("inspector")) return;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_task_assignment WHERE task_id=? AND user_id=?",
                Integer.class, taskId, user.userId());
        if (count != null && count > 0) return;
        if (!createWhenMissing) throw new BusinessException(403, "该任务未分配给当前检查人员");
        jdbcTemplate.update("""
                INSERT INTO tb_task_assignment
                (assignment_id,task_id,user_id,assignment_status,accept_time)
                VALUES (?,?,?,?,CURRENT_TIMESTAMP)
                """, idService.next("ASG"), taskId, user.userId(), "进行中");
    }

    private void addHistory(String taskId, String from, String to, String reason) {
        jdbcTemplate.update("""
                INSERT INTO tb_task_status_history
                (history_id,task_id,from_status,to_status,opinion,operator_id)
                VALUES (?,?,?,?,?,?)
                """, idService.next("HIS"), taskId, from, to, reason, SecurityUtils.currentUserId());
    }
}
