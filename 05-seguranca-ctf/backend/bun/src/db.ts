/**
 * Database setup and seed data.
 */

import { Database } from "bun:sqlite";

export const db = new Database("secureshop.db", { create: true });

db.run("PRAGMA journal_mode=WAL");

// ---------------------------------------------------------------------------
// Schema
// ---------------------------------------------------------------------------

db.run(`
  CREATE TABLE IF NOT EXISTS users (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    nome       TEXT    NOT NULL,
    email      TEXT    NOT NULL UNIQUE,
    password   TEXT    NOT NULL,
    role       TEXT    NOT NULL DEFAULT 'user',
    telefone   TEXT,
    cpf_last4  TEXT,
    endereco   TEXT,
    created_at TEXT    DEFAULT (datetime('now'))
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS products (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    nome       TEXT    NOT NULL,
    descricao  TEXT,
    preco      REAL    NOT NULL,
    categoria  TEXT,
    created_at TEXT    DEFAULT (datetime('now'))
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS orders (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER REFERENCES users(id),
    total      REAL    NOT NULL,
    status     TEXT    DEFAULT 'pendente',
    created_at TEXT    DEFAULT (datetime('now'))
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS reviews (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER REFERENCES products(id),
    user_id    INTEGER REFERENCES users(id),
    user_name  TEXT    NOT NULL,
    rating     INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    text       TEXT    NOT NULL,
    created_at TEXT    DEFAULT (datetime('now'))
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS coupons (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    code      TEXT    NOT NULL UNIQUE,
    discount  REAL    NOT NULL,
    max_uses  INTEGER NOT NULL DEFAULT 1,
    uses      INTEGER NOT NULL DEFAULT 0,
    active    INTEGER NOT NULL DEFAULT 1
  )
`);

db.run(`
  CREATE TABLE IF NOT EXISTS request_logs (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    method           TEXT,
    path             TEXT,
    query_params     TEXT,
    body             TEXT,
    status_code      INTEGER,
    sql_query        TEXT,
    response_preview TEXT,
    ip               TEXT,
    created_at       TEXT DEFAULT (datetime('now'))
  )
`);

// ---------------------------------------------------------------------------
// Seed data
// ---------------------------------------------------------------------------

const userCount = (db.query("SELECT COUNT(*) as c FROM users").get() as { c: number }).c;

if (userCount === 0) {
  db.run(
    `INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco)
     VALUES ('Administrador', 'admin@secureshop.com', 'Admin@2024!', 'admin',
             '(11) 99999-0001', '7890', 'Rua Augusta 1200, Sao Paulo - SP')`
  );

  // Fictional regular users (for orders/reviews seed data)
  const fictionalUsers: [string, string, string, string, string, string][] = [
    ["Carlos Silva",    "carlos@secureshop.com",   "Senha123",     "(11) 98765-4321", "1234", "Av. Paulista 1000, Apto 42, Sao Paulo - SP"],
    ["Ana Oliveira",    "ana@secureshop.com",       "Minhasenha1",  "(21) 97654-3210", "5678", "Rua Copacabana 500, Rio de Janeiro - RJ"],
    ["Pedro Santos",    "pedro@secureshop.com",     "Pedro@456",    "(31) 96543-2109", "9012", "Rua da Bahia 1500, Belo Horizonte - MG"],
    ["Mariana Costa",   "mariana@secureshop.com",   "MarCosta99",   "(41) 95432-1098", "3456", "Rua XV de Novembro 700, Curitiba - PR"],
    ["Rafael Mendes",   "rafael@secureshop.com",    "Rafael2024",   "(51) 94321-0987", "7891", "Av. Borges de Medeiros 2300, Porto Alegre - RS"],
  ];

  for (const [nome, email, password, telefone, cpf_last4, endereco] of fictionalUsers) {
    db.run(
      "INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES (?, ?, ?, 'user', ?, ?, ?)",
      [nome, email, password, telefone, cpf_last4, endereco]
    );
  }

  // Workshop participants (pre-registered accounts)
  const participants: [string, string][] = [
    ["Danilo Pimentel",     "danilo.pimentel@esales.com.br"],
    ["Pedro Cardoso",       "pedro.cardoso@esales.com.br"],
    ["Dienis Silva",        "dienis.silva@esales.com.br"],
    ["Claudio Pereira",     "claudio.pereira@esales.com.br"],
    ["Bruno Nunes",         "bruno.nunes@esales.com.br"],
    ["Paulo Souza",         "paulo.souza@esales.com.br"],
    ["Rodrigo Weiler",      "rodrigo.weiler@esales.com.br"],
    ["Paulo Araujo",        "paulo.araujo@pagplan.com.br"],
    ["Wilson Abdala",       "wilson.abdala@esales.com.br"],
    ["Guilherme Schlup",    "guilherme.schlup@esales.com.br"],
    ["Marilia Soares",      "marilia.silva@esales.com.br"],
    ["Victor Moreira",      "victor.moreira@esales.com.br"],
    ["Marcelo Mattos",      "marcelo.mattos@esales.com.br"],
    ["Rudimar Grass",       "rudimar.grass@esales.com.br"],
    ["Leonardo Borges",     "leonardo.borges@esales.com.br"],
    ["Thiago Miranda",      "thiago.miranda@esales.com.br"],
    ["Guilherme Freitas",   "guilherme.freitas@esales.com.br"],
    ["Cassio Cristiano",    "cassio.cristiano@esales.com.br"],
    ["Lucas Jesus",         "lucas.jesus@esales.com.br"],
    ["Luciano Sager",       "luciano.sager@esales.com.br"],
    ["Rodrigo Quadros",     "rodrigo.quadros@esales.com.br"],
    ["Marco Braida",        "marco.braida@esales.com.br"],
    ["Eden Meireles",       "eden.meireles@esales.com.br"],
    ["Marcelino Avelar",    "marcelino.avelar@esales.com.br"],
    ["Eric Gottschalk",     "eric.gottschalk@esales.com.br"],
    ["Luis Felipe Silva",   "luis.silva@esales.com.br"],
    ["Linsmar Cruz",        "linsmar.cruz@esales.com.br"],
    ["Moises Oliveira",     "moises.oliveira@esales.com.br"],
    ["Felipe Girardi",      "felipe.girardi@esales.com.br"],
    ["Alexandre Finger",    "alexandre.finger@esales.com.br"],
    ["Dioner Seffrin",      "dioner.seffrin@esales.com.br"],
    ["Filipe Freitas",      "filipe.freitas@esales.com.br"],
    ["Susiane Basso",       "susiane.basso@esales.com.br"],
    ["Camila Silva",        "camila.silva@esales.com.br"],
    ["Fernando Garibotti",  "fernando.garibotti@esales.com.br"],
    ["Taiomara Soares",     "taiomara.soares@esales.com.br"],
    ["Vilson Santos",       "vilson.santos@esales.com.br"],
    ["Pablo Gomez",         "pablo.gomez@esales.com.br"],
  ];

  for (const [nome, email] of participants) {
    // Password = first name lowercase + @2024
    const password = "eSalesWorkshopAI-2026";
    db.run(
      "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, 'user')",
      [nome, email, password]
    );
  }

  console.log(`[DB] Users seeded (1 admin + ${fictionalUsers.length} fictional + ${participants.length} participants)`);
}

