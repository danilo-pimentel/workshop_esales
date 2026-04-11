package com.treinamento.ctf.database

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

lateinit var dbConnection: Connection
    private set

fun initDatabase(path: String = "secureshop.db") {
    Class.forName("org.sqlite.JDBC")

    if (::dbConnection.isInitialized && !dbConnection.isClosed) {
        runCatching { dbConnection.close() }
    }

    if (path != ":memory:") {
        val dbFile = File(path)
        if (dbFile.exists()) {
            dbFile.delete()
            logger.info("[DB] Deleted existing database file: $path")
        }
    }

    dbConnection = DriverManager.getConnection("jdbc:sqlite:$path")

    if (path != ":memory:") {
        dbConnection.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
    }
    dbConnection.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }

    createSchema()
    seedData()
    logger.info("[DB] Database ready")
}

private fun createSchema() {
    dbConnection.createStatement().executeUpdate("""
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
    """.trimIndent())

    dbConnection.createStatement().executeUpdate("""
        CREATE TABLE IF NOT EXISTS products (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            nome       TEXT    NOT NULL,
            descricao  TEXT,
            preco      REAL    NOT NULL,
            categoria  TEXT,
            created_at TEXT    DEFAULT (datetime('now'))
        )
    """.trimIndent())

    dbConnection.createStatement().executeUpdate("""
        CREATE TABLE IF NOT EXISTS orders (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id    INTEGER REFERENCES users(id),
            total      REAL    NOT NULL,
            status     TEXT    DEFAULT 'pendente',
            created_at TEXT    DEFAULT (datetime('now'))
        )
    """.trimIndent())

    dbConnection.createStatement().executeUpdate("""
        CREATE TABLE IF NOT EXISTS reviews (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER REFERENCES products(id),
            user_id    INTEGER REFERENCES users(id),
            user_name  TEXT    NOT NULL,
            rating     INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
            text       TEXT    NOT NULL,
            created_at TEXT    DEFAULT (datetime('now'))
        )
    """.trimIndent())

    dbConnection.createStatement().executeUpdate("""
        CREATE TABLE IF NOT EXISTS coupons (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            code      TEXT    NOT NULL UNIQUE,
            discount  REAL    NOT NULL,
            max_uses  INTEGER NOT NULL DEFAULT 1,
            uses      INTEGER NOT NULL DEFAULT 0,
            active    INTEGER NOT NULL DEFAULT 1
        )
    """.trimIndent())

    dbConnection.createStatement().executeUpdate("""
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
    """.trimIndent())
}

private fun seedData() {
    seedUsers()
    seedProducts()
    seedOrders()
    seedReviews()
    seedCoupons()
}

fun reseedDatabase() {
    dbConnection.createStatement().executeUpdate("DELETE FROM request_logs")
    dbConnection.createStatement().executeUpdate("DELETE FROM reviews")
    dbConnection.createStatement().executeUpdate("DELETE FROM orders")
    dbConnection.createStatement().executeUpdate("DELETE FROM coupons")
    dbConnection.createStatement().executeUpdate("DELETE FROM products")
    dbConnection.createStatement().executeUpdate("DELETE FROM users")
    dbConnection.createStatement().executeUpdate("DELETE FROM sqlite_sequence")
    seedData()
}

