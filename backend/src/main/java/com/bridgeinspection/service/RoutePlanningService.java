package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RoutePlanningService {
    private final RestClient restClient;
    private final JdbcTemplate jdbcTemplate;
    private final String amapKey;

    public RoutePlanningService(RestClient.Builder builder, JdbcTemplate jdbcTemplate,
                                @Value("${app.amap.web-service-key:}") String amapKey) {
        this.restClient = builder.build();
        this.jdbcTemplate = jdbcTemplate;
        this.amapKey = amapKey;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> plan(List<?> points) {
        if (points == null || points.size() < 2) throw new BusinessException("路线至少需要起点和终点");
        if (amapKey.isBlank()) throw new BusinessException("尚未配置高德 Web 服务 Key");
        List<String> coordinates = points.stream().map(this::coordinate).toList();
        String origin = coordinates.get(0);
        String destination = coordinates.get(coordinates.size() - 1);
        String waypoints = coordinates.size() > 2
                ? String.join(";", coordinates.subList(1, coordinates.size() - 1)) : null;
        Map<String, Object> response = restClient.get().uri(uri -> {
            var builder = uri.scheme("https").host("restapi.amap.com").path("/v3/direction/driving")
                    .queryParam("key", amapKey).queryParam("origin", origin)
                    .queryParam("destination", destination).queryParam("extensions", "all");
            if (waypoints != null) builder.queryParam("waypoints", waypoints);
            return builder.build();
        }).retrieve().body(Map.class);
        if (response == null || !"1".equals(String.valueOf(response.get("status")))) {
            throw new BusinessException("高德路线规划失败：" + (response == null ? "无响应" : response.get("info")));
        }
        Map<String, Object> route = firstMap(response.get("route"));
        List<Map<String, Object>> paths = maps(route.get("paths"));
        if (paths.isEmpty()) throw new BusinessException("高德未找到可规划路线");
        Map<String, Object> path = paths.get(0);
        List<Map<String, Object>> steps = maps(path.get("steps"));
        List<String> polylines = new ArrayList<>();
        List<String> roads = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            String polyline = string(step.get("polyline"));
            if (!polyline.isBlank()) polylines.add(polyline);
            String road = string(step.get("road"));
            if (!road.isBlank() && !roads.contains(road)) roads.add(road);
        }
        String routeName = roads.isEmpty() ? "未命名路线" : roads.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("route_name", routeName);
        result.put("route_grade", suggestGrade(routeName, roads));
        result.put("route_geometry", String.join(";", polylines));
        result.put("route_distance", number(path.get("distance")));
        result.put("roads", roads);
        result.put("points", points);
        return result;
    }

    public void assertUniqueName(String routeName, String routeCode) {
        if (routeName == null || routeName.isBlank()) throw new BusinessException("路线名称不能为空");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_route WHERE route_name = ? AND (? IS NULL OR route_code <> ?)",
                Integer.class, routeName.trim(), routeCode, routeCode);
        if (count != null && count > 0) throw new BusinessException("路线名称已存在，路线名称必须唯一");
    }

    private String suggestGrade(String routeName, List<String> roads) {
        String all = (routeName + " " + String.join(" ", roads)).toUpperCase(Locale.ROOT);
        if (Pattern.compile("(^|[^A-Z])(G|S)\\d+").matcher(all).find() || all.contains("高速")) return "高速公路";
        if (all.matches(".*(^|[^A-Z])(X|Y)\\d+.*")) return "三级公路";
        if (all.contains("省道")) return "二级公路";
        if (all.contains("县道")) return "三级公路";
        return null;
    }

    private String coordinate(Object value) {
        if (value instanceof Map<?, ?> map) return string(map.get("longitude")) + "," + string(map.get("latitude"));
        if (value instanceof List<?> list && list.size() >= 2) return string(list.get(0)) + "," + string(list.get(1));
        String raw = string(value);
        if (!raw.matches("-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?")) throw new BusinessException("地图点坐标格式无效");
        return raw.replace(" ", "");
    }

    private Map<String, Object> firstMap(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return Map.of();
    }

    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }

    private String string(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private Object number(Object value) { return value == null ? null : value; }
}