const productCount = (db.query("SELECT COUNT(*) as c FROM products").get() as { c: number }).c;

if (productCount === 0) {
  const products: [string, string, number, string][] = [
    ["Notebook Pro 15",           "Notebook de alto desempenho com SSD 512GB",        4599.99, "Eletronicos"],
    ["Mouse Ergonomico",          "Mouse sem fio com design ergonomico",               189.90, "Perifericos"],
    ["Teclado Mecanico RGB",      "Teclado mecanico com iluminacao RGB",               349.00, "Perifericos"],
    ["Monitor 4K 27\"",           "Monitor 4K UHD com taxa de 144Hz",                 2199.00, "Monitores"],
    ["Headset Gamer",             "Fone de ouvido com microfone e som surround 7.1",   279.90, "Audio"],
    ["Webcam HD 1080p",           "Camera para videoconferencia em Full HD",            249.00, "Perifericos"],
    ["SSD NVMe 1TB",              "Armazenamento ultrarapido NVMe Gen4",               599.00, "Armazenamento"],
    ["Memoria RAM 32GB DDR5",     "Kit de memoria DDR5 6000MHz",                       799.90, "Memoria"],
    ["Placa de Video RTX 4070",   "GPU para jogos e criacao de conteudo",             3299.00, "Hardware"],
    ["Fonte 750W 80+ Gold",       "Fonte de alimentacao modular 80 Plus Gold",         499.00, "Hardware"],
    ["Gabinete ATX Mid-Tower",    "Gabinete com painel lateral em vidro temperado",    399.00, "Hardware"],
    ["Cooler CPU 360mm AIO",      "Water cooler all-in-one com radiador de 360mm",     699.00, "Refrigeracao"],
    ["Placa-mae Z790",            "Placa-mae para Intel 13a/14a geracao DDR5",        1499.00, "Hardware"],
    ["Processador i9-14900K",     "Processador Intel Core i9 de 14a geracao",         3899.00, "Processadores"],
    ["Processador Ryzen 9 7950X", "AMD Ryzen 9 7950X 16-Core 32-Thread",             3299.00, "Processadores"],
    ["Hub USB-C 10 em 1",         "Hub com HDMI, USB-A, USB-C, SD card e ethernet",    249.90, "Perifericos"],
    ["Mesa para Escritorio",      "Mesa com suporte para monitor e organizacao",       1199.00, "Mobiliario"],
    ["Cadeira Gamer Pro",         "Cadeira com apoio lombar e descanso de braco",      1599.00, "Mobiliario"],
    ["Suporte Monitor Articulado","Suporte de mesa com braco articulado duplo",         399.00, "Acessorios"],
    ["Mousepad XL",               "Mousepad extra large 90x40cm antiderrapante",        99.90, "Acessorios"],
    ["Switch KVM 4 portas",       "Chaveador KVM HDMI para 4 computadores",            349.00, "Rede"],
    ["Roteador Wi-Fi 6 AX6000",   "Roteador dual-band com Wi-Fi 6 e 6Gbps",          1299.00, "Rede"],
    ["Cabo HDMI 2.1 2m",          "Cabo HDMI 2.1 certificado 4K@120Hz 8K@60Hz",        89.90, "Cabos"],
    ["No-break 1500VA",           "UPS com gerenciamento de energia e USB",             899.00, "Energia"],
    ["Impressora Laser Color",    "Impressora laser colorida duplex Wi-Fi",            1899.00, "Impressao"],
    ["Scanner de Mesa A4",        "Scanner otico com resolucao 1200dpi e Wi-Fi",        799.00, "Impressao"],
    ["Pendrive 256GB USB 3.2",    "Pendrive de alta velocidade leitura 400MB/s",        89.90, "Armazenamento"],
    ["HD Externo 4TB USB 3.0",    "Disco rigido portatil 4TB com backup automatico",    599.00, "Armazenamento"],
    ["Tablet 10\" Android",       "Tablet com tela Full HD e bateria 7040mAh",         1099.00, "Tablets"],
    ["Smartphone 5G 256GB",       "Celular 5G camera 200MP e tela AMOLED 6.7\"",       3499.00, "Smartphones"],
  ];

  for (const [nome, descricao, preco, categoria] of products) {
    db.run("INSERT INTO products (nome, descricao, preco, categoria) VALUES (?, ?, ?, ?)", [nome, descricao, preco, categoria]);
  }
  console.log("[DB] Products seeded (30 items)");
}

