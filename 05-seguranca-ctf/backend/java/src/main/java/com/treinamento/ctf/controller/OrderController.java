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
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<?> listOrders(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String role   = (String) request.getAttribute("userRole");

        String sql;
        List<Map<String, Object>> orders;

        if ("admin".equals(role)) {
            sql = "SELECT * FROM orders ORDER BY id DESC";
            RequestLogFilter.CURRENT_SQL.set(sql);
            orders = jdbcTemplate.queryForList(sql);
        } else {
            sql = "SELECT * FROM orders WHERE user_id = " + userId + " ORDER BY id DESC";
            RequestLogFilter.CURRENT_SQL.set(sql);
            orders = jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC", userId
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", DbUtil.lowercaseKeys(orders));
        result.put("count", orders.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer id, HttpServletRequest request) {
        String sql = "SELECT o.*, u.nome as user_name, u.email as user_email " +
                     "FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = " + id;
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT o.*, u.nome as user_name, u.email as user_email " +
                "FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?", id
            );

            return ResponseEntity.ok(DbUtil.lowercaseKeys(order));
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Pedido nao encontrado");
            return ResponseEntity.status(404).body(err);
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        List<?> items = (List<?>) body.get("items");
        Number totalNum = (Number) body.get("total");
        double total = totalNum != null ? totalNum.doubleValue() : 0;

        if ((items == null || items.isEmpty()) || total <= 0) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Pedido deve conter itens e total valido");
            return ResponseEntity.status(400).body(err);
        }

        String sqlQuery = "INSERT INTO orders (user_id, total, status) VALUES (" +
                userId + ", " + total + ", 'pendente')";
        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            jdbcTemplate.update(
                "INSERT INTO orders (user_id, total, status) VALUES (?, ?, 'pendente')",
                Integer.parseInt(userId), total
            );

            Map<String, Object> newOrder = jdbcTemplate.queryForMap(
                "SELECT * FROM orders WHERE id = (SELECT MAX(id) FROM orders)"
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Pedido criado com sucesso");
            result.put("order", DbUtil.lowercaseKeys(newOrder));

            return ResponseEntity.status(201).body(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/{id}/apply-coupon")
    public ResponseEntity<?> applyCoupon(@PathVariable Integer id,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        String code = body.getOrDefault("code", "");

        if (code.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Codigo do cupom obrigatorio");
            return ResponseEntity.status(400).body(err);
        }

        Map<String, Object> coupon;
        try {
            coupon = jdbcTemplate.queryForMap(
                "SELECT * FROM coupons WHERE code = ? AND active = 1", code
            );
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Cupom nao encontrado");
            return ResponseEntity.status(404).body(err);
        }

        int uses = ((Number) coupon.get("USES")).intValue();
        int maxUses = ((Number) coupon.get("MAX_USES")).intValue();

        if (uses >= maxUses) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Cupom esgotado");
            return ResponseEntity.status(400).body(err);
        }

        // Increment coupon usage
        Integer couponId = (Integer) coupon.get("ID");
        jdbcTemplate.update("UPDATE coupons SET uses = uses + 1 WHERE id = ?", couponId);

        Map<String, Object> order;
        try {
            order = jdbcTemplate.queryForMap("SELECT * FROM orders WHERE id = ?", id);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Pedido nao encontrado");
            return ResponseEntity.status(404).body(err);
        }

        double orderTotal = ((Number) order.get("TOTAL")).doubleValue();
        double discountPct = ((Number) coupon.get("DISCOUNT")).doubleValue();
        double discount = Math.round(orderTotal * (discountPct / 100) * 100.0) / 100.0;
        double newTotal = Math.round((orderTotal - discount) * 100.0) / 100.0;

        jdbcTemplate.update("UPDATE orders SET total = ? WHERE id = ?", newTotal, id);

        RequestLogFilter.CURRENT_SQL.set(
            "UPDATE coupons SET uses = uses + 1 WHERE id = " + couponId +
            "; UPDATE orders SET total = " + newTotal + " WHERE id = " + id
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Cupom aplicado com sucesso");
        result.put("discount", discount);
        result.put("original_total", orderTotal);
        result.put("new_total", newTotal);
        result.put("coupon_code", code);
        result.put("coupon_uses", uses + 1);
        result.put("coupon_max", maxUses);

        return ResponseEntity.ok(result);
    }
}
