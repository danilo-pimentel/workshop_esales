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
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"&]", "");
    }

    @GetMapping
    public ResponseEntity<?> listProducts(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        int offset = (page - 1) * limit;

        String sqlQuery = "SELECT * FROM products LIMIT " + limit + " OFFSET " + offset;
        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT * FROM products LIMIT ? OFFSET ?", limit, offset
            );
            Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("products", DbUtil.lowercaseKeys(products));
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("limit", limit);

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam(required = false, defaultValue = "") String q) {

        String search = sanitizeInput(q);

        String sqlQuery = "SELECT id, nome, descricao, preco, categoria, created_at " +
                           "FROM products WHERE nome LIKE '%" + search + "%'";

        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("results", DbUtil.lowercaseKeys(results));
            result.put("count", results.size());
            result.put("query", search);

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        String sqlQuery = "SELECT * FROM products WHERE id = " + id;
        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            Map<String, Object> product = jdbcTemplate.queryForMap(
                "SELECT * FROM products WHERE id = ?", id
            );
            return ResponseEntity.ok(DbUtil.lowercaseKeys(product));
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Produto nao encontrado");
            return ResponseEntity.status(404).body(err);
        }
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getProductReviews(@PathVariable Integer id) {
        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
            "SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC", id
        );

        RequestLogFilter.CURRENT_SQL.set("SELECT * FROM reviews WHERE product_id = " + id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviews", DbUtil.lowercaseKeys(reviews));
        result.put("count", reviews.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> createReview(@PathVariable Integer id,
                                          @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Token nao fornecido");
            return ResponseEntity.status(401).body(err);
        }

        String text = (String) body.getOrDefault("text", "");
        Number ratingNum = (Number) body.getOrDefault("rating", 5);
        int rating = ratingNum.intValue();

        if (text.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Campo 'text' obrigatorio");
            return ResponseEntity.status(400).body(err);
        }

        if (rating < 1 || rating > 5) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Rating deve ser entre 1 e 5");
            return ResponseEntity.status(400).body(err);
        }

        String userName;
        try {
            Map<String, Object> userRow = jdbcTemplate.queryForMap(
                "SELECT nome FROM users WHERE id = ?", Integer.parseInt(userId)
            );
            userName = (String) userRow.get("NOME");
        } catch (Exception ex) {
            userName = "Usuario";
        }

        jdbcTemplate.update(
            "INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)",
            id, Integer.parseInt(userId), userName, rating, text
        );

        Map<String, Object> review = jdbcTemplate.queryForMap(
            "SELECT * FROM reviews WHERE id = (SELECT MAX(id) FROM reviews)"
        );

        RequestLogFilter.CURRENT_SQL.set(
            "INSERT INTO reviews (product_id, user_id, ...) VALUES (" + id + ", " + userId + ", ...)"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Review adicionada");
        result.put("review", DbUtil.lowercaseKeys(review));

        return ResponseEntity.status(201).body(result);
    }
}
