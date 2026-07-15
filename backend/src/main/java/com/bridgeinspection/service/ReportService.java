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
                (report_id,task_id,initial_inspection_code,periodic_inspection_code,report_type,version_no,
                 file_format,file_path,report_status,generation_time,generator_id,change_summary)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, reportId, taskId, context.initialCode(), context.periodicCode(), reportType, version,
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
        String title = switch (type) {
            case "initial_record" -> "桥梁初始检查记录表";
            case "periodic_record" -> "桥梁定期检查记录表";
            default -> "桥梁基本状况卡片";
        };
        StringBuilder body = new StringBuilder();
        body.append("<div class='top'><span>公路管理机构名称：").append(esc(bridge.get("road_management_org"))).append("</span>")
                .append("<span class='number'>专属编号：").append(esc(context.record() == null ? reportId : context.record().get("record_form_no"))).append("</span></div>");
        body.append("<h1>表 ").append(esc(context.tableCode())).append("　").append(title).append("</h1>");
        body.append(metaTable(bridge));
        if ("bridge_card".equals(type)) body.append(bridgeCardSections(bridge));
        else body.append(inspectionTable(context));
        body.append("<div class='footer'>报告编号：").append(esc(reportId)).append("　版本：").append(esc(version))
                .append("　生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</div>");
        return """
                <!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml" lang="zh-CN"><head><meta charset="UTF-8" />
                <style>
                @page{size:A4 landscape;margin:12mm 10mm 14mm}@page{ @bottom-center{content:"第 " counter(page) " 页 / 共 " counter(pages) " 页";font-size:9px;color:#555}}
                *{box-sizing:border-box}body{font-family:'BridgeCN';font-size:10px;color:#111}h1{text-align:center;font-size:18px;margin:5px 0 10px}
                .top{border-bottom:1px solid #111;padding:3px 0}.number{float:right}.section{font-size:12px;font-weight:bold;margin:10px 0 4px}
                table{width:100%;border-collapse:collapse;table-layout:fixed}th,td{border:1px solid #222;padding:5px 4px;word-wrap:break-word;vertical-align:middle}th{background:#f2f2f2}
                .label{font-weight:bold;background:#f7f7f7;width:9%}.value{width:24%}.inspection th{font-size:9px}.inspection td{text-align:center}.footer{margin-top:10px;border-top:1px solid #333;padding-top:5px;color:#444}
                </style></head><body>""" + body + "</body></html>";
    }

    private String metaTable(Map<String, Object> bridge) {
        return "<table><tr><td class='label'>路线编号</td><td class='value'>" + esc(bridge.get("route_code")) +
                "</td><td class='label'>路线名称</td><td class='value'>" + esc(bridge.get("route_name")) +
                "</td><td class='label'>桥位桩号</td><td class='value'>" + esc(bridge.get("pile_number")) + "</td></tr>" +
                "<tr><td class='label'>桥梁编号</td><td>" + esc(bridge.get("bridge_code")) +
                "</td><td class='label'>桥梁名称</td><td>" + esc(bridge.get("bridge_name")) +
                "</td><td class='label'>桥梁类型</td><td>" + esc(bridge.get("bridge_type_name")) + "</td></tr></table>";
    }

    private String bridgeCardSections(Map<String, Object> bridge) {
        return "<div class='section'>A 桥梁所处行政区划</div><table><tr><td class='label'>行政区划代码</td><td>" + esc(bridge.get("administrative_code")) +
                "</td><td class='label'>详细地址</td><td colspan='3'>" + esc(bridge.get("location_address")) + "</td></tr></table>" +
                "<div class='section'>B 行政识别数据</div><table><tr><td class='label'>功能类型</td><td>" + esc(bridge.get("function_type")) +
                "</td><td class='label'>设计荷载</td><td>" + esc(bridge.get("design_load")) +
                "</td><td class='label'>建成时间</td><td>" + esc(bridge.get("built_year")) + "</td></tr>" +
                "<tr><td class='label'>设计单位</td><td>" + esc(bridge.get("design_unit")) + "</td><td class='label'>施工单位</td><td>" +
                esc(bridge.get("construction_unit")) + "</td><td class='label'>管养单位</td><td>" + esc(bridge.get("management_unit")) + "</td></tr></table>" +
                "<div class='section'>C 桥梁技术指标</div><table><tr><td class='label'>桥梁全长(m)</td><td>" + esc(bridge.get("bridge_length")) +
                "</td><td class='label'>桥面总宽(m)</td><td>" + esc(bridge.get("deck_width")) +
                "</td><td class='label'>车道宽度(m)</td><td>" + esc(bridge.get("lane_width")) + "</td></tr>" +
                "<tr><td class='label'>跨径组合</td><td colspan='2'>" + esc(bridge.get("span_combination")) +
                "</td><td class='label'>结构体系</td><td colspan='2'>" + esc(bridge.get("structural_system")) + "</td></tr></table>";
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
            default -> "C-7";
        };
    }

    private String normalizeType(String requested, String taskType) {
        if ("bridge_card".equals(requested)) return "bridge_card";
        if ("initial_record".equals(requested) || "initial".equals(taskType)) return "initial_record";
        return "periodic_record";
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

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private String esc(Object value) {
        String text = string(value);
        return text.isBlank() || "null".equalsIgnoreCase(text) ? "　" : text.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record InspectionContext(String initialCode, String periodicCode, String tableCode,
                                     Map<String, Object> record, List<Map<String, Object>> rows) { }
}
