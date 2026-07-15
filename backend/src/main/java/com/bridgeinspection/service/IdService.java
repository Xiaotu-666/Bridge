package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IdService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Map<String, IdTarget> TARGETS = Map.of(
            "JC", new IdTarget("tb_inspection_task", "task_id"),
            "ASG", new IdTarget("tb_task_assignment", "assignment_id"),
            "HIS", new IdTarget("tb_task_status_history", "history_id"),
            "QI", new IdTarget("tb_initial_inspection", "initial_inspection_code"),
            "QP", new IdTarget("tb_periodic_inspection", "periodic_inspection_code"),
            "REP", new IdTarget("tb_report", "report_id")
    );

    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public IdService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next(String prefix) {
        String date = LocalDate.now().format(DATE_FORMAT);
        String key = prefix + date;
        AtomicInteger counter = counters.computeIfAbsent(key,
                ignored -> new AtomicInteger(loadCurrentSequence(prefix, date)));
        int sequence = counter.incrementAndGet();
        if (sequence > 9999) {
            throw new BusinessException("当天" + prefix + "编号已超过9999条");
        }
        return key + String.format(Locale.ROOT, "%04d", sequence);
    }

    public String uuid(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private int loadCurrentSequence(String prefix, String date) {
        IdTarget target = TARGETS.get(prefix);
        if (target == null) return 0;
        String stem = prefix + date;
        String sql = "SELECT COALESCE(MAX(CAST(RIGHT(`" + target.idColumn()
                + "`, 4) AS UNSIGNED)), 0) FROM `" + target.tableName()
                + "` WHERE `" + target.idColumn() + "` REGEXP ?";
        Integer current = jdbcTemplate.queryForObject(sql, Integer.class,
                "^" + stem + "[0-9]{4}$");
        return current == null ? 0 : current;
    }

    private record IdTarget(String tableName, String idColumn) { }
}
