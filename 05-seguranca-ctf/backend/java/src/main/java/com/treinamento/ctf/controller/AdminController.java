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
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/users")
    public ResponseEntity<?> adminListUsers(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");

        if (!"admin".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        String sql = "SELECT id, nome, email, password, role, created_at FROM users ORDER BY id";
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("users", DbUtil.lowercaseKeys(users));
            result.put("count", users.size());

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> adminCreateUser(@RequestBody Map<String, String> body,
                                              HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");

        if (!"admin".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        String nome     = body.getOrDefault("nome", "");
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");
        String newRole  = body.getOrDefault("role", "user");

        if (nome.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Campos obrigatorios: nome, email, password");
            return ResponseEntity.status(400).body(err);
        }

        String sqlQuery = "INSERT INTO users (nome, email, password, role) VALUES ('" +
                nome + "', '" + email + "', '***', '" + newRole + "')";
        RequestLogFilter.CURRENT_SQL.set(sqlQuery);

        try {
            jdbcTemplate.update(
                "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)",
                nome, email, password, newRole
            );
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
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

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> adminDeleteUser(@PathVariable Integer id,
                                             HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");

        if (!"admin".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Acesso negado");
            return ResponseEntity.status(403).body(err);
        }

        String sql = "DELETE FROM users WHERE id = " + id;
        RequestLogFilter.CURRENT_SQL.set(sql);

        try {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Database error");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Usuario " + id + " removido com sucesso");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-db")
    public ResponseEntity<?> resetDb(@RequestBody Map<String, String> body) {
        String RESET_KEY = "esales-ai-reset-2026";
        if (!RESET_KEY.equals(body.get("key"))) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Forbidden");
            err.put("message", "Chave de reset invalida");
            return ResponseEntity.status(403).body(err);
        }

        try {
            jdbcTemplate.update("DELETE FROM request_logs");
            jdbcTemplate.update("DELETE FROM reviews");
            jdbcTemplate.update("DELETE FROM orders");
            jdbcTemplate.update("DELETE FROM coupons");
            jdbcTemplate.update("DELETE FROM users");

            // Reset auto-increment (H2 syntax)
            jdbcTemplate.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.update("ALTER TABLE products ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.update("ALTER TABLE orders ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.update("ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.update("ALTER TABLE coupons ALTER COLUMN id RESTART WITH 1");

            // Re-seed admin
            jdbcTemplate.update(
                "INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) " +
                "VALUES ('Administrador', 'admin@secureshop.com', 'Admin@2024!', 'admin', " +
                "'(11) 99999-0001', '7890', 'Rua Augusta 1200, Sao Paulo - SP')"
            );

            // Re-seed fictional users
            String[][] fictionalUsers = {
                {"Carlos Silva",   "carlos@secureshop.com",   "Senha123",    "(11) 98765-4321", "1234", "Av. Paulista 1000, Apto 42, Sao Paulo - SP"},
                {"Ana Oliveira",   "ana@secureshop.com",      "Minhasenha1", "(21) 97654-3210", "5678", "Rua Copacabana 500, Rio de Janeiro - RJ"},
                {"Pedro Santos",   "pedro@secureshop.com",    "Pedro@456",   "(31) 96543-2109", "9012", "Rua da Bahia 1500, Belo Horizonte - MG"},
                {"Mariana Costa",  "mariana@secureshop.com",  "MarCosta99",  "(41) 95432-1098", "3456", "Rua XV de Novembro 700, Curitiba - PR"},
                {"Rafael Mendes",  "rafael@secureshop.com",   "Rafael2024",  "(51) 94321-0987", "7891", "Av. Borges de Medeiros 2300, Porto Alegre - RS"},
            };
            for (String[] u : fictionalUsers) {
                jdbcTemplate.update(
                    "INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) " +
                    "VALUES (?, ?, ?, 'user', ?, ?, ?)",
                    u[0], u[1], u[2], u[3], u[4], u[5]
                );
            }

            // Re-seed workshop participants
            String[][] participants = {
                {"Danilo Pimentel", "danilo.pimentel@esales.com.br"},
                {"Pedro Cardoso", "pedro.cardoso@esales.com.br"},
                {"Dienis Silva", "dienis.silva@esales.com.br"},
                {"Claudio Pereira", "claudio.pereira@esales.com.br"},
                {"Bruno Nunes", "bruno.nunes@esales.com.br"},
                {"Paulo Souza", "paulo.souza@esales.com.br"},
                {"Rodrigo Weiler", "rodrigo.weiler@esales.com.br"},
                {"Paulo Araujo", "paulo.araujo@pagplan.com.br"},
                {"Wilson Abdala", "wilson.abdala@esales.com.br"},
                {"Guilherme Schlup", "guilherme.schlup@esales.com.br"},
                {"Marilia Soares", "marilia.silva@esales.com.br"},
                {"Victor Moreira", "victor.moreira@esales.com.br"},
                {"Marcelo Mattos", "marcelo.mattos@esales.com.br"},
                {"Rudimar Grass", "rudimar.grass@esales.com.br"},
                {"Leonardo Borges", "leonardo.borges@esales.com.br"},
                {"Thiago Miranda", "thiago.miranda@esales.com.br"},
                {"Guilherme Freitas", "guilherme.freitas@esales.com.br"},
                {"Cassio Cristiano", "cassio.cristiano@esales.com.br"},
                {"Lucas Jesus", "lucas.jesus@esales.com.br"},
                {"Luciano Sager", "luciano.sager@esales.com.br"},
                {"Rodrigo Quadros", "rodrigo.quadros@esales.com.br"},
                {"Marco Braida", "marco.braida@esales.com.br"},
                {"Eden Meireles", "eden.meireles@esales.com.br"},
                {"Marcelino Avelar", "marcelino.avelar@esales.com.br"},
                {"Eric Gottschalk", "eric.gottschalk@esales.com.br"},
                {"Luis Felipe Silva", "luis.silva@esales.com.br"},
                {"Linsmar Cruz", "linsmar.cruz@esales.com.br"},
                {"Moises Oliveira", "moises.oliveira@esales.com.br"},
                {"Felipe Girardi", "felipe.girardi@esales.com.br"},
                {"Alexandre Finger", "alexandre.finger@esales.com.br"},
                {"Dioner Seffrin", "dioner.seffrin@esales.com.br"},
                {"Filipe Freitas", "filipe.freitas@esales.com.br"},
                {"Susiane Basso", "susiane.basso@esales.com.br"},
                {"Camila Silva", "camila.silva@esales.com.br"},
                {"Fernando Garibotti", "fernando.garibotti@esales.com.br"},
                {"Taiomara Soares", "taiomara.soares@esales.com.br"},
                {"Vilson Santos", "vilson.santos@esales.com.br"},
                {"Pablo Gomez", "pablo.gomez@esales.com.br"},
            };
            for (String[] p : participants) {
                jdbcTemplate.update(
                    "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, 'user')",
                    p[0], p[1], "eSalesWorkshopAI-2026"
                );
            }

            // Re-seed orders (50)
            String[] statuses = {"pendente", "processando", "enviado", "entregue", "cancelado"};
            double[] totals = {
                189.90, 349.00, 4599.99, 2199.00, 599.00, 279.90, 799.90, 3299.00,
                499.00, 399.00, 699.00, 1499.00, 3899.00, 249.90, 1199.00, 1599.00,
                399.00, 99.90, 349.00, 1299.00, 89.90, 899.00, 1899.00, 799.00,
                89.90, 599.00, 1099.00, 3499.00, 249.00, 2199.00, 4599.99, 189.90,
                349.00, 279.90, 799.90, 3299.00, 499.00, 399.00, 699.00, 1499.00,
                3899.00, 249.90, 1199.00, 1599.00, 399.00, 99.90, 349.00, 1299.00,
                89.90, 899.00
            };
            for (int i = 0; i < 50; i++) {
                int uId = (i % 5) + 2;
                String status = statuses[i % statuses.length];
                jdbcTemplate.update(
                    "INSERT INTO orders (user_id, total, status) VALUES (?, ?, ?)",
                    uId, totals[i], status
                );
            }

            // Re-seed reviews (5)
            Object[][] reviews = {
                {1, 2, "Carlos Silva", 5, "Excelente notebook, muito rapido! Recomendo para desenvolvedores."},
                {2, 3, "Ana Oliveira", 4, "Mouse confortavel, uso o dia inteiro sem dor. So poderia ter mais botoes."},
                {3, 4, "Pedro Santos", 5, "Teclado fantastico! O som dos switches e muito satisfatorio."},
                {4, 5, "Mariana Costa", 3, "Monitor bom mas chegou com um pixel morto. Suporte resolveu rapido."},
                {5, 6, "Rafael Mendes", 4, "Som surround impressionante. Microfone poderia ser melhor."},
            };
            for (Object[] r : reviews) {
                jdbcTemplate.update(
                    "INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)",
                    r[0], r[1], r[2], r[3], r[4]
                );
            }

            // Re-seed coupons (3)
            jdbcTemplate.update("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('DESCONTO10', 10, 10, 0, 1)");
            jdbcTemplate.update("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('PRIMEIRACOMPRA', 15, 1, 0, 1)");
            jdbcTemplate.update("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('BLACKFRIDAY', 25, 100, 95, 1)");

            Integer userTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Database restaurado ao estado inicial");
            result.put("users", userTotal);
            result.put("products", 30);
            result.put("orders", 50);
            result.put("reviews", 5);
            result.put("coupons", 3);

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Reset failed");
            err.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