private fun seedUsers() {
    dbConnection.prepareStatement(
        "INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES (?, ?, ?, ?, ?, ?, ?)"
    ).apply {
        setString(1, "Administrador")
        setString(2, "admin@secureshop.com")
        setString(3, "Admin@2024!")
        setString(4, "admin")
        setString(5, "(11) 99999-0001")
        setString(6, "7890")
        setString(7, "Rua Augusta 1200, Sao Paulo - SP")
        executeUpdate()
    }

    val fictionalUsers = listOf(
        arrayOf("Carlos Silva",   "carlos@secureshop.com",  "Senha123",    "(11) 98765-4321", "1234", "Av. Paulista 1000, Apto 42, Sao Paulo - SP"),
        arrayOf("Ana Oliveira",   "ana@secureshop.com",     "Minhasenha1", "(21) 97654-3210", "5678", "Rua Copacabana 500, Rio de Janeiro - RJ"),
        arrayOf("Pedro Santos",   "pedro@secureshop.com",   "Pedro@456",   "(31) 96543-2109", "9012", "Rua da Bahia 1500, Belo Horizonte - MG"),
        arrayOf("Mariana Costa",  "mariana@secureshop.com", "MarCosta99",  "(41) 95432-1098", "3456", "Rua XV de Novembro 700, Curitiba - PR"),
        arrayOf("Rafael Mendes",  "rafael@secureshop.com",  "Rafael2024",  "(51) 94321-0987", "7891", "Av. Borges de Medeiros 2300, Porto Alegre - RS"),
    )

    for (u in fictionalUsers) {
        dbConnection.prepareStatement(
            "INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES (?, ?, ?, 'user', ?, ?, ?)"
        ).apply {
            setString(1, u[0]); setString(2, u[1]); setString(3, u[2])
            setString(4, u[3]); setString(5, u[4]); setString(6, u[5])
            executeUpdate()
        }
    }

    val participants = listOf(
        arrayOf("Danilo Pimentel",     "danilo.pimentel@esales.com.br"),
        arrayOf("Pedro Cardoso",       "pedro.cardoso@esales.com.br"),
        arrayOf("Dienis Silva",        "dienis.silva@esales.com.br"),
        arrayOf("Claudio Pereira",     "claudio.pereira@esales.com.br"),
        arrayOf("Bruno Nunes",         "bruno.nunes@esales.com.br"),
        arrayOf("Paulo Souza",         "paulo.souza@esales.com.br"),
        arrayOf("Rodrigo Weiler",      "rodrigo.weiler@esales.com.br"),
        arrayOf("Paulo Araujo",        "paulo.araujo@pagplan.com.br"),
        arrayOf("Wilson Abdala",       "wilson.abdala@esales.com.br"),
        arrayOf("Guilherme Schlup",    "guilherme.schlup@esales.com.br"),
        arrayOf("Marilia Soares",      "marilia.silva@esales.com.br"),
        arrayOf("Victor Moreira",      "victor.moreira@esales.com.br"),
        arrayOf("Marcelo Mattos",      "marcelo.mattos@esales.com.br"),
        arrayOf("Rudimar Grass",       "rudimar.grass@esales.com.br"),
        arrayOf("Leonardo Borges",     "leonardo.borges@esales.com.br"),
        arrayOf("Thiago Miranda",      "thiago.miranda@esales.com.br"),
        arrayOf("Guilherme Freitas",   "guilherme.freitas@esales.com.br"),
        arrayOf("Cassio Cristiano",    "cassio.cristiano@esales.com.br"),
        arrayOf("Lucas Jesus",         "lucas.jesus@esales.com.br"),
        arrayOf("Luciano Sager",       "luciano.sager@esales.com.br"),
        arrayOf("Rodrigo Quadros",     "rodrigo.quadros@esales.com.br"),
        arrayOf("Marco Braida",        "marco.braida@esales.com.br"),
        arrayOf("Eden Meireles",       "eden.meireles@esales.com.br"),
        arrayOf("Marcelino Avelar",    "marcelino.avelar@esales.com.br"),
        arrayOf("Eric Gottschalk",     "eric.gottschalk@esales.com.br"),
        arrayOf("Luis Felipe Silva",   "luis.silva@esales.com.br"),
        arrayOf("Linsmar Cruz",        "linsmar.cruz@esales.com.br"),
        arrayOf("Moises Oliveira",     "moises.oliveira@esales.com.br"),
        arrayOf("Felipe Girardi",      "felipe.girardi@esales.com.br"),
        arrayOf("Alexandre Finger",    "alexandre.finger@esales.com.br"),
        arrayOf("Dioner Seffrin",      "dioner.seffrin@esales.com.br"),
        arrayOf("Filipe Freitas",      "filipe.freitas@esales.com.br"),
        arrayOf("Susiane Basso",       "susiane.basso@esales.com.br"),
        arrayOf("Camila Silva",        "camila.silva@esales.com.br"),
        arrayOf("Fernando Garibotti",  "fernando.garibotti@esales.com.br"),
        arrayOf("Taiomara Soares",     "taiomara.soares@esales.com.br"),
        arrayOf("Vilson Santos",       "vilson.santos@esales.com.br"),
        arrayOf("Pablo Gomez",         "pablo.gomez@esales.com.br"),
    )

    for (p in participants) {
        dbConnection.prepareStatement(
            "INSERT INTO users (nome, email, password, role) VALUES (?, ?, ?, 'user')"
        ).apply {
            setString(1, p[0]); setString(2, p[1]); setString(3, "eSalesWorkshopAI-2026")
            executeUpdate()
        }
    }

    logger.info("[DB] Users seeded (1 admin + ${fictionalUsers.size} fictional + ${participants.size} participants)")
}

