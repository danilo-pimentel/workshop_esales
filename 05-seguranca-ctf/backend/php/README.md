# CTF Vulnerable REST API — PHP 8.3 + Slim 4

> **FOR DEFENDER / SECURITY TRAINING USE ONLY**
> This application is **deliberately insecure**. Never deploy to a production or internet-accessible environment.

---

## Quick Start

```bash
# 1. Install dependencies
composer install

# 2. Start the server on port 4000
php -S localhost:4000 -t public

# 3. Open monitoring dashboard
#    http://localhost:4000/monitor/

# 4. Run vulnerability tests (server must be running)
./vendor/bin/phpunit
```

---

## Stack

| Component | Version |
|-----------|---------|
| PHP       | 8.3     |
| Slim      | 4.x     |
| Database  | SQLite (PDO) |
| JWT       | firebase/php-jwt 6.x |
| Tests     | PHPUnit 11.x |

The SQLite database is created automatically at `data/ctf.sqlite` on first request.

---

## Seed Data

| Role  | Email                   | Password     |
|-------|-------------------------|--------------|
| admin | admin@empresa.com       | Admin@2024!  |
| user  | joao.silva@email.com    | senha123     |
| user  | maria.santos@email.com  | maria@pass   |
| user  | carlos.o@email.com      | carlos2024   |
| user  | ana.pereira@email.com   | ana#secure   |
| user  | pedro.costa@email.com   | pedro!321    |

- 30 products across 5 categories
- 50 orders distributed among users 2–6

---

## API Endpoints

### Public (no auth)
| Method | Path                | Description              |
|--------|---------------------|--------------------------|
| GET    | /                   | API info & endpoint list |
| POST   | /api/auth/login     | Login                    |
| POST   | /api/auth/register  | Register new user        |
| GET    | /api/products       | List/search products     |
| GET    | /api/products/{id}  | Single product           |
| GET    | /api/logs           | Request logs             |
| DELETE | /api/logs           | Clear logs               |

### Authenticated (JWT Bearer token required)
| Method | Path             | Description              |
|--------|------------------|--------------------------|
| GET    | /api/auth/me     | Current user from JWT    |
| GET    | /api/users       | List all users           |
| GET    | /api/users/{id}  | User by ID               |
| PUT    | /api/users/{id}  | Update user              |
| GET    | /api/orders      | All orders               |
| GET    | /api/orders/{id} | Single order             |
| POST   | /api/orders      | Create order             |

### Admin (requires admin permission)
| Method | Path                    | Description       |
|--------|-------------------------|-------------------|
| GET    | /api/admin/users        | All users         |
| GET    | /api/admin/stats        | Stats             |
| DELETE | /api/admin/users/{id}   | Delete user       |
| POST   | /api/admin/reset-db     | Reset database    |

---

## Monitoring Dashboard

Open `http://localhost:4000/monitor/` in a browser.

- Auto-polls `/api/logs` every 5 seconds (configurable)
- Highlights SQL keywords in blue
- Highlights SQL injection patterns in red
- Highlights credential fields (`password`, `senha`, etc.) in yellow
- Rows with injection attempts shown with red left border
- Rows with credential bodies shown with yellow left border
- Filter by method, path, SQL-only, credentials-only
- Clear logs button

---

## Running Tests

The test suite sends real HTTP requests to `localhost:4000`. The server must be running.

```bash
# Terminal 1 — start server
php -S localhost:4000 -t public

# Terminal 2 — run tests
./vendor/bin/phpunit --testdox
```

The tests verify security aspects of the application.

---

## File Structure

```
php/
├── composer.json
├── phpunit.xml
├── README.md
├── public/
│   ├── index.php              # Slim bootstrap, port 4000
│   └── monitor/
│       └── index.html         # Monitoring dashboard
├── src/
│   ├── Database.php           # SQLite connection + seed (6 users, 30 products, 50 orders)
│   ├── JwtManager.php         # JWT encode/decode
│   ├── Middleware/
│   │   ├── AuthMiddleware.php      # JWT validation for protected routes
│   │   ├── CorsMiddleware.php      # CORS config
│   │   └── LoggerMiddleware.php    # Request logger
│   └── Controllers/
│       ├── AuthController.php      # Login + register
│       ├── UserController.php      # Endpoints de usuario
│       ├── ProductController.php   # Busca de produtos
│       ├── AdminController.php     # Endpoints admin
│       ├── LogController.php       # Logs de requisicoes
│       └── OrderController.php     # CRUD de pedidos
├── tests/
│   └── SecurityTest.php       # Security tests
└── data/
    └── ctf.sqlite             # Created automatically on first run
```
