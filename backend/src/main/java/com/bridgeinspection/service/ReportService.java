package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.SecurityUtils;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;
    private final IdService idService;
    private final Path reportDir;
    private final Path uploadDir;
    private final Path fontPath;

    public ReportService(JdbcTemplate jdbcTemplate, IdService idService,
                         @Value("${app.files.report-dir}") String reportDir,
                         @Value("${app.files.upload-dir}") String uploadDir,
                         @Value("${app.files.pdf-font-path:C:/Windows/Fonts/simfang.ttf}") String fontPath) throws IOException {
        this.jdbcTemplate = jdbcTemplate;
        this.idService = idService;
        this.reportDir = Path.of(reportDir).toAbsolutePath().normalize();
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.fontPath = Path.of(fontPath).toAbsolutePath().normalize();
        Files.createDirectories(this.reportDir);
    }

    @Transactional
    public Map<String, Object> generate(String taskId, String reportType, String changeSummary) {
        Map<String, Object> task = queryOne("SELECT * FROM tb_inspection_task WHERE task_id = ?", taskId);
        String bridgeCode = String.valueOf(task.get("bridge_code"));
        Map<String, Object> bridge = bridge(bridgeCode);
        String normalized = normalizeType(reportType, String.valueOf(task.get("inspection_type")));
        InspectionContext context = inspectionContext(normalized, taskId, bridgeCode, bridge);
        return createPdfRecord(taskId, normalized, changeSummary, bridge, context);
    }

    @Transactional
    public Map<String, Object> generateBridgeCard(String bridgeCode) {
        Map<String, Object> bridge = bridge(bridgeCode);
        return createPdfRecord(null, "bridge_card", "生成桥梁基本状况卡片", bridge,
                new InspectionContext(null, null, "A", null, List.of()));
    }

    @Transactional
    public Map<String, Object> generateInitialRecord(String bridgeCode) {
        Map<String, Object> bridge = bridge(bridgeCode);
        Map<String, Object> record = queryOne(
                "SELECT * FROM tb_initial_inspection WHERE bridge_code=? ORDER BY effective_flag DESC,inspection_date DESC LIMIT 1", bridgeCode);
        if (record == null) throw new BusinessException("该桥梁没有初始检查记录");
        String code = String.valueOf(record.get("initial_inspection_code"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.item_category,d.item_name,i.measured_value,d.unit,i.inspection_description
                FROM tb_initial_inspection_item i JOIN tb_initial_inspection_item_definition d ON d.item_code=i.item_code
                WHERE i.initial_inspection_code=? ORDER BY d.item_category,d.item_code
                """, code);
        return createPdfRecord(null, "initial_record", "生成桥梁初始检查记录表", bridge,
                new InspectionContext(code, null, "B", record, rows));
    }

    @Transactional
    public Map<String, Object> generatePeriodicRecord(String bridgeCode, String periodicCode) {
        Map<String, Object> bridge = bridge(bridgeCode);
        Map<String, Object> record = queryOne(
                "SELECT p.*,r.rating_level_name FROM tb_periodic_inspection p LEFT JOIN tb_rating_level r ON r.rating_level_code=p.rating_level_code WHERE p.periodic_inspection_code=?", periodicCode);
        if (record == null) throw new BusinessException("定期检查记录不存在");
        String tableCode = tableCode(String.valueOf(bridge.get("bridge_type_code")));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.part_name,c.component_name,ci.score,ci.defect_type,ci.defect_location,ci.defect_range,
                       ci.worst_component,ci.maintenance_advice,ci.special_check_required
                FROM tb_bridge_type_component_config cfg
                JOIN tb_part p ON p.part_code=cfg.part_code JOIN tb_component c ON c.component_code=cfg.component_code
                LEFT JOIN tb_component_inspection ci ON ci.periodic_inspection_code=? AND ci.component_code=cfg.component_code
                WHERE cfg.bridge_type_code=? AND cfg.active_flag=1 ORDER BY p.sort_order,cfg.display_order
                """, periodicCode, bridge.get("bridge_type_code"));
        return createPdfRecord(null, "periodic_record", "生成桥梁定期检查记录表", bridge,
                new InspectionContext(null, periodicCode, tableCode, record, rows));
    }

@Transactional
    public Map<String, Object> generateSummaryRecord(String bridgeCode) {
        Map<String, Object> bridge = bridge(bridgeCode);
        return createPdfRecord(null, "bridge_summary", "生成桥梁检查趋势与对比", bridge,
                new InspectionContext(null, null, "Σ", null, List.of()));
    }

    private String summaryDocument(Map<String, Object> bridge) {
        String bridgeCode = String.valueOf(bridge.get("bridge_code"));
        List<Map<String, Object>> initials = jdbcTemplate.queryForList(
                "SELECT inspection_date,initial_inspection_code,version_no,effective_flag FROM tb_initial_inspection WHERE bridge_code=? ORDER BY effective_flag DESC,inspection_date DESC", bridgeCode);
        List<Map<String, Object>> periodics = jdbcTemplate.queryForList(
                "SELECT p.inspection_date,p.periodic_inspection_code,rl.rating_level_name FROM tb_periodic_inspection p LEFT JOIN tb_rating_level rl ON rl.rating_level_code=p.rating_level_code WHERE p.bridge_code=? ORDER BY p.inspection_date DESC", bridgeCode);
        StringBuilder html = new StringBuilder(officialHead("桥梁检查趋势与对比", bridge));
        html.append("<table class='info-table' style='margin:12px 0'><tbody><tr><td class='label'>初始检查次数</td><td>").append(initials.size()).append("</td><td class='label'>定期检查次数</td><td>").append(periodics.size()).append("</td></tr></tbody></table>");
        html.append("<div class='card-section'><div class='section'>1 历次初始检查</div><table class='data-table'><thead><tr><th>检查日期</th><th>初始检查编号</th><th>版本</th><th>有效状态</th></tr></thead><tbody>");
        if (initials.isEmpty()) html.append("<tr><td class='empty' colspan='4'>暂无记录</td></tr>");
        else for (Map<String, Object> row : initials) html.append("<tr><td>").append(esc(row.get("inspection_date"))).append("</td><td>").append(esc(row.get("initial_inspection_code"))).append("</td><td>").append(esc(row.get("version_no"))).append("</td><td>").append(esc(Objects.equals(1, row.get("effective_flag")) ? "当前有效" : "历史版本")).append("</td></tr>");
        html.append("</tbody></table></div>");
        html.append("<div class='card-section'><div class='section'>2 历次定期检查</div><table class='data-table'><thead><tr><th>检查日期</th><th>定期检查编号</th><th>技术状况等级</th></tr></thead><tbody>");
        if (periodics.isEmpty()) html.append("<tr><td class='empty' colspan='3'>暂无记录</td></tr>");
        else for (Map<String, Object> row : periodics) html.append("<tr><td>").append(esc(row.get("inspection_date"))).append("</td><td>").append(esc(row.get("periodic_inspection_code"))).append("</td><td>").append(esc(row.get("rating_level_name") != null ? row.get("rating_level_name") : "—")).append("</td></tr>");
        html.append("</tbody></table></div>");
        return html.toString();
    }

    public Resource loadReportFile(String reportId) {
        Map<String, Object> report = queryOne("SELECT * FROM tb_report WHERE report_id = ?", reportId);
        Object filePath = report.get("file_path");
        if (filePath == null || String.valueOf(filePath).isBlank()) throw new BusinessException("报告文件不存在");
        try {
            Path target = reportDir.resolve(String.valueOf(filePath)).normalize();
            if (!target.startsWith(reportDir) || !Files.exists(target)) throw new BusinessException("报告文件不存在");
            return new UrlResource(target.toUri());
        } catch (Exception ex) {
            if (ex instanceof BusinessException business) throw business;
            throw new BusinessException("报告文件读取失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> createPdfRecord(String taskId, String reportType, String summary,
                                                 Map<String, Object> bridge, InspectionContext context) {
        String reportId = idService.next("REP");
        String version = nextVersion(taskId, reportType);
        String fileName = reportId + ".pdf";
        Path file = reportDir.resolve(fileName);
        String html = buildDocument(reportId, version, reportType, bridge, context);
        renderPdf(html, file);
        jdbcTemplate.update("""
                INSERT INTO tb_report
                (report_id,bridge_code,task_id,initial_inspection_code,periodic_inspection_code,report_type,version_no,
                 file_format,file_path,report_status,generation_time,generator_id,change_summary)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, reportId, bridge.get("bridge_code"), taskId, context.initialCode(), context.periodicCode(), reportType, version,
                "PDF", fileName, "草稿", LocalDateTime.now(), SecurityUtils.currentUserId(), summary);
        return queryOne("SELECT * FROM tb_report WHERE report_id=?", reportId);
    }

    private InspectionContext inspectionContext(String type, String taskId, String bridgeCode, Map<String, Object> bridge) {
        if ("initial_record".equals(type)) {
            Map<String, Object> record = queryOne("""
                    SELECT * FROM tb_initial_inspection WHERE task_id=? OR (task_id IS NULL AND bridge_code=?)
                    ORDER BY (task_id=? ) DESC,effective_flag DESC,inspection_date DESC,version_no DESC LIMIT 1
                    """, taskId, bridgeCode, taskId);
            String code = String.valueOf(record.get("initial_inspection_code"));
            String formNo = string(record.get("record_form_no"));
            if (formNo.isBlank()) {
                formNo = "B-" + compact(record.get("inspection_date")) + "-" + bridgeCode + "-" + String.format("%02d", record.getOrDefault("version_no", 1));
                jdbcTemplate.update("UPDATE tb_initial_inspection SET record_form_no=? WHERE initial_inspection_code=?", formNo, code);
                record.put("record_form_no", formNo);
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT d.item_category,d.item_name,i.measured_value,d.unit,i.inspection_description
                    FROM tb_initial_inspection_item i JOIN tb_initial_inspection_item_definition d ON d.item_code=i.item_code
                    WHERE i.initial_inspection_code=? ORDER BY d.item_category,d.item_code
                    """, code);
            return new InspectionContext(code, null, "B", record, rows);
        }
        if ("periodic_record".equals(type)) {
            Map<String, Object> record = queryOne("""
                    SELECT p.*,r.rating_level_name FROM tb_periodic_inspection p
                    LEFT JOIN tb_rating_level r ON r.rating_level_code=p.rating_level_code
                    WHERE p.task_id=? OR (p.task_id IS NULL AND p.bridge_code=?)
                    ORDER BY (p.task_id=? ) DESC,p.inspection_date DESC LIMIT 1
                    """, taskId, bridgeCode, taskId);
            String code = String.valueOf(record.get("periodic_inspection_code"));
            String tableCode = tableCode(String.valueOf(bridge.get("bridge_type_code")));
            String formNo = string(record.get("record_form_no"));
            if (formNo.isBlank()) {
                formNo = tableCode + "-" + compact(record.get("inspection_date")) + "-" + bridgeCode + "-01";
                jdbcTemplate.update("UPDATE tb_periodic_inspection SET form_table_code=?,record_form_no=? WHERE periodic_inspection_code=?",
                        tableCode, formNo, code);
                record.put("record_form_no", formNo);
                record.put("form_table_code", tableCode);
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT p.part_name,c.component_name,ci.score,ci.defect_type,ci.defect_location,ci.defect_range,
                           ci.worst_component,ci.maintenance_advice,ci.special_check_required
                    FROM tb_bridge_type_component_config cfg
                    JOIN tb_part p ON p.part_code=cfg.part_code JOIN tb_component c ON c.component_code=cfg.component_code
                    LEFT JOIN tb_component_inspection ci ON ci.periodic_inspection_code=? AND ci.component_code=cfg.component_code
                    WHERE cfg.bridge_type_code=? AND cfg.active_flag=1 ORDER BY p.sort_order,cfg.display_order
                    """, code, bridge.get("bridge_type_code"));
            return new InspectionContext(null, code, tableCode, record, rows);
        }
        return new InspectionContext(null, null, "A", null, List.of());
    }

    private String buildDocument(String reportId, String version, String type, Map<String, Object> bridge,
                                 InspectionContext context) {
        String body = switch (type) {
            case "initial_record" -> officialInitialDocument(bridge, context);
            case "periodic_record" -> officialPeriodicDocument(bridge, context);
            case "bridge_summary" -> summaryDocument(bridge);
            default -> officialBridgeCardDocument(bridge);
        };
        String pageSize = "bridge_card".equals(type) ? "A4 landscape" : "A4 portrait";
        String css = ("""
                @page{size:PAGE_SIZE;margin:10mm 9mm 13mm;@bottom-center{content:"第 " counter(page) " 页 / 共 " counter(pages) " 页";font-family:'BridgeCN';font-size:9px;color:#555}}
                *{box-sizing:border-box}body{font-family:'BridgeCN';font-size:10px;color:#111}h1{text-align:center;font-size:18px;margin:5px 0 10px}
                .top{border-bottom:1px solid #111;padding:3px 0}.number{float:right}.section{font-size:12px;font-weight:bold;margin:10px 0 4px}.subsection{font-weight:bold;margin:6px 0 3px;color:#333}
                table{width:100%;border-collapse:collapse;table-layout:fixed}th,td{border:1px solid #222;padding:5px 4px;word-wrap:break-word;vertical-align:middle}th{background:#f2f2f2}
                .label{font-weight:bold;background:#f7f7f7;width:9%}.value{width:24%}.inspection,.data-table{-fs-table-paginate:paginate}.inspection thead,.data-table thead{display:table-header-group}.inspection th,.data-table th{font-size:9px}.inspection td,.data-table td{text-align:center}.empty{text-align:center;color:#666;padding:8px}.card-section{margin-top:10px}.photo-table td{width:33.33%;vertical-align:top}.photo-table img{display:block;width:100%;height:112px;object-fit:cover}.photo-caption{font-size:8px;line-height:1.4;margin-top:3px}.footer{margin-top:10px;border-top:1px solid #333;padding-top:5px;color:#444}
                .official-head{text-align:center;border-top:1.5px solid #111;border-bottom:1px solid #111;padding:4px 0 6px}.official-head h1{margin:0;font-size:17px}.official-head p{margin:0 0 2px;font-weight:bold}.official-page{page-break-after:always}.official-page:last-child{page-break-after:auto}.numbered th{font-weight:bold;text-align:left;background:#fafafa}.numbered td{height:27px}.narrative td{height:35px}.test td,.test th{height:108px}.periodic th,.periodic td{text-align:center;padding:3px;font-size:8px}.periodic td{height:23px}.periodic-head th{text-align:center}.main-photo{width:50%;height:150px;object-fit:cover}.photo-label{text-align:center;font-weight:bold}.structure-cell span{display:inline-block;min-width:90px;margin:2px;padding:3px;border:1px solid #aaa}.vertical{writing-mode:vertical-rl;text-orientation:mixed;text-align:center;font-weight:bold;width:22px}
                """).replace("PAGE_SIZE", pageSize);
        return "<!DOCTYPE html><html xmlns='http://www.w3.org/1999/xhtml' lang='zh-CN'><head><meta charset='UTF-8' /><style>" + css + "</style></head><body>" + body + "</body></html>";
    }

    private String metaTable(Map<String, Object> bridge) {
        return "<table><tr><td class='label'>路线编号</td><td class='value'>" + esc(bridge.get("route_code")) +
                "</td><td class='label'>路线名称</td><td class='value'>" + esc(bridge.get("route_name")) +
                "</td><td class='label'>桥位桩号</td><td class='value'>" + esc(bridge.get("pile_number")) + "</td></tr>" +
                "<tr><td class='label'>桥梁编号</td><td>" + esc(bridge.get("bridge_code")) +
                "</td><td class='label'>桥梁名称</td><td>" + esc(bridge.get("bridge_name")) +
                "</td><td class='label'>桥梁类型</td><td>" + esc(bridge.get("bridge_type_name")) + "</td></tr></table>";
    }

    private String officialInitialDocument(Map<String, Object> bridge, InspectionContext context) {
        Map<String, Object> record = context.record();
        StringBuilder html = new StringBuilder(officialHead("桥梁初始检查记录表", bridge));
        html.append("<div class='official-page'><table class='numbered'><tbody>");
        html.append(numberedTriple(1, "路线编号", value(record, bridge, "route_code"), 2, "路线名称", value(record, bridge, "route_name"), 3, "桥位桩号", value(record, bridge, "pile_number")));
        html.append(numberedTriple(4, "桥梁编号", value(record, bridge, "bridge_code"), 5, "桥梁名称", value(record, bridge, "bridge_name"), 6, "被跨越道路（通道）名称", value(record, bridge, "crossed_road_name")));
        html.append(numberedTriple(7, "被跨越道路（通道）桩号", value(record, bridge, "crossed_road_pile"), 8, "桥梁全长(m)", value(record, bridge, "bridge_length"), 9, "最大跨径(m)", value(record, bridge, "maximum_span")));
        html.append(numberedWide(10, "上、下部结构形式", value(record, bridge, "structure_form")));
        html.append(numberedWide(11, "桥梁分联及跨径组合", value(record, bridge, "span_combination")));
        html.append(numberedWide(12, "桥梁施工方法", value(record, bridge, "construction_method")));
        html.append(numberedWide(13, "新建桥梁在施工过程中的返工、维修或加固情况", value(record, bridge, "construction_rework")));
        html.append(numberedWide(14, "加固改造后的桥梁、加固改造情况", value(record, bridge, "reinforcement_info")));
        html.append(numberedWide(15, "档案资料不齐全的桥梁、维修加固情况", value(record, bridge, "missing_archive_drawings")));
        html.append(numberedTriple(16, "设计单位名称", value(record, bridge, "design_unit"), 17, "施工单位名称", value(record, bridge, "construction_unit"), 18, "监理单位名称", value(record, bridge, "supervision_unit")));
        html.append(numberedTriple(19, "交工时间（年 月 日）", value(record, bridge, "completion_date"), 20, "初始检查（年 月 日）", value(record, bridge, "inspection_date"), 21, "初始检查时的气候及环境温度", value(record, bridge, "weather_temperature")));
        html.append(numberedWide(22, "桥面高程", initialItem(context.rows(), "桥面高程")));
        html.append(numberedWide(23, "拱轴线", initialItem(context.rows(), "拱轴线")));
        html.append("</tbody></table></div><div class='official-page'><table class='numbered'><tbody>");
        for (Object[] item : List.of(new Object[]{24,"主缆线形"},new Object[]{25,"墩、台身、锚碇的高程"},new Object[]{26,"墩、台身、索塔倾斜度"},new Object[]{27,"索塔水平变位、高程"},new Object[]{28,"拱桥桥台、悬索桥锚碇水平位移"},new Object[]{29,"悬索桥索夹螺栓紧固力"},new Object[]{30,"水中基础"},new Object[]{31,"斜拉索或吊杆索力"},new Object[]{32,"主要承重构件尺寸"},new Object[]{33,"材质强度"},new Object[]{34,"保护层厚度"},new Object[]{35,"钢管混凝土管内混凝土密实度"})) {
            html.append(numberedWide((Integer)item[0], String.valueOf(item[1]), initialItem(context.rows(), String.valueOf(item[1]))));
        }
        html.append("<tr class='test'><th>36 静载试验结果</th><td>").append(esc(initialItem(context.rows(), "静载试验"))).append("</td></tr>");
        html.append("<tr class='test'><th>37 动载试验结果</th><td>").append(esc(initialItem(context.rows(), "动载试验"))).append("</td></tr>");
        html.append("<tr><th>38 记录人</th><td>").append(esc(value(record, bridge, "recorder"))).append("</td><th>39 桥梁工程师</th><td>").append(esc(value(record, bridge, "bridge_engineer"))).append("</td></tr>");
        html.append("<tr><th>40 桥梁初始检查机构</th><td colspan='3'>").append(esc(value(record, bridge, "inspection_org"))).append("</td></tr>");
        return html.append("</tbody></table></div>").toString();
    }

    private String officialPeriodicDocument(Map<String, Object> bridge, InspectionContext context) {
        Map<String, Object> record = context.record();
        String title = periodicTitle(context.tableCode());
        StringBuilder html = new StringBuilder(officialHead(title, bridge));
        html.append("<div class='official-page'><div class='top'>公路管理机构名称：").append(esc(bridge.get("road_management_org"))).append("</div><table class='numbered'><tbody>");
        html.append(numberedTriple(1, "路线编号", value(record, bridge, "route_code"), 2, "路线名称", value(record, bridge, "route_name"), 3, "桥位桩号", value(record, bridge, "pile_number")));
        html.append(numberedTriple(4, "桥梁编号", value(record, bridge, "bridge_code"), 5, "桥梁名称", value(record, bridge, "bridge_name"), 6, "被跨越道路名称", value(record, bridge, "crossed_road_name")));
        html.append(numberedTriple(7, "桥梁全长(m)", value(record, bridge, "bridge_length"), 8, "主跨结构", value(record, bridge, "main_span_structure"), 9, "最大跨径(m)", value(record, bridge, "maximum_span")));
        html.append(numberedTriple(10, "管养单位", value(record, bridge, "management_unit"), 11, "建成时间", value(record, bridge, "completion_date"), 12, "上次修复养护时间", value(record, bridge, "last_maintenance_date")));
        html.append(numberedTriple(13, "上次检查时间", value(record, bridge, "last_inspection_date"), 14, "本次检查时间", value(record, bridge, "inspection_date"), 15, "本次检查时气候及环境温度", value(record, bridge, "weather_temperature")));
        html.append("</tbody></table><table class='periodic'><thead><tr class='periodic-head'><th rowspan='2'>序号</th><th rowspan='2'>16 部位</th><th rowspan='2'>17 部件名称</th><th rowspan='2'>18 评分</th><th colspan='5'>19 缺损</th><th rowspan='2'>20 养护建议（维修范围、方式、时间）</th><th rowspan='2'>21 是否需特殊检查</th></tr><tr><th>类型</th><th>位置</th><th>范围</th><th>照片</th><th>最不利构件</th></tr></thead><tbody>");
        int index = 1;
        for (Map<String, Object> row : context.rows()) {
            html.append("<tr><td>").append(index++).append("</td><td>").append(esc(row.get("part_name"))).append("</td><td>").append(esc(row.get("component_name"))).append("</td><td>").append(esc(row.get("score"))).append("</td><td>").append(esc(row.get("defect_type"))).append("</td><td>").append(esc(row.get("defect_location"))).append("</td><td>").append(esc(row.get("defect_range"))).append("</td><td>　</td><td>").append(esc(row.get("worst_component"))).append("</td><td>").append(esc(row.get("maintenance_advice"))).append("</td><td>").append(esc(row.get("special_check_required"))).append("</td></tr>");
        }
        if (context.rows().isEmpty()) html.append("<tr><td colspan='11' class='empty'>该桥型尚未配置检查部件，不能作为正式定期检查表提交。</td></tr>");
        html.append("</tbody></table><table class='numbered'><tbody><tr><th>22 桥梁技术状况评定等级</th><td>").append(esc(first(record.get("rating_level_name"), record.get("rating_level_code")))).append("</td><th>23 全桥清洁状况</th><td>").append(esc(record.get("cleanliness"))).append("</td><th>24 预防及修复养护状况</th><td>").append(esc(record.get("maintenance_status"))).append("</td></tr><tr><th>25 记录人</th><td>").append(esc(record.get("recorder"))).append("</td><th>26 负责人</th><td>").append(esc(record.get("principal"))).append("</td><th>27 下次检查时间</th><td>").append(esc(record.get("next_inspection_date"))).append("</td></tr></tbody></table></div>");
        return html.toString();
    }

    private String officialBridgeCardDocument(Map<String, Object> bridge) {
        return officialHead("桥梁基本状况卡片", bridge) + metaTable(bridge) + bridgeCardSections(bridge);
    }

    private String officialHead(String title, Map<String, Object> bridge) {
        return "<div class='official-head'><p>（" + esc(bridge.get("road_management_org")) + "）</p><h1>" + esc(title) + "</h1></div>";
    }

    private String numberedTriple(int aNo, String aLabel, Object aValue, int bNo, String bLabel, Object bValue, int cNo, String cLabel, Object cValue) {
        return "<tr><th>" + aNo + " " + esc(aLabel) + "</th><td>" + esc(aValue) + "</td><th>" + bNo + " " + esc(bLabel) + "</th><td>" + esc(bValue) + "</td><th>" + cNo + " " + esc(cLabel) + "</th><td>" + esc(cValue) + "</td></tr>";
    }

    private String numberedWide(int no, String label, Object value) {
        return "<tr class='narrative'><th>" + no + " " + esc(label) + "</th><td>" + esc(value) + "</td></tr>";
    }

    private Object value(Map<String, Object> record, Map<String, Object> bridge, String key) {
        return first(record == null ? null : record.get(key), bridge.get(key));
    }

    private Object first(Object primary, Object fallback) {
        return primary == null || string(primary).isBlank() ? fallback : primary;
    }

    private String initialItem(List<Map<String, Object>> rows, String label) {
        return rows.stream().filter(row -> string(row.get("item_name")).contains(label) || label.contains(string(row.get("item_name"))))
                .map(row -> string(row.get("measured_value"))).filter(value -> !value.isBlank()).findFirst().orElse("未录入");
    }

    private String periodicTitle(String tableCode) {
        return switch (tableCode) {
            case "C-1" -> "表 C-1　桥梁定期检查记录表（梁桥）";
            case "C-2" -> "表 C-2　桥梁定期检查记录表（板拱桥、肋拱桥、箱形拱桥、双曲拱桥）";
            case "C-3" -> "表 C-3　桥梁定期检查记录表（刚架拱桥、桁架拱桥）";
            case "C-4" -> "表 C-4　桥梁定期检查记录表（钢-混凝土组合拱桥）";
            case "C-5" -> "表 C-5　桥梁定期检查记录表（斜拉桥）";
            case "C-6" -> "表 C-6　桥梁定期检查记录表（悬索桥）";
            default -> "桥梁定期检查记录表";
        };
    }

    private String bridgeCardSections(Map<String, Object> bridge) {
        String bridgeCode = string(bridge.get("bridge_code"));
        List<Map<String, Object>> components = jdbcTemplate.queryForList("""
                SELECT p.part_name,c.component_name,sc.component_serial,sc.location_desc,sc.material_type,
                       sc.dimension_spec,sc.quantity,sc.force_value,sc.elevation_displacement,sc.remark
                FROM tb_bridge_specific_component sc
                LEFT JOIN tb_part p ON p.part_code=sc.part_code
                LEFT JOIN tb_component c ON c.component_code=sc.component_code
                WHERE sc.bridge_code=? AND sc.status=1
                ORDER BY p.sort_order,c.component_name,sc.component_serial
                """, bridgeCode);
        List<Map<String, Object>> spans = jdbcTemplate.queryForList(
                "SELECT span_no,span_length,structure_form,material_type,location_desc,remark FROM tb_bridge_span_detail WHERE bridge_code=? ORDER BY span_no", bridgeCode);
        List<Map<String, Object>> measurementPoints = jdbcTemplate.queryForList(
                "SELECT point_no,point_name,benchmark_elevation,remark FROM tb_bridge_measurement_point WHERE bridge_code=? ORDER BY point_category,display_order,point_no", bridgeCode);
        List<Map<String, Object>> structures = jdbcTemplate.queryForList(
                "SELECT structure_group,structure_type,serial_no,form,material_type,quantity,location_desc,remark FROM tb_bridge_structure_detail WHERE bridge_code=? ORDER BY structure_group,display_order,serial_no", bridgeCode);
        List<Map<String, Object>> cables = jdbcTemplate.queryForList(
                "SELECT cable_type,serial_no,force_value,material_type,location_desc,remark FROM tb_bridge_cable_detail WHERE bridge_code=? ORDER BY cable_type,display_order,serial_no", bridgeCode);
        List<Map<String, Object>> archives = jdbcTemplate.queryForList("""
                SELECT ai.archive_item_name,ar.completeness_status,ar.description
                FROM tb_bridge_archive_record ar
                LEFT JOIN tb_archive_item ai ON ai.archive_item_code=ar.archive_item_code
                WHERE ar.bridge_code=? ORDER BY ai.archive_item_name
                """, bridgeCode);
        List<Map<String, Object>> evaluations = jdbcTemplate.queryForList("""
                SELECT eh.evaluation_date,cc.check_category_name,eh.rating_result,eh.special_conclusion,
                       eh.treatment_strategy,eh.next_check_date
                FROM tb_evaluation_history eh
                LEFT JOIN tb_check_category cc ON cc.check_category_code=eh.check_category_code
                WHERE eh.bridge_code=? ORDER BY eh.evaluation_date ASC, CASE WHEN cc.check_category_name LIKE '%初始%' THEN 0 ELSE 1 END
                """, bridgeCode);
        List<Map<String, Object>> photos = jdbcTemplate.queryForList("""
                SELECT file_name,storage_path,file_type,file_description,photo_category,upload_time
                FROM tb_attachment WHERE bridge_code=? ORDER BY upload_time DESC
                """, bridgeCode);

        StringBuilder html = new StringBuilder();
        html.append(cardSection("A", "桥梁所处行政区划", keyValueTable(
                "行政区划代码", bridge.get("administrative_code"), "桥梁详细地址", bridge.get("location_address"),
                "经纬度", coordinate(bridge))));
        html.append(cardSection("B", "行政识别数据", keyValueTable(
                "路线编号", bridge.get("route_code"), "路线名称", bridge.get("route_name"), "路线等级", bridge.get("route_grade"),
                "桥梁编号", bridge.get("bridge_code"), "桥梁名称", bridge.get("bridge_name"), "桥位桩号", bridge.get("pile_number"),
                "功能类型", bridge.get("function_type"), "被跨道路（通道）", bridge.get("crossed_road_name"), "被跨道路桩号", bridge.get("crossed_road_pile"),
                "养护等级", bridge.get("maintenance_level"), "设计荷载", bridge.get("design_load"), "桥梁坡度", bridge.get("bridge_slope"),
                "平曲线半径", curveRadius(bridge.get("curve_radius")), "建成年份", bridge.get("built_year"), "设计单位", bridge.get("design_unit"),
                "施工单位", bridge.get("construction_unit"), "监理单位", bridge.get("supervision_unit"), "业主单位", bridge.get("owner_unit"),
                "管养单位", bridge.get("management_unit"))));
        html.append(cardSection("C", "桥梁技术指标", keyValueTable(
                "桥梁全长（m）", bridge.get("bridge_length"), "桥面总宽（m）", bridge.get("deck_width"), "车道宽度（m）", bridge.get("lane_width"),
                "人行道宽度（m）", bridge.get("sidewalk_width"), "护栏/防撞墙高（m）", bridge.get("barrier_height"), "中央分隔带宽（m）", bridge.get("median_width"),
                "桥面标准净空", bridge.get("standard_clearance"), "桥面实际净空", bridge.get("actual_clearance"), "通航等级及标准净空", bridge.get("navigation_standard"),
                "桥下实际净空", bridge.get("navigation_actual"), "引道总宽（m）", bridge.get("approach_width"), "引道线形/曲线半径", bridge.get("approach_alignment"),
                "设计洪水频率及水位", bridge.get("design_flood"), "历史洪水位", bridge.get("historical_flood"), "地震动峰值加速度", bridge.get("seismic_coefficient"))));

        StringBuilder structure = new StringBuilder();
        structure.append(subsection("34 桥面高程（根据测点设置列数）")).append(dataTable(
                new String[]{"测点编号", "测点名称", "基准高程（m）"}, measurementPoints,
                "point_no", "point_name", "benchmark_elevation"));
        structure.append(subsection("35 桥梁分孔（根据孔数设置列数）")).append(dataTable(
                new String[]{"孔号", "跨径（m）", "结构形式", "材料"}, spans,
                "span_no", "span_length", "structure_form", "material_type"));
        String[] structHeaders = new String[]{"结构类型", "编号", "形式", "材料", "数量"};
        String[] structKeys = new String[]{"structure_type", "serial_no", "form", "material_type", "quantity"};
        structure.append(subsection("36—41 上部结构形式与材料（按种类设置列数）")).append(dataTable(
                structHeaders, filterByGroup(structures, "superstructure"), structKeys));
        structure.append(subsection("桥面系形式与材料（A表45-49）")).append(dataTable(
                structHeaders, filterByGroup(structures, "deck"), structKeys));
        structure.append(subsection("下部结构形式与材料（A表50-53）")).append(dataTable(
                structHeaders, filterByGroup(structures, "substructure"), structKeys));
        structure.append(subsection("基础形式与材料（A表54-55）")).append(dataTable(
                structHeaders, filterByGroup(structures, "foundation"), structKeys));
        structure.append(subsection("支座形式与材料（A表56-59）")).append(dataTable(
                structHeaders, filterByGroup(structures, "bearing_facility"), structKeys));
        structure.append(subsection("42—44 斜拉索、吊杆、系杆（按索数设置列数，含索力）")).append(dataTable(
                new String[]{"类型", "编号", "索力/内力", "材料"}, cables,
                "cable_type", "serial_no", "force_value", "material_type"));
        html.append(cardSection("D", "桥梁结构信息", structure.toString()));
        html.append(cardSection("E（60—71）", "桥梁档案资料", dataTable(
                new String[]{"档案资料项", "齐全状态", "说明"}, archives,
                "archive_item_name", "completeness_status", "description")));
        html.append(cardSection("F（72—76）", "桥梁检测评定时间", dataTable(
                new String[]{"评定时间", "检测类别", "评定结果", "特殊检查结论", "下次检测时间"}, evaluations,
                "evaluation_date", "check_category_name", "rating_result", "special_conclusion", "next_check_date")));
        html.append(cardSection("H（88）", "需要说明的事项", keyValueTable(
                "说明", bridge.get("notes"), "公路管理机构", bridge.get("road_management_org"), "管养单位", bridge.get("management_unit"),
                "桥梁工程师", bridge.get("bridge_engineer"), "填卡人", bridge.get("card_filler"), "填卡日期", bridge.get("card_date"))));
        html.append(cardSection("I（89—93）", "桥梁总体照片及桥梁正面照片", mainPhotoTable(photos, bridge)));
        return html.toString();
    }

    private String cardSection(String code, String title, String content) {
        return "<div class='card-section'><div class='section'>" + esc(code) + " " + esc(title) + "</div>" + content + "</div>";
    }

    private String subsection(String title) {
        return "<div class='subsection'>" + esc(title) + "</div>";
    }

    private String keyValueTable(Object... cells) {
        StringBuilder html = new StringBuilder("<table class='info-table'><tbody>");
        for (int offset = 0; offset < cells.length; offset += 6) {
            html.append("<tr>");
            for (int column = 0; column < 3; column++) {
                int index = offset + column * 2;
                if (index + 1 < cells.length) {
                    html.append("<td class='label'>").append(esc(cells[index])).append("</td><td>").append(esc(cells[index + 1])).append("</td>");
                } else {
                    html.append("<td class='label'>　</td><td>　</td>");
                }
            }
            html.append("</tr>");
        }
        return html.append("</tbody></table>").toString();
    }

    private String dataTable(String[] headers, List<Map<String, Object>> rows, String... keys) {
        StringBuilder html = new StringBuilder("<table class='data-table'><thead><tr>");
        for (String header : headers) html.append("<th>").append(esc(header)).append("</th>");
        html.append("</tr></thead><tbody>");
        if (rows.isEmpty()) {
            html.append("<tr><td class='empty' colspan='").append(headers.length).append("'>暂无记录</td></tr>");
        } else {
            for (Map<String, Object> row : rows) {
                html.append("<tr>");
                for (String key : keys) html.append("<td>").append(esc(row.get(key))).append("</td>");
                html.append("</tr>");
            }
        }
        return html.append("</tbody></table>").toString();
    }

    private String photoTable(List<Map<String, Object>> rows) {
        StringBuilder html = new StringBuilder("<table class='photo-table'><tbody><tr>");
        int count = 0;
        for (Map<String, Object> row : rows) {
            String source = localImageSource(row);
            if (source.isBlank()) continue;
            html.append("<td><img src='").append(esc(source)).append("' alt='").append(esc(row.get("file_description"))).append("'/>")
                    .append("<div class='photo-caption'>").append(esc(row.get("photo_category"))).append(" · ")
                    .append(esc(row.get("file_description"))).append("<br/>").append(esc(row.get("file_name"))).append("</div></td>");
            count++;
            if (count % 3 == 0 && count < rows.size()) html.append("</tr><tr>");
        }
        if (count == 0) return "<table><tbody><tr><td class='empty'>暂无可用照片</td></tr></tbody></table>";
        while (count % 3 != 0) {
            html.append("<td>　</td>");
            count++;
        }
        return html.append("</tr></tbody></table>").toString();
    }

    private String mainPhotoTable(List<Map<String, Object>> rows, Map<String, Object> bridge) {
        StringBuilder html = new StringBuilder("<table class='photo-table'><tbody><tr>");
        for (String category : List.of("overall", "front")) {
            Map<String, Object> photo = rows.stream().filter(row -> category.equals(string(row.get("photo_category"))))
                    .filter(row -> !localImageSource(row).isBlank()).findFirst().orElse(null);
            String label = "overall".equals(category) ? "89 桥梁总体照片" : "90 桥梁正面照片";
            html.append("<td><div class='photo-label'>").append(label).append("</div>");
            if (photo == null) html.append("<div class='empty'>未录入照片</div>");
            else html.append("<img class='main-photo' src='").append(esc(localImageSource(photo))).append("' alt='").append(esc(photo.get("file_description"))).append("'/><div class='photo-caption'>").append(esc(photo.get("file_description"))).append("</div>");
            html.append("</td>");
        }
        html.append("</tr><tr><td>91 桥梁工程师：").append(esc(bridge.get("bridge_engineer"))).append("</td><td>92 填卡人：")
                .append(esc(bridge.get("card_filler"))).append("　93 填卡日期：").append(esc(bridge.get("card_date"))).append("</td></tr>");
        return html.append("</tbody></table>").toString();
    }

    private String coordinate(Map<String, Object> bridge) {
        Object longitude = bridge.get("longitude"), latitude = bridge.get("latitude");
        return longitude == null || latitude == null ? "" : longitude + ", " + latitude;
    }

    private String localImageSource(Map<String, Object> row) {
        String fileType = string(row.get("file_type")).toLowerCase();
        String fileName = string(row.get("file_name")).toLowerCase();
        if (!(fileType.startsWith("image/") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))) return "";
        try {
            Path target = uploadDir.resolve(string(row.get("storage_path"))).normalize();
            return target.startsWith(uploadDir) && Files.isRegularFile(target) ? target.toUri().toString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String inspectionTable(InspectionContext context) {
        Map<String, Object> record = context.record();
        StringBuilder html = new StringBuilder("<div class='section'>检查基本信息</div><table><tr>");
        html.append("<td class='label'>检查日期</td><td>").append(esc(record.get("inspection_date"))).append("</td>")
                .append("<td class='label'>天气与温度</td><td>").append(esc(record.get("weather_temperature"))).append("</td>")
                .append("<td class='label'>记录状态</td><td>").append(esc(record.get("status"))).append("</td></tr></table>");
        html.append("<div class='section'>检查项目记录</div><table class='inspection'><thead><tr>");
        if (context.initialCode() != null) html.append("<th>项目分类</th><th>检测项目</th><th>检测结果</th><th>单位</th><th>检测说明</th>");
        else html.append("<th>部位</th><th>部件名称</th><th>评分</th><th>缺损类型</th><th>位置</th><th>范围</th><th>最不利构件</th><th>养护建议</th><th>特殊检查</th>");
        html.append("</tr></thead><tbody>");
        for (Map<String, Object> row : context.rows()) {
            html.append("<tr>");
            List<String> keys = context.initialCode() != null
                    ? List.of("item_category","item_name","measured_value","unit","inspection_description")
                    : List.of("part_name","component_name","score","defect_type","defect_location","defect_range","worst_component","maintenance_advice","special_check_required");
            for (String key : keys) html.append("<td>").append(esc(row.get(key))).append("</td>");
            html.append("</tr>");
        }
        if (context.rows().isEmpty()) html.append("<tr><td colspan='9'>暂无检查明细</td></tr>");
        return html.append("</tbody></table>").toString();
    }

    private void renderPdf(String html, Path file) {
        try (OutputStream output = Files.newOutputStream(file)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            if (Files.exists(fontPath)) builder.useFont(fontPath.toFile(), "BridgeCN");
            builder.withHtmlContent(html, reportDir.toUri().toString());
            builder.toStream(output);
            builder.run();
        } catch (Exception ex) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            throw new BusinessException("PDF 报告生成失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> bridge(String bridgeCode) {
        return queryOne("""
                SELECT b.*,r.route_name,r.route_grade,bt.bridge_type_name FROM tb_bridge b
                LEFT JOIN tb_route r ON r.route_code=b.route_code
                LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code WHERE b.bridge_code=?
                """, bridgeCode);
    }

    private String tableCode(String bridgeType) {
        return switch (bridgeType) {
            case "beam" -> "C-1"; case "arch" -> "C-2"; case "rigid_arch" -> "C-3";
            case "composite_arch" -> "C-4"; case "cable_stayed" -> "C-5"; case "suspension" -> "C-6";
            default -> throw new BusinessException("桥梁类型待核验，不能生成正式定期检查表");
        };
    }

    private String normalizeType(String requested, String taskType) {
        if ("bridge_card".equals(requested)) return "bridge_card";
        String expected = switch (taskType) {
            case "initial" -> "initial_record";
            case "periodic" -> "periodic_record";
            default -> throw new BusinessException("检查任务类型无效，无法生成报告");
        };
        if (requested != null && !requested.isBlank() && !"comprehensive".equals(requested) && !expected.equals(requested)) {
            throw new BusinessException("报告类型与所选检查任务不一致，请重新选择任务");
        }
        return expected;
    }

    private String nextVersion(String taskId, String reportType) {
        Integer count = taskId == null
                ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_report WHERE task_id IS NULL AND report_type=?", Integer.class, reportType)
                : jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_report WHERE task_id=? AND report_type=?", Integer.class, taskId, reportType);
        return "V1." + (count == null ? 0 : count);
    }

    private Map<String, Object> queryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        if (rows.isEmpty()) throw new BusinessException("报告所需数据不存在");
        return new LinkedHashMap<>(rows.get(0));
    }

    private String compact(Object value) {
        String text = string(value);
        return text.isBlank() ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) : text.replace("-", "");
    }

    private String curveRadius(Object value) {
        String text = string(value);
        return text.isBlank() || "null".equalsIgnoreCase(text) ? "∞" : text;
    }

    private List<Map<String, Object>> filterByGroup(List<Map<String, Object>> rows, String group) {
        return rows.stream().filter(row -> group.equals(String.valueOf(row.get("structure_group")))).toList();
    }

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private String esc(Object value) {
        String text = string(value);
        return text.isBlank() || "null".equalsIgnoreCase(text) ? "　" : text.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record InspectionContext(String initialCode, String periodicCode, String tableCode,
                                     Map<String, Object> record, List<Map<String, Object>> rows) { }
}
