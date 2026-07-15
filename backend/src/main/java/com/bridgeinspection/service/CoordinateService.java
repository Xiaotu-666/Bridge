package com.bridgeinspection.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Coordinate conversion used at the map boundary. Database values remain auditable. */
@Service
public class CoordinateService {
    private static final double PI = Math.PI;
    private static final double AXIS = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    public Map<String, Object> toGcj(Map<String, Object> row) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        Number lonValue = number(row.get("longitude"));
        Number latValue = number(row.get("latitude"));
        if (lonValue == null || latValue == null) return copy;
        String source = String.valueOf(row.getOrDefault("coordinate_source", "WGS84"));
        double lon = lonValue.doubleValue();
        double lat = latValue.doubleValue();
        if ("WGS84".equalsIgnoreCase(source) || "GPS".equalsIgnoreCase(source)) {
            double[] converted = wgs84ToGcj02(lon, lat);
            copy.put("longitude", converted[0]);
            copy.put("latitude", converted[1]);
            copy.put("display_longitude", converted[0]);
            copy.put("display_latitude", converted[1]);
        } else {
            copy.put("display_longitude", lon);
            copy.put("display_latitude", lat);
        }
        copy.put("coordinate_source", source);
        return copy;
    }

    public double[] wgs84ToGcj02(double longitude, double latitude) {
        if (outOfChina(longitude, latitude)) return new double[]{longitude, latitude};
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLon = transformLon(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((AXIS * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (AXIS / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{longitude + dLon, latitude + dLat};
    }

    private boolean outOfChina(double lon, double lat) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    private Number number(Object value) {
        return value instanceof Number n ? n : value == null ? null : parse(value.toString());
    }

    private Number parse(String value) {
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return null; }
    }
}
