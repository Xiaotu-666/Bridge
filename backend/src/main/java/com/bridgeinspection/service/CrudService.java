package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.common.PageResult;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CrudService {
    private final JdbcTemplate jdbcTemplate;
    private final IdService idService;
    private final PasswordEncoder passwordEncoder;
    private final RoutePlanningService routePlanningService;
    private final MatrixService matrixService;
    private final RoleTemplateService roleTemplateService;
    private final Map<String, ResourceConfig> resources;
    private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

    public CrudService(JdbcTemplate jdbcTemplate, IdService idService, PasswordEncoder passwordEncoder,
                       RoutePlanningService routePlanningService, MatrixService matrixService,
                       RoleTemplateService roleTemplateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.idService = idService;
        this.passwordEncoder = passwordEncoder;
        this.routePlanningService = routePlanningService;
        this.matrixService = matrixService;
        this.roleTemplateService = roleTemplateService;
        this.resources = buildResources();
    }

    public PageResult<Map<String, Object>> list(String resource, Map<String, String> params) {
        assertCanRead(resource);
        ResourceConfig config = config(resource);
        Set<String> columns = columns(config);
        int page = parseInt(params.get("page"), 1);
        int size = Math.min(parseInt(params.get("size"), 10), 100);
        int offset = (Math.max(page, 1) - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (columns.contains("is_deleted") && !"true".equalsIgnoreCase(params.get("includeDeleted"))) {
            where.append(" AND is_deleted = 0");
        }

        String keyword = params.get("keyword");
        if ("bridges".equals(resource)) {
            appendBridgeFilters(where, args, params, columns);
            keyword = null;
        }
        if ("reports".equals(resource)) {
            where.append(" AND UPPER(COALESCE(file_format,'PDF'))<>'HTML'");
            appendReportFilters(where, args, params);
        }
        if ("tasks".equals(resource) && "inspector".equals(currentRole())) {
            where.append(" AND EXISTS (SELECT 1 FROM tb_task_assignment assignment WHERE assignment.task_id=tb_inspection_task.task_id AND assignment.user_id=?)");
            args.add(SecurityUtils.currentUserId());
        }
        if ("initial-inspections".equals(resource) && "inspector".equals(currentRole())) {
            where.append(" AND EXISTS (SELECT 1 FROM tb_task_assignment assignment WHERE assignment.task_id=tb_initial_inspection.task_id AND assignment.user_id=?)");
            args.add(SecurityUtils.currentUserId());
        }
        if ("periodic-inspections".equals(resource) && "inspector".equals(currentRole())) {
            where.append(" AND EXISTS (SELECT 1 FROM tb_task_assignment assignment WHERE assignment.task_id=tb_periodic_inspection.task_id AND assignment.user_id=?)");
            args.add(SecurityUtils.currentUserId());
        }
        if (keyword != null && !keyword.isBlank() && !config.searchColumns().isEmpty()) {
            where.append(" AND (");
            for (int i = 0; i < config.searchColumns().size(); i++) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append("`").append(config.searchColumns().get(i)).append("` LIKE ?");
                args.add("%" + keyword.trim() + "%");
            }
            where.append(")");
        }

        params.forEach((key, value) -> {
            if (value == null || value.isBlank() || List.of("page", "size", "keyword", "includeDeleted").contains(key)) {
                return;
            }
            if (columns.contains(key)) {
                where.append(" AND `").append(key).append("` = ?");
                args.add(value);
            }
        });

        String orderColumn = columns.contains("create_time") ? "create_time" : config.idColumn();
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + config.tableName() + "`" + where,
                Long.class,
                args.toArray()
        );
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM `" + config.tableName() + "`" + where
                        + " ORDER BY `" + orderColumn + "` DESC LIMIT ? OFFSET ?",
                listArgs.toArray()
        ).stream().map(row -> enrich(resource, sanitize(row))).toList();
        return new PageResult<>(records, total == null ? 0 : total, page, size);
    }

    public Map<String, Object> get(String resource, String id) {
        assertCanRead(resource);
        ResourceConfig config = config(resource);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM `" + config.tableName() + "` WHERE `" + config.idColumn() + "` = ?",
                id
        );
        if (rows.isEmpty()) {
            throw new BusinessException("记录不存在");
        }
        return sanitize(rows.get(0));
    }

    @Transactional
    public Map<String, Object> create(String resource, Map<String, Object> payload) {
        assertCanWrite(resource);
        ResourceConfig config = config(resource);
        if (config.readOnly()) {
            throw new BusinessException("该资源不允许新增");
        }
        Set<String> columns = columns(config);
        Map<String, Object> values = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (columns.contains(key) && value != null && !List.of("create_time", "update_time").contains(key)) {
                values.put(key, value);
            }
        });
        if ("users".equals(resource)) values.remove("user_id");

        validateSpecialCreate(resource, values);

        if (config.idPrefix() != null && columns.contains(config.idColumn())
                && (values.get(config.idColumn()) == null || String.valueOf(values.get(config.idColumn())).isBlank())) {
            values.put(config.idColumn(), idService.next(config.idPrefix()));
        }
        applyDefaults(config, columns, values, true);
        if (values.containsKey("password")) {
            values.put("password", passwordEncoder.encode(String.valueOf(values.get("password"))));
        }
        if (values.isEmpty()) {
            throw new BusinessException("没有可保存的数据");
        }

        String sql = "INSERT INTO `" + config.tableName() + "` ("
                + String.join(", ", values.keySet().stream().map(c -> "`" + c + "`").toList())
                + ") VALUES ("
                + String.join(", ", values.keySet().stream().map(c -> "?").toList())
                + ")";
        Object id = values.get(config.idColumn());
        if (id == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                int index = 1;
                for (Object value : values.values()) statement.setObject(index++, value);
                return statement;
            }, keyHolder);
            if (keyHolder.getKey() != null) id = keyHolder.getKey();
        } else {
            jdbcTemplate.update(sql, values.values().toArray());
        }
        if ("initial-inspections".equals(resource) && id != null) {
            matrixService.generateInitialItems(String.valueOf(id));
        }
        return id == null ? sanitize(new HashMap<>(values)) : get(resource, String.valueOf(id));
    }

    @Transactional
    public Map<String, Object> update(String resource, String id, Map<String, Object> payload) {
        assertCanWrite(resource);
        ResourceConfig config = config(resource);
        assertRecordUnlocked(resource, id);
        if (config.readOnly()) {
            throw new BusinessException("该资源不允许修改");
        }
        Set<String> columns = columns(config);
        Map<String, Object> values = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (columns.contains(key) && !key.equals(config.idColumn()) && !List.of("create_time", "update_time").contains(key)) {
                values.put(key, value);
            }
        });
        if ("bridge-positions".equals(resource)) {
            values.remove("sort_order");
        }
        if ("users".equals(resource)) validateUserValues(values);
        if ("roles".equals(resource) && values.containsKey("role_desc")) applyRoleTemplate(values);
        if ("routes".equals(resource) && values.containsKey("route_name")) {
            routePlanningService.assertUniqueName(String.valueOf(values.get("route_name")), id);
        }
        if ("reports".equals(resource) && values.containsKey("file_format")
                && !"PDF".equalsIgnoreCase(String.valueOf(values.get("file_format")))) {
            throw new BusinessException("检查报告只允许使用PDF格式");
        }
        if (values.containsKey("password")) {
            String password = String.valueOf(values.get("password"));
            if (password.isBlank()) {
                values.remove("password");
            } else {
                values.put("password", passwordEncoder.encode(password));
            }
        }
        if (values.isEmpty()) {
            return get(resource, id);
        }
        List<Object> args = new ArrayList<>(values.values());
        args.add(id);
        String sql = "UPDATE `" + config.tableName() + "` SET "
                + String.join(", ", values.keySet().stream().map(c -> "`" + c + "` = ?").toList())
                + " WHERE `" + config.idColumn() + "` = ?";
        jdbcTemplate.update(sql, args.toArray());
        return get(resource, id);
    }

    public void delete(String resource, String id) {
        assertCanWrite(resource);
        ResourceConfig config = config(resource);
        assertRecordUnlocked(resource, id);
        if (config.readOnly()) {
            throw new BusinessException("该资源不允许删除");
        }
        Set<String> columns = columns(config);
        if (columns.contains("is_deleted")) {
            jdbcTemplate.update("UPDATE `" + config.tableName() + "` SET is_deleted = 1 WHERE `" + config.idColumn() + "` = ?", id);
        } else {
            jdbcTemplate.update("DELETE FROM `" + config.tableName() + "` WHERE `" + config.idColumn() + "` = ?", id);
        }
    }

    public ResourceConfig config(String resource) {
        ResourceConfig config = resources.get(resource);
        if (config == null) {
            throw new BusinessException("未知资源：" + resource);
        }
        return config;
    }

    private void assertCanWrite(String resource) {
        var user = SecurityUtils.currentUserOrNull();
        String role = user == null || user.roles().isEmpty() ? "" : user.roles().get(0);
        if ("admin".equals(role)) {
            return;
        }
        Set<String> engineerResources = Set.of(
                "routes", "bridges", "bridge-types", "bridge-positions", "bridge-components",
                "bridge-type-components", "bridge-instance-components", "initial-item-definitions",
                "bridge-type-initial-items", "initial-inspections", "initial-inspection-items",
                "periodic-inspections", "component-inspection-records", "tasks", "defect-definitions", "defects", "reports",
                "archive-records", "attachments"
        );
        Set<String> inspectorResources = Set.of(
                "initial-inspections", "initial-inspection-items", "periodic-inspections",
                "component-inspection-records", "defects", "attachments"
        );
        if (("engineer".equals(role) && engineerResources.contains(resource))
                || ("inspector".equals(role) && inspectorResources.contains(resource))) {
            return;
        }
        throw new BusinessException(403, "当前角色没有该数据的维护权限");
    }

    private void assertCanRead(String resource) {
        var user = SecurityUtils.currentUserOrNull();
        String role = user == null || user.roles().isEmpty() ? "" : user.roles().get(0);
        if ("admin".equals(role)) {
            return;
        }
        Set<String> engineerResources = Set.of(
                "routes", "bridges", "bridge-types", "bridge-positions", "bridge-components",
                "bridge-type-components", "bridge-instance-components", "initial-item-definitions",
                "bridge-type-initial-items", "initial-inspections", "initial-inspection-items",
                "periodic-inspections", "component-inspection-records", "tasks", "task-assignments",
                "task-history", "defect-definitions", "defects", "reports", "archive-records", "attachments"
        );
        Set<String> businessResources = Set.of(
                "routes", "bridge-types", "bridge-positions", "bridges", "bridge-instance-components", "initial-inspections", "initial-inspection-items",
                "periodic-inspections", "component-inspection-records", "tasks", "task-assignments",
                "task-history", "defect-definitions", "defects", "reports", "archive-records", "attachments"
        );
        if (("engineer".equals(role) && engineerResources.contains(resource))
                || (Set.of("inspector", "reviewer", "viewer").contains(role) && businessResources.contains(resource))) {
            return;
        }
        throw new BusinessException(403, "当前角色没有该数据的查询权限");
    }

    private String currentRole() {
        var user = SecurityUtils.currentUserOrNull();
        return user == null || user.roles().isEmpty() ? "" : user.roles().get(0);
    }

    private void assertRecordUnlocked(String resource, String id) {
        String sql = switch (resource) {
            case "initial-inspections" -> """
                    SELECT task.task_status FROM tb_initial_inspection record
                    JOIN tb_inspection_task task ON task.task_id=record.task_id
                    WHERE record.initial_inspection_code=?
                    """;
            case "periodic-inspections" -> """
                    SELECT task.task_status FROM tb_periodic_inspection record
                    JOIN tb_inspection_task task ON task.task_id=record.task_id
                    WHERE record.periodic_inspection_code=?
                    """;
            case "initial-inspection-items" -> """
                    SELECT task.task_status FROM tb_initial_inspection_item item
                    JOIN tb_initial_inspection record ON record.initial_inspection_code=item.initial_inspection_code
                    JOIN tb_inspection_task task ON task.task_id=record.task_id
                    WHERE item.item_record_id=?
                    """;
            case "component-inspection-records" -> """
                    SELECT task.task_status FROM tb_component_inspection component
                    JOIN tb_periodic_inspection record ON record.periodic_inspection_code=component.periodic_inspection_code
                    JOIN tb_inspection_task task ON task.task_id=record.task_id
                    WHERE component.component_inspection_id=?
                    """;
            case "defects" -> """
                    SELECT COALESCE(initial_task.task_status,periodic_task.task_status) task_status
                    FROM tb_defect defect
                    LEFT JOIN tb_initial_inspection initial_record ON initial_record.initial_inspection_code=defect.initial_inspection_code
                    LEFT JOIN tb_component_inspection component ON component.component_inspection_id=defect.component_inspection_id
                    LEFT JOIN tb_periodic_inspection periodic_record ON periodic_record.periodic_inspection_code=COALESCE(defect.periodic_inspection_code,component.periodic_inspection_code)
                    LEFT JOIN tb_inspection_task initial_task ON initial_task.task_id=initial_record.task_id
                    LEFT JOIN tb_inspection_task periodic_task ON periodic_task.task_id=periodic_record.task_id
                    WHERE defect.defect_id=?
                    """;
            default -> null;
        };
        if (sql == null) return;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        if (rows.isEmpty()) return;
        String status = String.valueOf(rows.get(0).get("task_status"));
        if (Set.of("已完成", "已审核", "已取消").contains(status)) {
            throw new BusinessException("任务已完成，关联检查记录和病害不能编辑或删除");
        }
    }

    private void applyDefaults(ResourceConfig config, Set<String> columns, Map<String, Object> values, boolean create) {
        if ("tasks".equals(config.resourceName()) && columns.contains("creator_id") && !values.containsKey("creator_id")) {
            values.put("creator_id", SecurityUtils.currentUserId());
        }
        if ("reports".equals(config.resourceName())) {
            values.putIfAbsent("generation_time", LocalDateTime.now());
            values.putIfAbsent("generator_id", SecurityUtils.currentUserId());
            values.putIfAbsent("report_status", "草稿");
            values.putIfAbsent("version_no", "V1.0");
        }
        if ("users".equals(config.resourceName()) && create) {
            values.putIfAbsent("password", "admin123");
            values.putIfAbsent("user_status", 1);
            values.putIfAbsent("force_pwd_change", 1);
        }
        if ("users".equals(config.resourceName())) validateUserValues(values);
        if ("roles".equals(config.resourceName())) applyRoleTemplate(values);
        if ("bridge-positions".equals(config.resourceName()) && create && columns.contains("sort_order")) {
            Integer next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM tb_part", Integer.class);
            values.putIfAbsent("sort_order", next == null ? 1 : next);
        }
        if ("bridges".equals(config.resourceName()) && create) {
            values.putIfAbsent("coordinate_source", "WGS84");
            if (values.containsKey("longitude")) values.putIfAbsent("raw_longitude", values.get("longitude"));
            if (values.containsKey("latitude")) values.putIfAbsent("raw_latitude", values.get("latitude"));
        }
        if ("initial-inspections".equals(config.resourceName()) && create && values.get("bridge_code") != null) {
            String bridgeCode = String.valueOf(values.get("bridge_code"));
            Integer nextVersion = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(version_no),0)+1 FROM tb_initial_inspection WHERE bridge_code=?", Integer.class, bridgeCode);
            jdbcTemplate.update("UPDATE tb_initial_inspection SET effective_flag=0 WHERE bridge_code=?", bridgeCode);
            values.put("version_no", nextVersion == null ? 1 : nextVersion);
            values.put("effective_flag", 1);
        }
        if ("periodic-inspections".equals(config.resourceName()) && create && values.get("bridge_code") != null) {
            String bridgeType = jdbcTemplate.queryForObject("SELECT bridge_type_code FROM tb_bridge WHERE bridge_code=?",
                    String.class, values.get("bridge_code"));
            values.put("form_table_code", periodicTableCode(bridgeType));
        }
    }

    private Set<String> columns(ResourceConfig config) {
        return columnCache.computeIfAbsent(config.tableName(), table -> jdbcTemplate.queryForList("SHOW COLUMNS FROM `" + table + "`")
                .stream()
                .map(row -> String.valueOf(row.get("Field")))
                .collect(java.util.stream.Collectors.toSet()));
    }

    private Map<String, Object> sanitize(Map<String, Object> row) {
        Map<String, Object> clean = new LinkedHashMap<>(row);
        clean.remove("password");
        return clean;
    }

    private Map<String, Object> enrich(String resource, Map<String, Object> row) {
        if ("users".equals(resource) && row.get("role_id") != null) {
            List<Map<String, Object>> role = jdbcTemplate.queryForList(
                    "SELECT role_code,role_name,role_desc FROM tb_role WHERE role_id=?", row.get("role_id"));
            if (!role.isEmpty()) row.putAll(role.get(0));
        }
        if ("roles".equals(resource) && row.get("role_id") != null) {
            Long userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_user WHERE role_id=?", Long.class, row.get("role_id"));
            row.put("user_count", userCount == null ? 0 : userCount);
        }
        if ("bridge-components".equals(resource) && row.get("component_code") != null) {
            List<Map<String, Object>> types = jdbcTemplate.queryForList("""
                    SELECT GROUP_CONCAT(DISTINCT bt.bridge_type_name ORDER BY bt.bridge_type_name SEPARATOR '、') AS bridge_type_names
                    FROM tb_bridge_type_component_config cfg
                    JOIN tb_bridge_type bt ON bt.bridge_type_code = cfg.bridge_type_code
                    WHERE cfg.component_code = ?
                    """, row.get("component_code"));
            if (!types.isEmpty()) row.put("bridge_type_names", types.get(0).get("bridge_type_names"));
        }
        if ("initial-item-definitions".equals(resource) && row.get("item_code") != null) {
            List<Map<String, Object>> types = jdbcTemplate.queryForList("""
                    SELECT GROUP_CONCAT(DISTINCT bt.bridge_type_name ORDER BY bt.bridge_type_name SEPARATOR '、') AS bridge_type_names
                    FROM tb_bridge_type_initial_item_config cfg
                    JOIN tb_bridge_type bt ON bt.bridge_type_code=cfg.bridge_type_code
                    WHERE cfg.item_code=?
                    """, row.get("item_code"));
            if (!types.isEmpty()) row.put("bridge_type_names", types.get(0).get("bridge_type_names"));
        }
        if ("initial-inspections".equals(resource) && row.get("bridge_code") != null) {
            enrichBridge(row, row.get("bridge_code"));
            enrichTaskStatus(row);
        }
        if ("periodic-inspections".equals(resource) && row.get("bridge_code") != null) {
            enrichBridge(row, row.get("bridge_code"));
            enrichTaskStatus(row);
        }
        if ("initial-inspection-items".equals(resource) && row.get("initial_inspection_code") != null) {
            List<Map<String, Object>> details = jdbcTemplate.queryForList("""
                    SELECT i.bridge_code,b.bridge_name,i.inspection_date,d.item_name,d.unit,d.item_category
                    FROM tb_initial_inspection i JOIN tb_bridge b ON b.bridge_code=i.bridge_code
                    JOIN tb_initial_inspection_item_definition d ON d.item_code=?
                    WHERE i.initial_inspection_code=?
                    """, row.get("item_code"), row.get("initial_inspection_code"));
            if (!details.isEmpty()) row.putAll(details.get(0));
        }
        if ("component-inspection-records".equals(resource) && row.get("periodic_inspection_code") != null) {
            List<Map<String, Object>> details = jdbcTemplate.queryForList("""
                    SELECT p.bridge_code,b.bridge_name,p.inspection_date,part.part_name,component.component_name
                    FROM tb_periodic_inspection p JOIN tb_bridge b ON b.bridge_code=p.bridge_code
                    LEFT JOIN tb_part part ON part.part_code=? LEFT JOIN tb_component component ON component.component_code=?
                    WHERE p.periodic_inspection_code=?
                    """, row.get("part_code"), row.get("component_code"), row.get("periodic_inspection_code"));
            if (!details.isEmpty()) row.putAll(details.get(0));
        }
        if ("defects".equals(resource) && row.get("bridge_code") != null) {
            enrichBridge(row, row.get("bridge_code"));
            row.put("inspection_type", row.get("initial_inspection_code") != null ? "initial" : "periodic");
            if (row.get("periodic_inspection_code") == null && row.get("component_inspection_id") != null) {
                List<Map<String, Object>> codes = jdbcTemplate.queryForList(
                        "SELECT periodic_inspection_code FROM tb_component_inspection WHERE component_inspection_id=?",
                        row.get("component_inspection_id"));
                if (!codes.isEmpty()) row.put("periodic_inspection_code", codes.get(0).get("periodic_inspection_code"));
            }
        }
        if ("reports".equals(resource) && row.get("report_id") != null) {
            List<Map<String, Object>> details = jdbcTemplate.queryForList("""
                    SELECT COALESCE(t.bridge_code,i.bridge_code,p.bridge_code) bridge_code,
                           b.bridge_name,bt.bridge_type_name,
                           COALESCE(r.initial_inspection_code,r.periodic_inspection_code) inspection_code
                    FROM tb_report r LEFT JOIN tb_inspection_task t ON t.task_id=r.task_id
                    LEFT JOIN tb_initial_inspection i ON i.initial_inspection_code=r.initial_inspection_code
                    LEFT JOIN tb_periodic_inspection p ON p.periodic_inspection_code=r.periodic_inspection_code
                    LEFT JOIN tb_bridge b ON b.bridge_code=COALESCE(t.bridge_code,i.bridge_code,p.bridge_code)
                    LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                    WHERE r.report_id=?
                    """, row.get("report_id"));
            if (!details.isEmpty()) row.putAll(details.get(0));
        }
        return row;
    }

    private void enrichBridge(Map<String, Object> row, Object bridgeCode) {
        List<Map<String, Object>> bridges = jdbcTemplate.queryForList("""
                SELECT b.bridge_name,b.bridge_type_code,bt.bridge_type_name
                FROM tb_bridge b LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code
                WHERE b.bridge_code=?
                """, bridgeCode);
        if (!bridges.isEmpty()) row.putAll(bridges.get(0));
    }

    private void enrichTaskStatus(Map<String, Object> row) {
        if (row.get("task_id") == null) return;
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT task_status FROM tb_inspection_task WHERE task_id=?", row.get("task_id"));
        if (!tasks.isEmpty()) row.put("task_status", tasks.get(0).get("task_status"));
    }

    private void appendBridgeFilters(StringBuilder where, List<Object> args, Map<String, String> params, Set<String> columns) {
        appendExact(where, args, columns, "route_code", params.get("routeCode"));
        appendExact(where, args, columns, "administrative_code", params.get("administrativeCode"));
        appendExact(where, args, columns, "management_unit", params.get("managementUnit"));
        appendLike(where, args, columns, "bridge_name", params.get("bridgeName"));
        appendLike(where, args, columns, "bridge_code", params.get("bridgeCode"));
        String types = params.get("bridgeTypeCodes");
        if (types != null && !types.isBlank() && columns.contains("bridge_type_code")) {
            List<String> values = java.util.Arrays.stream(types.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
            if (!values.isEmpty()) {
                where.append(" AND bridge_type_code IN (");
                where.append(String.join(",", java.util.Collections.nCopies(values.size(), "?")));
                where.append(")");
                args.addAll(values);
            }
        }
        String from = params.get("builtYearFrom"), to = params.get("builtYearTo");
        if (from != null && !from.isBlank() && columns.contains("built_year")) { where.append(" AND built_year >= ?"); args.add(from); }
        if (to != null && !to.isBlank() && columns.contains("built_year")) { where.append(" AND built_year <= ?"); args.add(to); }
    }

    private void appendReportFilters(StringBuilder where, List<Object> args, Map<String, String> params) {
        String bridgeCode = params.get("bridgeCode");
        if (bridgeCode != null && !bridgeCode.isBlank()) {
            where.append("""
                     AND (task_id IN (SELECT task_id FROM tb_inspection_task WHERE bridge_code=?)
                       OR initial_inspection_code IN (SELECT initial_inspection_code FROM tb_initial_inspection WHERE bridge_code=?)
                       OR periodic_inspection_code IN (SELECT periodic_inspection_code FROM tb_periodic_inspection WHERE bridge_code=?))
                    """);
            args.add(bridgeCode.trim()); args.add(bridgeCode.trim()); args.add(bridgeCode.trim());
        }
    }

    private void appendExact(StringBuilder where, List<Object> args, Set<String> columns, String column, String value) {
        if (value != null && !value.isBlank() && columns.contains(column)) {
            where.append(" AND `" + column + "` = ?"); args.add(value.trim());
        }
    }

    private void appendLike(StringBuilder where, List<Object> args, Set<String> columns, String column, String value) {
        if (value != null && !value.isBlank() && columns.contains(column)) {
            where.append(" AND `" + column + "` LIKE ?"); args.add("%" + value.trim() + "%");
        }
    }

    private void validateSpecialCreate(String resource, Map<String, Object> values) {
        if ("routes".equals(resource)) {
            routePlanningService.assertUniqueName(String.valueOf(values.getOrDefault("route_name", "")), null);
        }
        if ("bridge-positions".equals(resource) && values.containsKey("sort_order")) {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_part WHERE sort_order = ?", Integer.class, values.get("sort_order"));
            if (count != null && count > 0) throw new com.bridgeinspection.common.BusinessException("部位顺序已存在");
        }
        if ("reports".equals(resource) && values.containsKey("file_format")
                && !"PDF".equalsIgnoreCase(String.valueOf(values.get("file_format")))) {
            throw new BusinessException("检查报告只允许使用PDF格式");
        }
        if ("users".equals(resource)) validateUserValues(values);
        if ("roles".equals(resource)) applyRoleTemplate(values);
    }

    private void validateUserValues(Map<String, Object> values) {
        if (values.containsKey("role_id")) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_role WHERE role_id=?", Integer.class, values.get("role_id"));
            if (count == null || count == 0) throw new BusinessException("所选角色不存在");
        }
        normalizeBinary(values, "user_status", 1);
        normalizeBinary(values, "force_pwd_change", 1);
    }

    private void normalizeBinary(Map<String, Object> values, String field, int defaultValue) {
        if (!values.containsKey(field)) return;
        String raw = String.valueOf(values.get(field));
        if (raw.isBlank()) {
            values.put(field, defaultValue);
            return;
        }
        if (!"0".equals(raw) && !"1".equals(raw)) throw new BusinessException(field + " 只能选择启用或停用");
        values.put(field, Integer.parseInt(raw));
    }

    private void applyRoleTemplate(Map<String, Object> values) {
        Object description = values.get("role_desc");
        if (description == null || String.valueOf(description).isBlank()) {
            throw new BusinessException("请选择角色说明");
        }
        values.put("permission_set", roleTemplateService.permissionJson(String.valueOf(description)));
    }

    private String periodicTableCode(String bridgeType) {
        return switch (bridgeType == null ? "" : bridgeType) {
            case "beam" -> "C-1"; case "arch" -> "C-2"; case "rigid_arch" -> "C-3";
            case "composite_arch" -> "C-4"; case "cable_stayed" -> "C-5"; case "suspension" -> "C-6";
            default -> "C-7";
        };
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Map<String, ResourceConfig> buildResources() {
        List<ResourceConfig> list = List.of(
                new ResourceConfig("users", "tb_user", "user_id", null, List.of("user_name", "login_account", "department", "phone"), false),
                new ResourceConfig("roles", "tb_role", "role_id", null, List.of("role_code", "role_name"), false),
                new ResourceConfig("logs", "tb_operation_log", "log_id", null, List.of("user_name", "module", "operation_type"), true),
                new ResourceConfig("routes", "tb_route", "route_code", null, List.of("route_code", "route_name"), false),
                new ResourceConfig("bridges", "tb_bridge", "bridge_code", null, List.of("bridge_code", "bridge_name", "route_code", "management_unit"), false),
                new ResourceConfig("bridge-types", "tb_bridge_type", "bridge_type_code", null, List.of("bridge_type_code", "bridge_type_name"), false),
                new ResourceConfig("bridge-positions", "tb_part", "part_code", null, List.of("part_code", "part_name"), false),
                new ResourceConfig("bridge-components", "tb_component", "component_code", null, List.of("component_code", "component_name"), false),
                new ResourceConfig("bridge-type-components", "tb_bridge_type_component_config", "config_id", null, List.of("bridge_type_code", "part_code", "component_code"), false),
                new ResourceConfig("bridge-instance-components", "tb_bridge_specific_component", "bridge_component_id", null, List.of("bridge_code", "component_code", "component_serial", "location_desc"), false),
                new ResourceConfig("initial-item-definitions", "tb_initial_inspection_item_definition", "item_code", null, List.of("item_code", "item_name", "item_category"), false),
                new ResourceConfig("bridge-type-initial-items", "tb_bridge_type_initial_item_config", "config_id", null, List.of("bridge_type_code", "item_code", "requirement_type"), false),
                new ResourceConfig("initial-inspections", "tb_initial_inspection", "initial_inspection_code", "QI", List.of("initial_inspection_code", "bridge_code", "inspection_org", "status"), false),
                new ResourceConfig("initial-inspection-items", "tb_initial_inspection_item", "item_record_id", null, List.of("initial_inspection_code", "item_code"), false),
                new ResourceConfig("periodic-inspections", "tb_periodic_inspection", "periodic_inspection_code", "QP", List.of("periodic_inspection_code", "bridge_code", "status"), false),
                new ResourceConfig("component-inspection-records", "tb_component_inspection", "component_inspection_id", null, List.of("periodic_inspection_code", "part_code", "component_code"), false),
                new ResourceConfig("tasks", "tb_inspection_task", "task_id", "JC", List.of("task_id", "bridge_code", "inspection_type", "task_status"), false),
                new ResourceConfig("task-assignments", "tb_task_assignment", "assignment_id", "ASG", List.of("task_id", "user_id"), false),
                new ResourceConfig("task-history", "tb_task_status_history", "history_id", "HIS", List.of("task_id", "to_status"), true),
                new ResourceConfig("defect-definitions", "tb_defect_definition", "defect_definition_code", null, List.of("defect_definition_code", "defect_name", "defect_nature", "description"), false),
                new ResourceConfig("defects", "tb_defect", "defect_id", null, List.of("bridge_code", "defect_type", "defect_nature", "description"), false),
                new ResourceConfig("reports", "tb_report", "report_id", "REP", List.of("task_id", "report_type", "version_no", "report_status"), false),
                new ResourceConfig("archive-records", "tb_bridge_archive_record", "archive_record_id", null, List.of("bridge_code", "archive_item_code", "completeness_status"), false),
                new ResourceConfig("attachments", "tb_attachment", "file_id", null, List.of("file_name", "file_type", "file_description"), false),
                new ResourceConfig("backups", "tb_backup_record", "backup_id", null, List.of("file_name", "backup_status"), true)
        );
        Map<String, ResourceConfig> map = new LinkedHashMap<>();
        for (ResourceConfig config : list) {
            map.put(config.resourceName(), config);
        }
        return map;
    }

    public record ResourceConfig(
            String resourceName,
            String tableName,
            String idColumn,
            String idPrefix,
            List<String> searchColumns,
            boolean readOnly
    ) {
    }
}
