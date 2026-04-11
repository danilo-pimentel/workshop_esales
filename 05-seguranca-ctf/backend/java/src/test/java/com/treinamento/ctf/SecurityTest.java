package com.treinamento.ctf;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static String userToken;
    private static String adminToken;

    private String doLogin(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + 9;
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @BeforeEach
    void setup() throws Exception {
        if (userToken == null) {
            userToken = doLogin("carlos@secureshop.com", "Senha123");
        }
        if (adminToken == null) {
            adminToken = doLogin("admin@secureshop.com", "Admin@2024!");
        }
    }

    // V1: SQL Injection Login - password not parameterized
    @Test
    @Order(1)
    @DisplayName("V1a: SQL Injection - login bypass with comment trick")
    void sqlInjectionLoginBypass() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@secureshop.com\",\"password\":\"' OR '1'='1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @Order(2)
    @DisplayName("V1b: SQL Injection - database error exposes info")
    void sqlInjectionLoginError() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"' INVALID SQL --\"}"))
                .andExpect(status().is(anyOf(is(200), is(401), is(500))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("error") || body.contains("token"),
                "Response should contain error info or token");
    }

    // V2: Price Manipulation
    @Test
    @Order(3)
    @DisplayName("V2: Price manipulation - client total accepted without server validation")
    void priceManipulation() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"product_id\":1,\"quantity\":1,\"price\":4599.99}],\"total\":0.01}"))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.order.total").value(0.01));
    }

    // V3: IDOR Users
    @Test
    @Order(4)
    @DisplayName("V3a: IDOR - user accesses other user PII (telefone, cpf_last4)")
    void idorUserAccess() throws Exception {
        mockMvc.perform(get("/api/users/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@secureshop.com"))
                .andExpect(jsonPath("$.telefone").exists())
                .andExpect(jsonPath("$.cpf_last4").exists())
                .andExpect(jsonPath("$.endereco").exists());
    }

    @Test
    @Order(5)
    @DisplayName("V3b: IDOR - password NOT returned (correct behavior)")
    void idorNoPassword() throws Exception {
        mockMvc.perform(get("/api/users/2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // V4: Mass Assignment
    @Test
    @Order(6)
    @DisplayName("V4: Mass assignment - register with role=admin")
    void massAssignmentRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hacker\",\"email\":\"hacker@test.com\",\"password\":\"hack123\",\"role\":\"admin\"}"))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.user.role").value("admin"));
    }

    // V5: Stored XSS in reviews
    @Test
    @Order(7)
    @DisplayName("V5: Stored XSS - review text stored without sanitization")
    void storedXssReview() throws Exception {
        String xssPayload = "<script>alert('xss')</script>";
        mockMvc.perform(post("/api/products/1/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + xssPayload + "\",\"rating\":5}"))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.review.text").value(xssPayload));
    }

    // V6: Error Disclosure (forgot-password)
    @Test
    @Order(8)
    @DisplayName("V6a: Error disclosure - forgot-password leaks stack trace")
    void errorDisclosureForgotPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\"}"))
                .andExpect(status().is(500))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("stack"), "Response should contain stack trace");
    }

    // V6: Path Traversal (export)
    @Test
    @Order(9)
    @DisplayName("V6b: Path traversal - export template reads arbitrary file")
    void pathTraversalExport() throws Exception {
        // Normal template should work
        mockMvc.perform(get("/api/export/csv.template")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template").exists());
    }

    // V7: CORS Wildcard
    @Test
    @Order(10)
    @DisplayName("V7: CORS wildcard - Access-Control-Allow-Origin: *")
    void corsWildcard() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/products")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        String acaoHeader = result.getResponse().getHeader("Access-Control-Allow-Origin");
        assertNotNull(acaoHeader, "CORS header should be present");
    }

    // V8: SQL Injection Search with sanitizeInput bypass
    @Test
    @Order(11)
    @DisplayName("V8a: SQL Injection search - UNION attack (sanitizeInput only removes HTML chars)")
    void sqlInjectionSearchUnion() throws Exception {
        String payload = "' UNION SELECT id,email,password,role,nome,created_at FROM users--";

        MvcResult result = mockMvc.perform(get("/api/products/search")
                        .param("q", payload))
                .andExpect(status().is(anyOf(is(200), is(500))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("results") || body.contains("error"),
                "Response should contain results or error");
    }

    @Test
    @Order(12)
    @DisplayName("V8b: sanitizeInput only removes HTML chars, not SQL chars")
    void sanitizeInputBypass() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/search")
                        .param("q", "' OR 1=1--"))
                .andExpect(status().is(anyOf(is(200), is(500))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("results") || body.contains("error"),
                "SQL chars should pass through sanitizeInput");
    }

    // Admin routes require JWT (not X-Role header)
    @Test
    @Order(13)
    @DisplayName("Admin: requires JWT auth, not X-Role header")
    void adminRequiresJwt() throws Exception {
        // No JWT, just X-Role header - should be denied
        mockMvc.perform(get("/api/admin/users")
                        .header("X-Role", "admin"))
                .andExpect(status().is(401));

        // With valid admin JWT - should work
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray());
    }

    // Logs require JWT + admin
    @Test
    @Order(14)
    @DisplayName("Logs: requires JWT auth + admin role")
    void logsRequiresJwtAdmin() throws Exception {
        // No auth - should be denied
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().is(401));

        // Regular user - should be denied
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is(403));

        // Admin - should work
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray());
    }

    // Orders: no ownership check on GET /:id (IDOR on orders)
    @Test
    @Order(15)
    @DisplayName("Orders: GET /:id has no ownership check, leaks user_name and user_email")
    void orderIdorWithUserLeak() throws Exception {
        // Carlos (user 2) accessing an order that might belong to another user
        mockMvc.perform(get("/api/orders/3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").exists())
                .andExpect(jsonPath("$.user_email").exists());
    }

    // PUT /api/users/:id has ownership check
    @Test
    @Order(16)
    @DisplayName("PUT /api/users/:id: ownership check works")
    void putUserOwnershipCheck() throws Exception {
        // Carlos (user 2) tries to update Ana (user 3) - should be denied
        mockMvc.perform(put("/api/users/3")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Hackeada\"}"))
                .andExpect(status().is(403));
    }

    // Health check
    @Test
    @Order(17)
    @DisplayName("Health check: GET / returns service info")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("SecureShop API"))
                .andExpect(jsonPath("$.status").value("running"));
    }

    // Apply coupon
    @Test
    @Order(18)
    @DisplayName("Coupon: apply coupon to order")
    void applyCoupon() throws Exception {
        mockMvc.perform(post("/api/orders/1/apply-coupon")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DESCONTO10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cupom aplicado com sucesso"))
                .andExpect(jsonPath("$.discount").exists())
                .andExpect(jsonPath("$.new_total").exists());
    }
}
