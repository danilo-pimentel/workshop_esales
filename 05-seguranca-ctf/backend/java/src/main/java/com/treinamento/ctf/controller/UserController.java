package com.treinamento.ctf.controller;

import com.treinamento.ctf.middleware.RequestLogFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Token nao fornecido");
            return ResponseEntity.status(401).body(err);
        }

        String sql = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = " + userId;
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?", userId
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", user.get("ID"));
            result.put("nome", user.get("NOME"));
            result.put("email", user.get("EMAIL"));
            result.put("role", user.get("ROLE"));
            result.put("telefone", user.get("TELEFONE"));
            result.put("cpf_last4", user.get("CPF_LAST4"));
            result.put("endereco", user.get("ENDERECO"));
            result.put("created_at", String.valueOf(user.get("CREATED_AT")));

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Usuario nao encontrado");
            return ResponseEntity.status(404).body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id, HttpServletRequest request) {
        String sql = "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = " + id;
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?", id
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", user.get("ID"));
            result.put("nome", user.get("NOME"));
            result.put("email", user.get("EMAIL"));
            result.put("role", user.get("ROLE"));
            result.put("telefone", user.get("TELEFONE"));
            result.put("cpf_last4", user.get("CPF_LAST4"));
            result.put("endereco", user.get("ENDERECO"));
            result.put("created_at", String.valueOf(user.get("CREATED_AT")));

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Not Found");
            err.put("message", "Usuario nao encontrado");
            return ResponseEntity.status(404).body(err);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");

        if (!String.valueOf(id).equals(userId) && !"admin".equals(userRole)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        String nome     = body.get("nome");
        String email    = body.get("email");
        String password = body.get("password");
        String telefone = body.get("telefone");
        String endereco = body.get("endereco");

        RequestLogFilter.CURRENT_SQL.set("UPDATE users SET ... WHERE id = " + id);

        try {
            jdbcTemplate.update(
                "UPDATE users SET nome = COALESCE(?, nome), email = COALESCE(?, email), " +
                "password = COALESCE(?, password), telefone = COALESCE(?, telefone), " +
                "endereco = COALESCE(?, endereco) WHERE id = ?",
                nome, email, password, telefone, endereco, id
            );
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }

        try {
            Map<String, Object> updated = jdbcTemplate.queryForMap(
                "SELECT id, nome, email, role, telefone, cpf_last4, endereco, created_at FROM users WHERE id = ?", id
            );

            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", updated.get("ID"));
            userMap.put("nome", updated.get("NOME"));
            userMap.put("email", updated.get("EMAIL"));
            userMap.put("role", updated.get("ROLE"));
            userMap.put("telefone", updated.get("TELEFONE"));
            userMap.put("cpf_last4", updated.get("CPF_LAST4"));
            userMap.put("endereco", updated.get("ENDERECO"));
            userMap.put("created_at", String.valueOf(updated.get("CREATED_AT")));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Usuario atualizado");
            result.put("user", userMap);

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
