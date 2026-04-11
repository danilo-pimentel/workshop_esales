package com.treinamento.ctf.controller;

import com.treinamento.ctf.middleware.JwtUtil;
import com.treinamento.ctf.middleware.RequestLogFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        String sql = "SELECT * FROM users WHERE email = ? AND password = '" + password + "'";

        RequestLogFilter.CURRENT_SQL.set(
                "SELECT * FROM users WHERE email = '" + email + "' AND password = '" + password + "'");

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, email);

            if (rows.isEmpty()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Unauthorized");
                err.put("message", "Credenciais invalidas");
                return ResponseEntity.status(401).body(err);
            }

            Map<String, Object> user = rows.get(0);

            Integer userId = (Integer) user.get("ID");
            String  uEmail = (String)  user.get("EMAIL");
            String  role   = (String)  user.get("ROLE");
            String  nome   = (String)  user.get("NOME");

            String token = jwtUtil.generateToken(userId, uEmail, role);

            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", userId);
            userMap.put("nome", nome);
            userMap.put("email", uEmail);
            userMap.put("role", role);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token", token);
            result.put("user", userMap);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String nome     = (String) body.getOrDefault("nome", "");
        String email    = (String) body.getOrDefault("email", "");
        String password = (String) body.getOrDefault("password", "");
        String role     = (String) body.getOrDefault("role", "user");

        if (nome.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Campos obrigatorios: nome, email, password");
            return ResponseEntity.status(400).body(err);
        }

        String sqlQuery = "INSERT INTO users (nome, email, password, role) VALUES ('" +
                nome + "', '" + email + "', '" + password + "', '" + role + "')";
        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            jdbcTemplate.update(
                "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)",
                nome, email, password, role
            );
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Conflict");
            err.put("message", "Email ja cadastrado");
            return ResponseEntity.status(409).body(err);
        }

        try {
            Map<String, Object> newUser = jdbcTemplate.queryForMap(
                "SELECT id, nome, email, role FROM users WHERE email = ?", email
            );

            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", newUser.get("ID"));
            userMap.put("nome", newUser.get("NOME"));
            userMap.put("email", newUser.get("EMAIL"));
            userMap.put("role", newUser.get("ROLE"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Usuario criado com sucesso");
            result.put("user", userMap);

            return ResponseEntity.status(201).body(result);

        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");

        if (email.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Campo 'email' obrigatorio");
            return ResponseEntity.status(400).body(err);
        }

        try {
            // Load SMTP configuration from config file
            String configPath = System.getProperty("user.dir") + "/config/smtp.json";
            FileInputStream fis = new FileInputStream(configPath);
            byte[] data = fis.readAllBytes();
            fis.close();
            String smtpConfig = new String(data);

            // Parse and use SMTP config to send reset email
            Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id, nome, email FROM users WHERE email = ?", email
            );

            if (user != null) {
                String resetToken = jwtUtil.generateToken(
                    (Integer) user.get("ID"),
                    (String) user.get("EMAIL"),
                    "reset"
                );
                System.out.println("[SMTP] Sending reset email to " + user.get("EMAIL") +
                    " via " + smtpConfig);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao.");
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Internal Server Error");
            err.put("message", ex.getMessage());

            String[] stackLines = new String[0];
            if (ex.getStackTrace() != null) {
                stackLines = new String[Math.min(8, ex.getStackTrace().length)];
                for (int i = 0; i < stackLines.length; i++) {
                    stackLines[i] = ex.getStackTrace()[i].toString();
                }
            }
            err.put("stack", java.util.Arrays.asList(stackLines));

            return ResponseEntity.status(500).body(err);
        }
    }
}
