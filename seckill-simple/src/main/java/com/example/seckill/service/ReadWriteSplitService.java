package com.example.seckill.service;

import com.example.seckill.datasource.ReadOnlyDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReadWriteSplitService {

    private final JdbcTemplate jdbcTemplate;

    public ReadWriteSplitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getWriteDataSourceInfo() {
        return jdbcTemplate.queryForObject(
                "SELECT @@hostname AS host, @@server_id AS serverId, @@read_only AS readOnly",
                (rs, rowNum) -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("host", rs.getString("host"));
                    result.put("serverId", rs.getLong("serverId"));
                    result.put("readOnly", rs.getInt("readOnly"));
                    return result;
                }
        );
    }

    @ReadOnlyDataSource
    public Map<String, Object> getReadDataSourceInfo() {
        return jdbcTemplate.queryForObject(
                "SELECT @@hostname AS host, @@server_id AS serverId, @@read_only AS readOnly",
                (rs, rowNum) -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("host", rs.getString("host"));
                    result.put("serverId", rs.getLong("serverId"));
                    result.put("readOnly", rs.getInt("readOnly"));
                    return result;
                }
        );
    }
}
