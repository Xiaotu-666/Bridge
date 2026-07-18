package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
public final class BridgeProfileService {
    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final FileStorageService fileStorageService;
    private final CoordinateService coordinateService;
    private final String amapKey;

    public BridgeProfileService(JdbcTemplate jdbcTemplate, RestClient.Builder builder, FileStorageService fileStorageService,
                                CoordinateService coordinateService,
                                @Value("${app.amap.web-service-key:}") String amapKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = builder.build();
        this.fileStorageService = fileStorageService;
        this.coordinateService = coordinateService;
        this.amapKey = amapKey;
    }

    public List<Map<String, Object>> mapPoints(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        String sql = "SELECT b.bridge_code,b.bridge_name,b.bridge_type_code,bt.bridge_type_name,b.route_code," +
                "b.location_address,b.longitude,b.latitude,b.coordinate_source,b.bridge_length,b.management_unit," +
                "SUM(CASE WHEN (c.component_name LIKE '%桥墩%' OR c.component_name LIKE '%桥桩%') AND sc.component_serial LIKE '%#%' THEN 1 ELSE 0 END) pier_count " +
                "FROM tb_bridge b LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code " +
                "LEFT JOIN tb_bridge_specific_component sc ON sc.bridge_code=b.bridge_code AND sc.status=1 " +
                "LEFT JOIN tb_component c ON c.component_code=sc.component_code " +
                "WHERE b.status=1 AND b.longitude IS NOT NULL AND b.latitude IS NOT NULL " +
                "AND (?='' OR b.bridge_code LIKE CONCAT('%',?,'%') OR b.bridge_name LIKE CONCAT('%',?,'%') " +
                "OR b.location_address LIKE CONCAT('%',?,'%')) " +
                "GROUP BY b.bridge_code,b.bridge_name,b.bridge_type_code,bt.bridge_type_name,b.route_code," +
                "b.location_address,b.longitude,b.latitude,b.coordinate_source,b.bridge_length,b.management_unit ORDER BY b.bridge_name";
        return jdbcTemplate.queryForList(sql, value, value, value, value).stream()
                .map(coordinateService::toGcj).toList();
    }

