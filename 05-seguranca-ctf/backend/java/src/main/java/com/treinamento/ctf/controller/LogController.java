package com.treinamento.ctf.controller;

import com.treinamento.ctf.middleware.RequestLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.treinamento.ctf.config.DbUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<?> getLogs(
            @RequestParam(required = false, defaultValue = "100") int limit,
            HttpServletRequest request) {

        String role = (String) request.getAttribute("userRole");

        if (!"admin".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        String sql = "SELECT * FROM request_logs ORDER BY id DESC LIMIT " + limit;
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM request_logs ORDER BY id DESC LIMIT ?", limit
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("logs", DbUtil.lowercaseKeys(logs));
            result.put("count", logs.size());

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearLogs(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");

        if (!"admin".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        jdbcTemplate.update("DELETE FROM request_logs");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Todos os logs foram apagados");

        return ResponseEntity.ok(result);
    }
}
