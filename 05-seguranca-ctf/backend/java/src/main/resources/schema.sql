DROP TABLE IF EXISTS request_logs;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id         INTEGER AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'user',
    telefone   VARCHAR(20),
    cpf_last4  VARCHAR(4),
    endereco   VARCHAR(500),
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id         INTEGER AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(255)   NOT NULL,
    descricao  VARCHAR(1000),
    preco      VARCHAR(50)    NOT NULL,
    categoria  VARCHAR(100),
    created_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id         INTEGER AUTO_INCREMENT PRIMARY KEY,
    user_id    INTEGER REFERENCES users(id),
    total      DECIMAL(10, 2) NOT NULL,
    status     VARCHAR(50)    DEFAULT 'pendente',
    created_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id         INTEGER AUTO_INCREMENT PRIMARY KEY,
    product_id INTEGER REFERENCES products(id),
    user_id    INTEGER REFERENCES users(id),
    user_name  VARCHAR(255) NOT NULL,
    rating     INTEGER      NOT NULL CHECK(rating >= 1 AND rating <= 5),
    text       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coupons (
    id        INTEGER AUTO_INCREMENT PRIMARY KEY,
    code      VARCHAR(50) NOT NULL UNIQUE,
    discount  DECIMAL(5, 2) NOT NULL,
    max_uses  INTEGER NOT NULL DEFAULT 1,
    uses      INTEGER NOT NULL DEFAULT 0,
    active    INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE request_logs (
    id               INTEGER AUTO_INCREMENT PRIMARY KEY,
    method           VARCHAR(10),
    path             VARCHAR(500),
    query_params     TEXT,
    body             TEXT,
    status_code      INTEGER,
    sql_query        TEXT,
    response_preview TEXT,
    ip               VARCHAR(50),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