    public Map<String, Object> profile(String bridgeCode) {
        List<Map<String, Object>> bridges = jdbcTemplate.queryForList(
                "SELECT b.*,r.route_name,r.route_grade,bt.bridge_type_name FROM tb_bridge b " +
                        "LEFT JOIN tb_route r ON r.route_code=b.route_code " +
                        "LEFT JOIN tb_bridge_type bt ON bt.bridge_type_code=b.bridge_type_code WHERE b.bridge_code=?",
                bridgeCode);
        if (bridges.isEmpty()) throw new BusinessException("桥梁不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bridge", coordinateService.toGcj(bridges.get(0)));
        result.put("components", jdbcTemplate.queryForList(
                "SELECT sc.*,p.part_name,c.component_name FROM tb_bridge_specific_component sc " +
                        "LEFT JOIN tb_part p ON p.part_code=sc.part_code LEFT JOIN tb_component c ON c.component_code=sc.component_code " +
                        "WHERE sc.bridge_code=? AND sc.status=1 ORDER BY p.sort_order,c.component_name,sc.component_serial", bridgeCode));
        result.put("archives", jdbcTemplate.queryForList(
                "SELECT ar.*,ai.archive_item_name FROM tb_bridge_archive_record ar LEFT JOIN tb_archive_item ai ON ai.archive_item_code=ar.archive_item_code " +
                        "WHERE ar.bridge_code=? ORDER BY ai.archive_item_name", bridgeCode));
        result.put("evaluations", jdbcTemplate.queryForList(
                "SELECT eh.*,cc.check_category_name FROM tb_evaluation_history eh LEFT JOIN tb_check_category cc ON cc.check_category_code=eh.check_category_code " +
                        "WHERE eh.bridge_code=? ORDER BY eh.evaluation_date DESC", bridgeCode));
        List<Map<String, Object>> initialInspections = jdbcTemplate.queryForList(
                "SELECT * FROM tb_initial_inspection WHERE bridge_code=? ORDER BY effective_flag DESC, inspection_date DESC, version_no DESC", bridgeCode);
        result.put("initialInspections", initialInspections);
        if (!initialInspections.isEmpty()) {
            String initialCode = String.valueOf(initialInspections.get(0).get("initial_inspection_code"));
            result.put("initialItems", jdbcTemplate.queryForList(
                    "SELECT item.*,definition.item_name,definition.unit,definition.item_category FROM tb_initial_inspection_item item " +
                            "LEFT JOIN tb_initial_inspection_item_definition definition ON definition.item_code=item.item_code " +
                            "WHERE item.initial_inspection_code=? ORDER BY definition.item_category,definition.item_code", initialCode));
            result.put("initialComponentItems", jdbcTemplate.queryForList(
                    "SELECT record.*,component.component_serial,component.location_desc,component.material_type,component.dimension_spec," +
                            "definition.item_name,definition.unit,definition.item_category FROM tb_initial_component_inspection record " +
                            "JOIN tb_bridge_specific_component component ON component.bridge_component_id=record.bridge_component_id " +
                            "LEFT JOIN tb_initial_inspection_item_definition definition ON definition.item_code=record.item_code " +
                            "WHERE record.initial_inspection_code=? ORDER BY component.component_serial,definition.item_code", initialCode));
        } else {
            result.put("initialItems", List.of());
            result.put("initialComponentItems", List.of());
        }
        List<Map<String, Object>> periodicInspections = jdbcTemplate.queryForList(
                "SELECT pi.*,rl.rating_level_name FROM tb_periodic_inspection pi LEFT JOIN tb_rating_level rl ON rl.rating_level_code=pi.rating_level_code " +
                        "WHERE pi.bridge_code=? ORDER BY pi.inspection_date DESC", bridgeCode);
        List<Map<String, Object>> periodicCards = new java.util.ArrayList<>();
        for (Map<String, Object> inspection : periodicInspections) {
            Map<String, Object> card = new LinkedHashMap<>(inspection);
            String periodicCode = String.valueOf(inspection.get("periodic_inspection_code"));
            List<Map<String, Object>> componentRows = jdbcTemplate.queryForList(
                    "SELECT ci.*,sc.component_serial,sc.location_desc,sc.material_type,sc.dimension_spec,p.part_name,c.component_name,dd.defect_degree_name " +
                            "FROM tb_component_inspection ci LEFT JOIN tb_bridge_specific_component sc ON sc.bridge_component_id=ci.bridge_component_id " +
                            "LEFT JOIN tb_part p ON p.part_code=ci.part_code LEFT JOIN tb_component c ON c.component_code=ci.component_code " +
                            "LEFT JOIN tb_defect_degree dd ON dd.defect_degree_code=ci.defect_degree_code " +
                            "WHERE ci.periodic_inspection_code=? ORDER BY p.sort_order,c.component_name,sc.component_serial", periodicCode);
            card.put("componentInspections", componentRows);
            long defectCount = componentRows.stream().filter(row -> row.get("defect_type") != null
                    && !String.valueOf(row.get("defect_type")).contains("未见明显缺损")).count();
            card.put("defectCount", defectCount);
            inspection.put("defectCount", defectCount);
            periodicCards.add(card);
        }
        result.put("periodicInspections", periodicCards);
        result.put("inspectionSummary", inspectionSummary(bridgeCode, initialInspections, periodicInspections));
        result.put("structureDetails", structureDetails(bridgeCode));
        result.put("attachments", jdbcTemplate.queryForList(
                "SELECT file_id,file_name,storage_path,file_type,file_description,photo_category,upload_by,upload_time FROM tb_attachment " +
                "WHERE bridge_code=? ORDER BY upload_time DESC", bridgeCode));
        return result;
    }

