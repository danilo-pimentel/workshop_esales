<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Database;
use App\JwtManager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class AuthController
{
    public function login(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        if ($body === null) {
            $raw = (string) $request->getBody();
            $body = json_decode($raw, true);
        }
        if (!is_array($body)) {
            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/login', 'queryParams' => '',
                'body' => '', 'statusCode' => 400, 'sqlQuery' => '',
                'responsePreview' => 'Invalid JSON',
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'JSON invalido',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $email    = $body['email'] ?? '';
        $password = $body['password'] ?? '';

        $sqlQuery = "SELECT * FROM users WHERE email = '$email' AND password = '$password'";

        $db = Database::getInstance();
        try {
            $sql = "SELECT * FROM users WHERE email = :email AND password = '$password'";
            $stmt = $db->prepare($sql);
            $stmt->execute([':email' => $email]);
            $user = $stmt->fetch();
        } catch (\Exception $e) {
            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/login', 'queryParams' => '',
                'body' => json_encode($body), 'statusCode' => 500,
                'sqlQuery' => $sqlQuery, 'responsePreview' => $e->getMessage(),
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Database error',
                'message' => $e->getMessage(),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }

        if (!$user) {
            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/login', 'queryParams' => '',
                'body' => json_encode($body), 'statusCode' => 401,
                'sqlQuery' => $sqlQuery, 'responsePreview' => 'Invalid credentials',
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Unauthorized',
                'message' => 'Credenciais invalidas',
            ]));
            return $response->withStatus(401)->withHeader('Content-Type', 'application/json');
        }

        $token = JwtManager::generate([
            'sub'   => $user['id'],
            'email' => $user['email'],
            'role'  => $user['role'],
        ]);

        Database::logRequest([
            'method' => 'POST', 'path' => '/api/auth/login', 'queryParams' => '',
            'body' => json_encode($body), 'statusCode' => 200,
            'sqlQuery' => $sqlQuery,
            'responsePreview' => json_encode(['id' => $user['id'], 'email' => $user['email'], 'role' => $user['role']]),
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'token' => $token,
            'user'  => [
                'id'    => $user['id'],
                'nome'  => $user['nome'],
                'email' => $user['email'],
                'role'  => $user['role'],
            ],
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function register(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        if ($body === null) {
            $raw = (string) $request->getBody();
            $body = json_decode($raw, true);
        }
        if (!is_array($body)) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'JSON invalido',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $nome     = $body['nome'] ?? '';
        $email    = $body['email'] ?? '';
        $password = $body['password'] ?? '';
        $role     = $body['role'] ?? 'user';

        if (!$nome || !$email || !$password) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'Campos obrigatorios: nome, email, password',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $sqlQuery = "INSERT INTO users (nome, email, password, role) VALUES ('$nome', '$email', '$password', '$role')";

        $db = Database::getInstance();
        try {
            $stmt = $db->prepare("INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, ?)");
            $stmt->execute([$nome, $email, $password, $role]);
        } catch (\Exception $e) {
            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/register', 'queryParams' => '',
                'body' => json_encode($body), 'statusCode' => 409,
                'sqlQuery' => $sqlQuery, 'responsePreview' => $e->getMessage(),
                'ip' => self::getIp($request),
            ]);
            $response->getBody()->write(json_encode([
                'error' => 'Conflict',
                'message' => 'Email ja cadastrado',
            ]));
            return $response->withStatus(409)->withHeader('Content-Type', 'application/json');
        }

        $stmt = $db->prepare("SELECT id, nome, email, role FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $newUser = $stmt->fetch();

        Database::logRequest([
            'method' => 'POST', 'path' => '/api/auth/register', 'queryParams' => '',
            'body' => json_encode($body), 'statusCode' => 201,
            'sqlQuery' => $sqlQuery, 'responsePreview' => json_encode($newUser),
            'ip' => self::getIp($request),
        ]);

        $response->getBody()->write(json_encode([
            'message' => 'Usuario criado com sucesso',
            'user' => $newUser,
        ]));
        return $response->withStatus(201)->withHeader('Content-Type', 'application/json');
    }

    public function forgotPassword(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        if ($body === null) {
            $raw = (string) $request->getBody();
            $body = json_decode($raw, true);
        }
        if (!is_array($body)) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => 'JSON invalido',
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $email = $body['email'] ?? '';

        if (!$email) {
            $response->getBody()->write(json_encode([
                'error' => 'Bad Request',
                'message' => "Campo 'email' obrigatorio",
            ]));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        try {
            $smtpConfigPath = dirname(__DIR__, 2) . '/config/smtp.json';
            $raw = file_get_contents($smtpConfigPath);
            if ($raw === false) {
                throw new \RuntimeException("Cannot read SMTP configuration from $smtpConfigPath");
            }
            $smtpConfig = json_decode($raw, true);

            $smtpConnection = $smtpConfig['host'] . ':' . $smtpConfig['port'];

            $db = Database::getInstance();
            $stmt = $db->prepare("SELECT id, nome, email FROM users WHERE email = ?");
            $stmt->execute([$email]);
            $user = $stmt->fetch();

            if ($user) {
                $resetToken = JwtManager::generate([
                    'sub'   => $user['id'],
                    'email' => $user['email'],
                    'role'  => 'reset',
                ]);
                $resetUrl = "http://localhost:5173/reset-password?token=$resetToken";
                @fwrite(fopen('php://stderr', 'w'), "[SMTP] Sending reset email to {$user['email']} via $smtpConnection\n");
            }

            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/forgot-password', 'queryParams' => '',
                'body' => json_encode(['email' => $email]), 'statusCode' => 200,
                'sqlQuery' => "SELECT id, nome, email FROM users WHERE email = '$email'",
                'responsePreview' => 'Reset email sent',
                'ip' => self::getIp($request),
            ]);

            $response->getBody()->write(json_encode([
                'message' => 'Se o email estiver cadastrado, voce recebera as instrucoes de recuperacao.',
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        } catch (\Throwable $e) {
            Database::logRequest([
                'method' => 'POST', 'path' => '/api/auth/forgot-password', 'queryParams' => '',
                'body' => json_encode(['email' => $email]), 'statusCode' => 500,
                'sqlQuery' => '', 'responsePreview' => $e->getMessage(),
                'ip' => self::getIp($request),
            ]);

            $response->getBody()->write(json_encode([
                'error'   => 'Internal Server Error',
                'message' => $e->getMessage(),
                'stack'   => array_slice(explode("\n", $e->getTraceAsString()), 0, 8),
            ]));
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }

    private static function getIp(Request $request): string
    {
        return $request->getHeaderLine('X-Forwarded-For') ?: ($request->getServerParams()['REMOTE_ADDR'] ?? 'unknown');
    }
}