const orderCount = (db.query("SELECT COUNT(*) as c FROM orders").get() as { c: number }).c;

if (orderCount === 0) {
  const statuses = ["pendente", "processando", "enviado", "entregue", "cancelado"];
  const totals = [
    189.90, 349.00, 4599.99, 2199.00, 599.00, 279.90, 799.90, 3299.00,
    499.00, 399.00, 699.00, 1499.00, 3899.00, 249.90, 1199.00, 1599.00,
    399.00, 99.90, 349.00, 1299.00, 89.90, 899.00, 1899.00, 799.00,
    89.90, 599.00, 1099.00, 3499.00, 249.00, 2199.00, 4599.99, 189.90,
    349.00, 279.90, 799.90, 3299.00, 499.00, 399.00, 699.00, 1499.00,
    3899.00, 249.90, 1199.00, 1599.00, 399.00, 99.90, 349.00, 1299.00,
    89.90, 899.00,
  ];

  for (let i = 0; i < 50; i++) {
    const userId = (i % 5) + 2;
    const status = statuses[i % statuses.length];
    db.run("INSERT INTO orders (user_id, total, status) VALUES (?, ?, ?)", [userId, totals[i], status]);
  }
  console.log("[DB] Orders seeded (50 items)");
}

const reviewCount = (db.query("SELECT COUNT(*) as c FROM reviews").get() as { c: number }).c;

if (reviewCount === 0) {
  const reviews: [number, number, string, number, string][] = [
    [1, 2, "Carlos Silva",  5, "Excelente notebook, muito rapido! Recomendo para desenvolvedores."],
    [2, 3, "Ana Oliveira",  4, "Mouse confortavel, uso o dia inteiro sem dor. So poderia ter mais botoes."],
    [3, 4, "Pedro Santos",  5, "Teclado fantastico! O som dos switches e muito satisfatorio."],
    [4, 5, "Mariana Costa", 3, "Monitor bom mas chegou com um pixel morto. Suporte resolveu rapido."],
    [5, 6, "Rafael Mendes", 4, "Som surround impressionante. Microfone poderia ser melhor."],
  ];

  for (const [productId, userId, userName, rating, text] of reviews) {
    db.run("INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)",
      [productId, userId, userName, rating, text]);
  }
  console.log("[DB] Reviews seeded (5 items)");
}

const couponCount = (db.query("SELECT COUNT(*) as c FROM coupons").get() as { c: number }).c;

if (couponCount === 0) {
  db.run("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('DESCONTO10', 10, 10, 0, 1)");
  db.run("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('PRIMEIRACOMPRA', 15, 1, 0, 1)");
  db.run("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('BLACKFRIDAY', 25, 100, 95, 1)");
  console.log("[DB] Coupons seeded (3 items)");
}

console.log("[DB] Database ready");