    public Map<String, Object> uploadPhoto(String bridgeCode, MultipartFile file, String description, String category) {
        Long bridgeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_bridge WHERE bridge_code = ? AND status = 1", Long.class, bridgeCode);
        if (bridgeCount == null || bridgeCount == 0) throw new BusinessException("桥梁不存在");
        if (file == null || file.isEmpty()) throw new BusinessException("请选择要上传的桥梁照片");
        if (file.getSize() > 10L * 1024 * 1024) throw new BusinessException("单张照片不能超过10MB");
        String originalName = file.getOriginalFilename() == null ? "bridge-photo" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!(contentType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png"))) {
            throw new BusinessException("仅允许上传 JPG、JPEG、PNG 图片");
        }
        String storagePath = fileStorageService.store(file, "bridge-photos/" + bridgeCode);
        String storedFileName = Path.of(storagePath).getFileName().toString();
        String photoCategory = category == null || category.isBlank() ? "overall" : category.trim();
        String fileDescription = description == null || description.isBlank() ? categoryName(photoCategory) : description.trim();
        String currentUserId = SecurityUtils.currentUserId();
        jdbcTemplate.update("INSERT INTO tb_attachment (bridge_code,file_name,stored_file_name,storage_path,file_type,file_size,file_description,photo_category,upload_by) VALUES (?,?,?,?,?,?,?,?,?)",
                bridgeCode, originalName, storedFileName, storagePath, contentType, file.getSize(), fileDescription, photoCategory,
                currentUserId == null ? null : Integer.valueOf(currentUserId));
        return jdbcTemplate.queryForMap("SELECT file_id,file_name,storage_path,file_type,file_description,photo_category,upload_time FROM tb_attachment WHERE storage_path=? ORDER BY file_id DESC LIMIT 1", storagePath);
    }

    public void deletePhoto(String bridgeCode, Long fileId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT file_id,bridge_code,storage_path,upload_by FROM tb_attachment WHERE file_id=? AND bridge_code=?",
                fileId, bridgeCode);
        if (rows.isEmpty()) {
            throw new BusinessException("照片不存在或不属于当前桥梁");
        }
        Map<String, Object> photo = rows.get(0);
        var user = SecurityUtils.currentUserOrNull();
        String role = user == null || user.roles().isEmpty() ? "" : user.roles().get(0);
        String uploaderId = photo.get("upload_by") == null ? "" : String.valueOf(photo.get("upload_by"));
        if (!"admin".equals(role) && !"engineer".equals(role)
                && !uploaderId.equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "检查人员只能删除自己上传的照片");
        }
        fileStorageService.delete(String.valueOf(photo.get("storage_path")));
        jdbcTemplate.update("DELETE FROM tb_attachment WHERE file_id=? AND bridge_code=?", fileId, bridgeCode);
    }

    private Map<String, Object> structureDetails(String bridgeCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("spans", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_span_detail WHERE bridge_code=? ORDER BY span_no", bridgeCode));
        result.put("structures", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_structure_detail WHERE bridge_code=? ORDER BY structure_group,display_order,serial_no", bridgeCode));
        result.put("cables", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_cable_detail WHERE bridge_code=? ORDER BY cable_type,display_order,serial_no", bridgeCode));
        result.put("measurementPoints", jdbcTemplate.queryForList("SELECT * FROM tb_bridge_measurement_point WHERE bridge_code=? ORDER BY point_category,display_order,point_no", bridgeCode));
        return result;
    }

    private Map<String, Object> inspectionSummary(String bridgeCode, List<Map<String, Object>> initials, List<Map<String, Object>> periodic) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("initialCount", initials.size());
        result.put("periodicCount", periodic.size());
        result.put("initialHistory", initials.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row.get("inspection_date")); item.put("code", row.get("initial_inspection_code"));
            item.put("version", row.getOrDefault("version_no", 1)); item.put("effective", row.getOrDefault("effective_flag", 1));
            return item;
        }).toList());
        result.put("periodicHistory", periodic.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row.get("inspection_date")); item.put("code", row.get("periodic_inspection_code"));
            Object rating = row.get("rating_level_name") == null ? row.get("rating_level_code") : row.get("rating_level_name");
            item.put("rating", rating); item.put("defectCount", row.getOrDefault("defectCount", 0));
            return item;
        }).toList());
        return result;
    }

    private String categoryName(String category) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT category_name FROM tb_bridge_photo_category WHERE category_code=?", category);
        return rows.isEmpty() ? "桥梁照片" : String.valueOf(rows.get(0).get("category_name"));
    }

    public Map<String, Object> geocode(String address) {
        if (address == null || address.isBlank()) throw new BusinessException("地址不能为空");
        if (amapKey.isBlank()) throw new BusinessException("尚未配置高德 Web 服务 Key");
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get().uri(builder -> builder.scheme("https")
                .host("restapi.amap.com").path("/v3/geocode/geo").queryParam("key", amapKey)
                .queryParam("address", address).queryParam("city", "重庆").build())
                .retrieve().body(Map.class);
        return response == null ? Map.of() : response;
    }
}