private fun seedProducts() {
    data class P(val nome: String, val descricao: String, val preco: Double, val categoria: String)

    val products = listOf(
        P("Notebook Pro 15",           "Notebook de alto desempenho com SSD 512GB",        4599.99, "Eletronicos"),
        P("Mouse Ergonomico",          "Mouse sem fio com design ergonomico",               189.90, "Perifericos"),
        P("Teclado Mecanico RGB",      "Teclado mecanico com iluminacao RGB",               349.00, "Perifericos"),
        P("Monitor 4K 27\"",           "Monitor 4K UHD com taxa de 144Hz",                 2199.00, "Monitores"),
        P("Headset Gamer",             "Fone de ouvido com microfone e som surround 7.1",   279.90, "Audio"),
        P("Webcam HD 1080p",           "Camera para videoconferencia em Full HD",            249.00, "Perifericos"),
        P("SSD NVMe 1TB",              "Armazenamento ultrarapido NVMe Gen4",               599.00, "Armazenamento"),
        P("Memoria RAM 32GB DDR5",     "Kit de memoria DDR5 6000MHz",                       799.90, "Memoria"),
        P("Placa de Video RTX 4070",   "GPU para jogos e criacao de conteudo",             3299.00, "Hardware"),
        P("Fonte 750W 80+ Gold",       "Fonte de alimentacao modular 80 Plus Gold",         499.00, "Hardware"),
        P("Gabinete ATX Mid-Tower",    "Gabinete com painel lateral em vidro temperado",    399.00, "Hardware"),
        P("Cooler CPU 360mm AIO",      "Water cooler all-in-one com radiador de 360mm",     699.00, "Refrigeracao"),
        P("Placa-mae Z790",            "Placa-mae para Intel 13a/14a geracao DDR5",        1499.00, "Hardware"),
        P("Processador i9-14900K",     "Processador Intel Core i9 de 14a geracao",         3899.00, "Processadores"),
        P("Processador Ryzen 9 7950X", "AMD Ryzen 9 7950X 16-Core 32-Thread",             3299.00, "Processadores"),
        P("Hub USB-C 10 em 1",         "Hub com HDMI, USB-A, USB-C, SD card e ethernet",    249.90, "Perifericos"),
        P("Mesa para Escritorio",      "Mesa com suporte para monitor e organizacao",       1199.00, "Mobiliario"),
        P("Cadeira Gamer Pro",         "Cadeira com apoio lombar e descanso de braco",      1599.00, "Mobiliario"),
        P("Suporte Monitor Articulado","Suporte de mesa com braco articulado duplo",         399.00, "Acessorios"),
        P("Mousepad XL",               "Mousepad extra large 90x40cm antiderrapante",        99.90, "Acessorios"),
        P("Switch KVM 4 portas",       "Chaveador KVM HDMI para 4 computadores",            349.00, "Rede"),
        P("Roteador Wi-Fi 6 AX6000",   "Roteador dual-band com Wi-Fi 6 e 6Gbps",          1299.00, "Rede"),
        P("Cabo HDMI 2.1 2m",          "Cabo HDMI 2.1 certificado 4K@120Hz 8K@60Hz",        89.90, "Cabos"),
        P("No-break 1500VA",           "UPS com gerenciamento de energia e USB",             899.00, "Energia"),
        P("Impressora Laser Color",    "Impressora laser colorida duplex Wi-Fi",            1899.00, "Impressao"),
        P("Scanner de Mesa A4",        "Scanner otico com resolucao 1200dpi e Wi-Fi",        799.00, "Impressao"),
        P("Pendrive 256GB USB 3.2",    "Pendrive de alta velocidade leitura 400MB/s",        89.90, "Armazenamento"),
        P("HD Externo 4TB USB 3.0",    "Disco rigido portatil 4TB com backup automatico",   599.00, "Armazenamento"),
        P("Tablet 10\" Android",       "Tablet com tela Full HD e bateria 7040mAh",        1099.00, "Tablets"),
        P("Smartphone 5G 256GB",       "Celular 5G camera 200MP e tela AMOLED 6.7\"",      3499.00, "Smartphones"),
    )

    val stmt = dbConnection.prepareStatement(
        "INSERT INTO products (nome, descricao, preco, categoria) VALUES (?, ?, ?, ?)"
    )
    for (p in products) {
        stmt.setString(1, p.nome)
        stmt.setString(2, p.descricao)
        stmt.setDouble(3, p.preco)
        stmt.setString(4, p.categoria)
        stmt.addBatch()
    }
    stmt.executeBatch()
    logger.info("[DB] Products seeded (30 items)")
}

