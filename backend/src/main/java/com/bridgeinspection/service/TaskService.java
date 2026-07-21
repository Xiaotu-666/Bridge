package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.AuthenticatedUser;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @Scheduled(cron = "${app.tasks.periodic-auto-create-cron:0 5 0 * * *}", zone = "Asia/Shanghai")
    @Transactional
    public synchronized Map<String, Object> generateUpcomingPeriodicTasks() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusMonths(1);
        List<Map<String, Object>> admins = jdbcTemplate.queryForList("""
                SELECT u.user_id FROM tb_user u JOIN tb_role r ON r.role_id=u.role_id
                WHERE r.role_code='admin' AND u.user_status=1 ORDER BY u.user_id LIMIT 1
                """);
        if (admins.isEmpty()) return Map.of("created", 0, "candidate_count", 0, "message", "未找到可作为任务创建人的系统管理员");
        int creatorId = ((Number) admins.get(0).get("user_id")).intValue();
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
                SELECT p.bridge_code,p.periodic_inspection_code,p.next_inspection_date
                FROM tb_periodic_inspection p
                WHERE p.next_inspection_date IS NOT NULL AND p.next_inspection_date<=?
                  AND NOT EXISTS (
                    SELECT 1 FROM tb_periodic_inspection newer
                    WHERE newer.bridge_code=p.bridge_code
                      AND (newer.inspection_date>p.inspection_date
                        OR (newer.inspection_date=p.inspection_date AND newer.periodic_inspection_code>p.periodic_inspection_code))
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM tb_inspection_task t
                    WHERE t.bridge_code=p.bridge_code AND t.inspection_type='periodic'
                      AND t.task_status IN ('待分配','进行中')
                  )
                UNION ALL
                SELECT i.bridge_code,NULL AS periodic_inspection_code,i.next_inspection_date
                FROM tb_initial_inspection i
                WHERE i.effective_flag=1 AND i.next_inspection_date IS NOT NULL AND i.next_inspection_date<=?
                  AND NOT EXISTS (
                    SELECT 1 FROM tb_periodic_inspection p WHERE p.bridge_code=i.bridge_code
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM tb_inspection_task t
                    WHERE t.bridge_code=i.bridge_code AND t.inspection_type='periodic'
                      AND t.task_status IN ('待分配','进行中')
                  )
                ORDER BY next_inspection_date,bridge_code
                """, cutoff, cutoff);
        int created = 0;
        for (Map<String, Object> candidate : candidates) {
            LocalDate dueDate = LocalDate.parse(String.valueOf(candidate.get("next_inspection_date")));
            LocalDate startDate = dueDate.minusMonths(1).isBefore(today) ? today : dueDate.minusMonths(1);
            String taskId = idService.next("JC");
            String bridgeCode = String.valueOf(candidate.get("bridge_code"));
            String sourceRecord = String.valueOf(candidate.get("periodic_inspection_code"));
            String remark = sourceRecord == null || "null".equals(sourceRecord)
                    ? "系统根据初始检查的下次检查日期自动生成首次定期检查任务"
                    : "系统根据上次定期检查 " + sourceRecord + " 的下次检查日期自动生成";
            jdbcTemplate.update("""
                    INSERT INTO tb_inspection_task
                    (task_id,bridge_code,inspection_type,inspection_level,plan_start_date,plan_end_date,task_status,remarks,creator_id)
                    VALUES (?,?, 'periodic','Ⅰ',?,?, '待分配',?,?)
                    """, taskId, bridgeCode, startDate, dueDate, remark, creatorId);
            jdbcTemplate.update("""
                    INSERT INTO tb_task_status_history
                    (history_id,task_id,from_status,to_status,opinion,operator_id)
                    VALUES (?,?,NULL,'待分配',?,?)
                    """, idService.next("HIS"), taskId, "定期检查任务自动创建", creatorId);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("candidate_count", candidates.size());
        result.put("cutoff_date", cutoff);
        result.put("message", created == 0 ? "未来一个月内没有需要新建的定期检查任务" : "已自动生成待分配的定期检查任务");
        return result;
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
