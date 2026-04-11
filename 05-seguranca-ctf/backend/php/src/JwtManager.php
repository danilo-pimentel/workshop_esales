<?php

declare(strict_types=1);

namespace App;

use Firebase\JWT\JWT;
use Firebase\JWT\Key;

class JwtManager
{
    public const JWT_SECRET = 'super-secret-jwt-key-2024';
    public const ALGORITHM  = 'HS256';
    public const TTL        = 86400; // 24 hours

    public static function generate(array $payload): string
    {
        $now = time();
        $claims = [
            'sub'   => (string) $payload['sub'],
            'email' => $payload['email'],
            'role'  => $payload['role'],
            'iat'   => $now,
            'exp'   => $now + self::TTL,
        ];

        return JWT::encode($claims, self::JWT_SECRET, self::ALGORITHM);
    }

    public static function decode(string $token): ?array
    {
        try {
            $decoded = JWT::decode($token, new Key(self::JWT_SECRET, self::ALGORITHM));
            return [
                'sub'   => $decoded->sub,
                'email' => $decoded->email,
                'role'  => $decoded->role,
            ];
        } catch (\Exception $e) {
            return null;
        }
    }

    public static function fromRequest(\Psr\Http\Message\ServerRequestInterface $request): ?array
    {
        $header = $request->getHeaderLine('Authorization');
        if (str_starts_with($header, 'Bearer ')) {
            $token = substr($header, 7);
            return self::decode($token);
        }
        return null;
    }
}