private fun seedOrders() {
    val statuses = listOf("pendente", "processando", "enviado", "entregue", "cancelado")
    val totals = listOf(
        189.90, 349.00, 4599.99, 2199.00, 599.00, 279.90, 799.90, 3299.00,
        499.00, 399.00, 699.00, 1499.00, 3899.00, 249.90, 1199.00, 1599.00,
        399.00,  99.90, 349.00, 1299.00,  89.90,  899.00, 1899.00,  799.00,
         89.90, 599.00, 1099.00, 3499.00, 249.00, 2199.00, 4599.99, 189.90,
        349.00, 279.90, 799.90, 3299.00,  499.00,  399.00,  699.00, 1499.00,
       3899.00, 249.90, 1199.00, 1599.00, 399.00,   99.90,  349.00, 1299.00,
         89.90, 899.00
    )

    val stmt = dbConnection.prepareStatement(
        "INSERT INTO orders (user_id, total, status) VALUES (?, ?, ?)"
    )
    for (i in 0 until 50) {
        stmt.setInt(1, (i % 5) + 2)
        stmt.setDouble(2, totals[i])
        stmt.setString(3, statuses[i % statuses.size])
        stmt.addBatch()
    }
    stmt.executeBatch()
    logger.info("[DB] Orders seeded (50 items)")
}

private fun seedReviews() {
    data class R(val productId: Int, val userId: Int, val userName: String, val rating: Int, val text: String)

    val reviews = listOf(
        R(1, 2, "Carlos Silva",  5, "Excelente notebook, muito rapido! Recomendo para desenvolvedores."),
        R(2, 3, "Ana Oliveira",  4, "Mouse confortavel, uso o dia inteiro sem dor. So poderia ter mais botoes."),
        R(3, 4, "Pedro Santos",  5, "Teclado fantastico! O som dos switches e muito satisfatorio."),
        R(4, 5, "Mariana Costa", 3, "Monitor bom mas chegou com um pixel morto. Suporte resolveu rapido."),
        R(5, 6, "Rafael Mendes", 4, "Som surround impressionante. Microfone poderia ser melhor."),
    )

    val stmt = dbConnection.prepareStatement(
        "INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (?, ?, ?, ?, ?)"
    )
    for (r in reviews) {
        stmt.setInt(1, r.productId)
        stmt.setInt(2, r.userId)
        stmt.setString(3, r.userName)
        stmt.setInt(4, r.rating)
        stmt.setString(5, r.text)
        stmt.addBatch()
    }
    stmt.executeBatch()
    logger.info("[DB] Reviews seeded (5 items)")
}

private fun seedCoupons() {
    dbConnection.createStatement().executeUpdate("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('DESCONTO10', 10, 10, 0, 1)")
    dbConnection.createStatement().executeUpdate("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('PRIMEIRACOMPRA', 15, 1, 0, 1)")
    dbConnection.createStatement().executeUpdate("INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('BLACKFRIDAY', 25, 100, 95, 1)")
    logger.info("[DB] Coupons seeded (3 items)")
}
